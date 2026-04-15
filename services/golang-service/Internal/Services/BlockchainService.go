package Services

import (
	"context"
	"encoding/hex"
	"fmt"
	"math/big"
	"strings"
	"time"

	"github.com/ethereum/go-ethereum"
	"github.com/ethereum/go-ethereum/accounts/abi"
	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/crypto"
	"github.com/ethereum/go-ethereum/ethclient"
	"github.com/google/uuid"

	"payment-service/Configs"
	"payment-service/Internal/Dtos/Request"
	"payment-service/Internal/Dtos/Response"
)

// ─── Interface ────────────────────────────────────────────────────────────────

type IBlockchainService interface {
	PreparePaymentOrder(idBooking uuid.UUID, payerAddress string, amount float64) (*Response.PrepareVndPaymentResponse, error)
	ExecutePayment(idPayment uuid.UUID, order Request.PaymentOrderData, signature string) (string, error)
	ExecutePermit(owner, spender string, value *big.Int, deadline int64, v uint8, r, s string) (string, error)
	VerifySignature(order Request.PaymentOrderData, signature string) (bool, error)
	GetNonce(payerAddress string) (uint64, error)
}

// ─── EIP-712 domain & type constants ─────────────────────────────────────────

const (
	domainName    = "BookingPaymentGateway"
	domainVersion = "1"
)

// PaymentGateway ABI — only the functions we call.
const paymentGatewayABI = `[
  {
    "inputs": [
      {
        "components": [
          {"internalType":"bytes32","name":"orderId","type":"bytes32"},
          {"internalType":"address","name":"payer","type":"address"},
          {"internalType":"uint256","name":"amount","type":"uint256"},
          {"internalType":"bytes32","name":"bookingId","type":"bytes32"},
          {"internalType":"uint256","name":"nonce","type":"uint256"},
          {"internalType":"uint256","name":"deadline","type":"uint256"}
        ],
        "internalType":"struct PaymentGateway.PaymentOrder",
        "name":"order",
        "type":"tuple"
      },
      {"internalType":"bytes","name":"signature","type":"bytes"}
    ],
    "name":"executePayment",
    "outputs":[],
    "stateMutability":"nonpayable",
    "type":"function"
  },
  {
    "inputs":[{"internalType":"address","name":"user","type":"address"}],
    "name":"getNonce",
    "outputs":[{"internalType":"uint256","name":"","type":"uint256"}],
    "stateMutability":"view",
    "type":"function"
  }
]`

// VNDToken ABI — permit function for EIP-2612 gasless approval.
const vndTokenABI = `[
  {
    "inputs": [
      {"internalType":"address","name":"owner","type":"address"},
      {"internalType":"address","name":"spender","type":"address"},
      {"internalType":"uint256","name":"value","type":"uint256"},
      {"internalType":"uint256","name":"deadline","type":"uint256"},
      {"internalType":"uint8","name":"v","type":"uint8"},
      {"internalType":"bytes32","name":"r","type":"bytes32"},
      {"internalType":"bytes32","name":"s","type":"bytes32"}
    ],
    "name":"permit",
    "outputs":[],
    "stateMutability":"nonpayable",
    "type":"function"
  }
]`

// ─── Implementation ───────────────────────────────────────────────────────────

type blockchainService struct{}

func BlockchainService() IBlockchainService {
	return &blockchainService{}
}

// PreparePaymentOrder builds the EIP-712 typed data for the frontend to sign.
func (b *blockchainService) PreparePaymentOrder(
	idBooking uuid.UUID,
	payerAddress string,
	amount float64,
) (*Response.PrepareVndPaymentResponse, error) {
	idPayment := uuid.New()

	// orderId = keccak256(bookingId bytes + timestamp bytes)
	bookingIDBytes := idBooking[:]
	timestamp := time.Now().UnixNano()
	tsBytes := make([]byte, 8)
	tsBytes[0] = byte(timestamp >> 56)
	tsBytes[1] = byte(timestamp >> 48)
	tsBytes[2] = byte(timestamp >> 40)
	tsBytes[3] = byte(timestamp >> 32)
	tsBytes[4] = byte(timestamp >> 24)
	tsBytes[5] = byte(timestamp >> 16)
	tsBytes[6] = byte(timestamp >> 8)
	tsBytes[7] = byte(timestamp)

	orderIDBytes := crypto.Keccak256(append(bookingIDBytes, tsBytes...))
	orderIDHex := "0x" + hex.EncodeToString(orderIDBytes)

	// bookingId as bytes32 (hex)
	bookingIDHex := "0x" + hex.EncodeToString(idBooking[:])

	// Get nonce from chain
	nonce, err := b.GetNonce(payerAddress)
	if err != nil {
		return nil, fmt.Errorf("failed to get nonce: %w", err)
	}

	// VND has 0 decimals — reject fractional amounts to prevent DB/on-chain discrepancy
	if amount != float64(uint64(amount)) {
		return nil, fmt.Errorf("amount must be a whole number (VND has no fractional units), got: %f", amount)
	}

	deadline := time.Now().Add(time.Hour).Unix()
	amountInt := uint64(amount)

	typedData := Response.EIP712TypedData{
		Domain: Response.EIP712Domain{
			Name:              domainName,
			Version:           domainVersion,
			ChainID:           Configs.ChainID,
			VerifyingContract: Configs.PaymentGatewayAddress,
		},
		Types: map[string][]Response.TypeField{
			"EIP712Domain": {
				{Name: "name", Type: "string"},
				{Name: "version", Type: "string"},
				{Name: "chainId", Type: "uint256"},
				{Name: "verifyingContract", Type: "address"},
			},
			"PaymentOrder": {
				{Name: "orderId", Type: "bytes32"},
				{Name: "payer", Type: "address"},
				{Name: "amount", Type: "uint256"},
				{Name: "bookingId", Type: "bytes32"},
				{Name: "nonce", Type: "uint256"},
				{Name: "deadline", Type: "uint256"},
			},
		},
		PrimaryType: "PaymentOrder",
		Message: map[string]interface{}{
			"orderId":   orderIDHex,
			"payer":     payerAddress,
			"amount":    fmt.Sprintf("%d", amountInt),
			"bookingId": bookingIDHex,
			"nonce":     fmt.Sprintf("%d", nonce),
			"deadline":  fmt.Sprintf("%d", deadline),
		},
	}

	return &Response.PrepareVndPaymentResponse{
		IDPayment:       idPayment,
		TypedData:       typedData,
		OrderID:         orderIDHex,
		VndTokenAddress: Configs.VNDTokenAddress,
	}, nil
}

// VerifySignature recovers the signer from the EIP-712 signature and checks it matches order.Payer.
func (b *blockchainService) VerifySignature(order Request.PaymentOrderData, signature string) (bool, error) {
	digest, err := buildEIP712Digest(order)
	if err != nil {
		return false, fmt.Errorf("failed to build EIP-712 digest: %w", err)
	}

	sigBytes, err := hexToBytes(signature)
	if err != nil {
		return false, fmt.Errorf("invalid signature hex: %w", err)
	}

	// Adjust v (MetaMask sends 27/28; crypto.SigToPub expects 0/1)
	if len(sigBytes) == 65 && (sigBytes[64] == 27 || sigBytes[64] == 28) {
		sigBytes[64] -= 27
	}

	pubKey, err := crypto.SigToPub(digest, sigBytes)
	if err != nil {
		return false, fmt.Errorf("failed to recover public key: %w", err)
	}

	recoveredAddr := crypto.PubkeyToAddress(*pubKey)
	expectedAddr := common.HexToAddress(order.Payer)

	return strings.EqualFold(recoveredAddr.Hex(), expectedAddr.Hex()), nil
}

// ExecutePayment submits the executePayment transaction to Sepolia via the relayer.
// Returns the transaction hash on success.
func (b *blockchainService) ExecutePayment(idPayment uuid.UUID, order Request.PaymentOrderData, signature string) (string, error) {
	client, err := ethclient.Dial(Configs.SepoliaRPCURL)
	if err != nil {
		return "", fmt.Errorf("failed to connect to Sepolia: %w", err)
	}
	defer client.Close()

	// Load relayer private key
	privKeyHex := strings.TrimPrefix(Configs.RelayerPrivateKey, "0x")
	privateKey, err := crypto.HexToECDSA(privKeyHex)
	if err != nil {
		return "", fmt.Errorf("invalid relayer private key: %w", err)
	}

	relayerAddr := crypto.PubkeyToAddress(privateKey.PublicKey)

	// Parse ABI
	parsedABI, err := abi.JSON(strings.NewReader(paymentGatewayABI))
	if err != nil {
		return "", fmt.Errorf("failed to parse ABI: %w", err)
	}

	// Build PaymentOrder struct for ABI encoding
	// Field names must match Solidity ABI names exactly (camelCase)
	type PaymentOrderABI struct {
		OrderId   [32]byte       `abi:"orderId"`
		Payer     common.Address `abi:"payer"`
		Amount    *big.Int       `abi:"amount"`
		BookingId [32]byte       `abi:"bookingId"`
		Nonce     *big.Int       `abi:"nonce"`
		Deadline  *big.Int       `abi:"deadline"`
	}

	var orderIDBytes [32]byte
	orderIDHex := strings.TrimPrefix(order.OrderID, "0x")
	orderIDSlice, err := hex.DecodeString(orderIDHex)
	if err != nil {
		return "", fmt.Errorf("invalid order ID: %w", err)
	}
	copy(orderIDBytes[:], orderIDSlice)

	var bookingIDBytes [32]byte
	bookingIDHex := strings.TrimPrefix(order.BookingID, "0x")
	bookingIDSlice, err := hex.DecodeString(bookingIDHex)
	if err != nil {
		return "", fmt.Errorf("invalid booking ID: %w", err)
	}
	copy(bookingIDBytes[:], bookingIDSlice)

	sigBytes, err := hexToBytes(signature)
	if err != nil {
		return "", fmt.Errorf("invalid signature: %w", err)
	}

	orderABI := PaymentOrderABI{
		OrderId:   orderIDBytes,
		Payer:     common.HexToAddress(order.Payer),
		Amount:    new(big.Int).SetUint64(order.Amount),
		BookingId: bookingIDBytes,
		Nonce:     new(big.Int).SetUint64(order.Nonce),
		Deadline:  new(big.Int).SetInt64(order.Deadline),
	}

	data, err := parsedABI.Pack("executePayment", orderABI, sigBytes)
	if err != nil {
		return "", fmt.Errorf("failed to pack ABI call: %w", err)
	}

	// Get nonce, gas price, chain ID
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	nonce, err := client.PendingNonceAt(ctx, relayerAddr)
	if err != nil {
		return "", fmt.Errorf("failed to get relayer nonce: %w", err)
	}

	gasPrice, err := client.SuggestGasPrice(ctx)
	if err != nil {
		return "", fmt.Errorf("failed to get gas price: %w", err)
	}

	gatewayAddr := common.HexToAddress(Configs.PaymentGatewayAddress)

	gasLimit, err := client.EstimateGas(ctx, ethereum.CallMsg{
		From: relayerAddr,
		To:   &gatewayAddr,
		Data: data,
	})
	if err != nil {
		// Use a safe default if estimation fails
		gasLimit = 200_000
	}

	chainID := big.NewInt(Configs.ChainID)
	tx := types.NewTransaction(nonce, gatewayAddr, big.NewInt(0), gasLimit, gasPrice, data)

	signer := types.NewEIP155Signer(chainID)
	signedTx, err := types.SignTx(tx, signer, privateKey)
	if err != nil {
		return "", fmt.Errorf("failed to sign transaction: %w", err)
	}

	// Broadcast transaction
	if err = client.SendTransaction(ctx, signedTx); err != nil {
		return "", fmt.Errorf("failed to send transaction: %w", err)
	}

	txHash := signedTx.Hash().Hex()

	// Wait for receipt with timeout
	receiptCtx, receiptCancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer receiptCancel()

	for {
		select {
		case <-receiptCtx.Done():
			return txHash, fmt.Errorf("transaction broadcast but not confirmed within timeout: %s", txHash)
		case <-time.After(3 * time.Second):
			receipt, err := client.TransactionReceipt(receiptCtx, signedTx.Hash())
			if err == nil {
				if receipt.Status == 0 {
					return txHash, fmt.Errorf("transaction reverted on-chain: %s", txHash)
				}
				return txHash, nil
			}
		}
	}
}

// ExecutePermit calls VNDToken.permit() on-chain via the relayer to set approval gaslessly.
func (b *blockchainService) ExecutePermit(owner, spender string, value *big.Int, deadline int64, v uint8, r, s string) (string, error) {
	client, err := ethclient.Dial(Configs.SepoliaRPCURL)
	if err != nil {
		return "", fmt.Errorf("failed to connect to Sepolia: %w", err)
	}
	defer client.Close()

	privKeyHex := strings.TrimPrefix(Configs.RelayerPrivateKey, "0x")
	privateKey, err := crypto.HexToECDSA(privKeyHex)
	if err != nil {
		return "", fmt.Errorf("invalid relayer private key: %w", err)
	}
	relayerAddr := crypto.PubkeyToAddress(privateKey.PublicKey)

	parsedABI, err := abi.JSON(strings.NewReader(vndTokenABI))
	if err != nil {
		return "", fmt.Errorf("failed to parse VNDToken ABI: %w", err)
	}

	var rBytes, sBytes [32]byte
	rSlice, _ := hex.DecodeString(strings.TrimPrefix(r, "0x"))
	sSlice, _ := hex.DecodeString(strings.TrimPrefix(s, "0x"))
	copy(rBytes[:], rSlice)
	copy(sBytes[:], sSlice)

	data, err := parsedABI.Pack("permit",
		common.HexToAddress(owner),
		common.HexToAddress(spender),
		value,
		new(big.Int).SetInt64(deadline),
		v,
		rBytes,
		sBytes,
	)
	if err != nil {
		return "", fmt.Errorf("failed to pack permit call: %w", err)
	}

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	nonce, err := client.PendingNonceAt(ctx, relayerAddr)
	if err != nil {
		return "", fmt.Errorf("failed to get nonce: %w", err)
	}

	vndTokenAddr := common.HexToAddress(Configs.VNDTokenAddress)
	gasPrice, _ := client.SuggestGasPrice(ctx)
	gasLimit, err := client.EstimateGas(ctx, ethereum.CallMsg{From: relayerAddr, To: &vndTokenAddr, Data: data})
	if err != nil {
		return "", fmt.Errorf("failed to estimate gas: %w", err)
	}

	chainID := new(big.Int).SetInt64(Configs.ChainID)
	tx := types.NewTransaction(nonce, vndTokenAddr, big.NewInt(0), gasLimit*2, gasPrice, data)
	signedTx, err := types.SignTx(tx, types.NewEIP155Signer(chainID), privateKey)
	if err != nil {
		return "", fmt.Errorf("failed to sign permit tx: %w", err)
	}

	if err = client.SendTransaction(ctx, signedTx); err != nil {
		return "", fmt.Errorf("failed to send permit tx: %w", err)
	}

	// Wait for confirmation
	receiptCtx, receiptCancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer receiptCancel()
	for {
		select {
		case <-receiptCtx.Done():
			return signedTx.Hash().Hex(), fmt.Errorf("permit tx broadcast but not confirmed")
		case <-time.After(3 * time.Second):
			receipt, err := client.TransactionReceipt(receiptCtx, signedTx.Hash())
			if err == nil {
				if receipt.Status == 0 {
					return signedTx.Hash().Hex(), fmt.Errorf("permit tx reverted")
				}
				return signedTx.Hash().Hex(), nil
			}
		}
	}
}

// GetNonce fetches the current nonce for a payer address from the PaymentGateway contract.
func (b *blockchainService) GetNonce(payerAddress string) (uint64, error) {
	client, err := ethclient.Dial(Configs.SepoliaRPCURL)
	if err != nil {
		return 0, fmt.Errorf("failed to connect to Sepolia: %w", err)
	}
	defer client.Close()

	parsedABI, err := abi.JSON(strings.NewReader(paymentGatewayABI))
	if err != nil {
		return 0, fmt.Errorf("failed to parse ABI: %w", err)
	}

	data, err := parsedABI.Pack("getNonce", common.HexToAddress(payerAddress))
	if err != nil {
		return 0, fmt.Errorf("failed to pack getNonce call: %w", err)
	}

	gatewayAddr := common.HexToAddress(Configs.PaymentGatewayAddress)
	msg := ethereum.CallMsg{
		To:   &gatewayAddr,
		Data: data,
	}

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	result, err := client.CallContract(ctx, msg, nil)
	if err != nil {
		return 0, fmt.Errorf("failed to call getNonce: %w", err)
	}

	var nonce *big.Int
	if err := parsedABI.UnpackIntoInterface(&nonce, "getNonce", result); err != nil {
		return 0, fmt.Errorf("failed to unpack getNonce result: %w", err)
	}

	return nonce.Uint64(), nil
}

// ─── EIP-712 helpers ──────────────────────────────────────────────────────────

// buildEIP712Digest constructs the EIP-712 digest for a PaymentOrder.
func buildEIP712Digest(order Request.PaymentOrderData) ([]byte, error) {
	// PAYMENT_ORDER_TYPEHASH
	typeHash := crypto.Keccak256([]byte(
		"PaymentOrder(bytes32 orderId,address payer,uint256 amount,bytes32 bookingId,uint256 nonce,uint256 deadline)",
	))

	// Encode struct fields
	var orderIDBytes [32]byte
	orderIDHex := strings.TrimPrefix(order.OrderID, "0x")
	orderIDSlice, err := hex.DecodeString(orderIDHex)
	if err != nil {
		return nil, fmt.Errorf("invalid order ID: %w", err)
	}
	copy(orderIDBytes[:], orderIDSlice)

	payerAddr := common.HexToAddress(order.Payer)
	var payerBytes [32]byte
	copy(payerBytes[12:], payerAddr.Bytes()) // left-padded address

	var bookingIDBytes [32]byte
	bookingIDHex := strings.TrimPrefix(order.BookingID, "0x")
	bookingIDSlice, err := hex.DecodeString(bookingIDHex)
	if err != nil {
		return nil, fmt.Errorf("invalid booking ID: %w", err)
	}
	copy(bookingIDBytes[:], bookingIDSlice)

	amountBytes := padUint256(new(big.Int).SetUint64(order.Amount))
	nonceBytes := padUint256(new(big.Int).SetUint64(order.Nonce))
	deadlineBytes := padUint256(big.NewInt(order.Deadline))

	// structHash = keccak256(abi.encode(typeHash, ...fields))
	encoded := make([]byte, 0, 32*7)
	encoded = append(encoded, typeHash...)
	encoded = append(encoded, orderIDBytes[:]...)
	encoded = append(encoded, payerBytes[:]...)
	encoded = append(encoded, amountBytes...)
	encoded = append(encoded, bookingIDBytes[:]...)
	encoded = append(encoded, nonceBytes...)
	encoded = append(encoded, deadlineBytes...)

	structHash := crypto.Keccak256(encoded)

	// Domain separator
	domainSep, err := buildDomainSeparator()
	if err != nil {
		return nil, err
	}

	// Final digest: keccak256("\x19\x01" + domainSeparator + structHash)
	digestInput := append([]byte{0x19, 0x01}, domainSep...)
	digestInput = append(digestInput, structHash...)
	return crypto.Keccak256(digestInput), nil
}

func buildDomainSeparator() ([]byte, error) {
	domainTypeHash := crypto.Keccak256([]byte(
		"EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)",
	))

	nameHash := crypto.Keccak256([]byte(domainName))
	versionHash := crypto.Keccak256([]byte(domainVersion))
	chainIDBytes := padUint256(big.NewInt(Configs.ChainID))

	gatewayAddr := common.HexToAddress(Configs.PaymentGatewayAddress)
	var addrBytes [32]byte
	copy(addrBytes[12:], gatewayAddr.Bytes())

	encoded := make([]byte, 0, 32*5)
	encoded = append(encoded, domainTypeHash...)
	encoded = append(encoded, nameHash...)
	encoded = append(encoded, versionHash...)
	encoded = append(encoded, chainIDBytes...)
	encoded = append(encoded, addrBytes[:]...)

	return crypto.Keccak256(encoded), nil
}

// padUint256 encodes a big.Int as a 32-byte big-endian value.
func padUint256(n *big.Int) []byte {
	b := n.Bytes()
	padded := make([]byte, 32)
	copy(padded[32-len(b):], b)
	return padded
}

// hexToBytes decodes a hex string (with or without "0x" prefix) to bytes.
func hexToBytes(s string) ([]byte, error) {
	s = strings.TrimPrefix(s, "0x")
	return hex.DecodeString(s)
}

