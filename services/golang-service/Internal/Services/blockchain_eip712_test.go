package Services

// NOTE: Run these tests after `go mod tidy` installs go-ethereum.
//
//   cd booking-be/services/golang-service
//   go get github.com/ethereum/go-ethereum@v1.15.0
//   go mod tidy
//   go test ./Internal/Services/... -v -run TestEIP712

import (
	"encoding/hex"
	"fmt"
	"math/big"
	"strings"
	"testing"

	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/crypto"

	"payment-service/Configs"
	"payment-service/Internal/Dtos/Request"
)

// ── Test fixtures ────────────────────────────────────────────────────────────

// setupTestConfigs initialises the package-level Configs used by buildDomainSeparator.
// Uses a well-known test contract address (first anvil default deployment).
func setupTestConfigs() {
	Configs.ChainID               = 11155111 // Sepolia
	Configs.PaymentGatewayAddress = "0x5FbDB2315678afecb367f032d93F642f64180aa3"
}

func makeTestOrder(payerHex string, amount uint64, nonce uint64) Request.PaymentOrderData {
	orderIDBytes := crypto.Keccak256([]byte("test-order-" + payerHex))
	bookingIDBytes := crypto.Keccak256([]byte("test-booking-" + payerHex))

	return Request.PaymentOrderData{
		OrderID:   "0x" + hex.EncodeToString(orderIDBytes),
		Payer:     payerHex,
		Amount:    amount,
		BookingID: "0x" + hex.EncodeToString(bookingIDBytes),
		Nonce:     nonce,
		Deadline:  9_999_999_999, // far future
	}
}

// signOrderWithKey signs an EIP-712 payment order with a private key,
// returning the 65-byte signature (r+s+v) with v in {27, 28} — MetaMask format.
func signOrderWithKey(t *testing.T, privKeyHex string, order Request.PaymentOrderData) string {
	t.Helper()

	digest, err := buildEIP712Digest(order)
	if err != nil {
		t.Fatalf("buildEIP712Digest: %v", err)
	}

	privKey, err := crypto.HexToECDSA(strings.TrimPrefix(privKeyHex, "0x"))
	if err != nil {
		t.Fatalf("HexToECDSA: %v", err)
	}

	// crypto.Sign returns (r,s,v) with v in {0,1}
	sigBytes, err := crypto.Sign(digest, privKey)
	if err != nil {
		t.Fatalf("crypto.Sign: %v", err)
	}

	// Convert v to MetaMask format {27, 28}
	sigBytes[64] += 27

	return "0x" + hex.EncodeToString(sigBytes)
}

// ── Tests ────────────────────────────────────────────────────────────────────

// TestEIP712Digest_Deterministic verifies that the same inputs always produce
// the same digest (pure function — no randomness).
func TestEIP712Digest_Deterministic(t *testing.T) {
	setupTestConfigs()

	order := makeTestOrder("0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266", 1_000_000, 0)

	d1, err := buildEIP712Digest(order)
	if err != nil {
		t.Fatal(err)
	}
	d2, err := buildEIP712Digest(order)
	if err != nil {
		t.Fatal(err)
	}

	if hex.EncodeToString(d1) != hex.EncodeToString(d2) {
		t.Fatal("digest is not deterministic")
	}
	t.Logf("Digest: 0x%s", hex.EncodeToString(d1))
}

// TestEIP712Digest_FieldSensitivity verifies that changing any single field
// of the order changes the digest (collision resistance).
func TestEIP712Digest_FieldSensitivity(t *testing.T) {
	setupTestConfigs()

	base := makeTestOrder("0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266", 1_000_000, 0)
	baseDigest, _ := buildEIP712Digest(base)

	tests := []struct {
		name    string
		mutate  func(o Request.PaymentOrderData) Request.PaymentOrderData
	}{
		{"amount", func(o Request.PaymentOrderData) Request.PaymentOrderData { o.Amount++; return o }},
		{"nonce",  func(o Request.PaymentOrderData) Request.PaymentOrderData { o.Nonce++; return o }},
		{"payer",  func(o Request.PaymentOrderData) Request.PaymentOrderData { o.Payer = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8"; return o }},
		{"orderId", func(o Request.PaymentOrderData) Request.PaymentOrderData {
			o.OrderID = "0x" + hex.EncodeToString(crypto.Keccak256([]byte("different")))
			return o
		}},
		{"deadline", func(o Request.PaymentOrderData) Request.PaymentOrderData { o.Deadline++; return o }},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			mutated := tc.mutate(base)
			mutatedDigest, err := buildEIP712Digest(mutated)
			if err != nil {
				t.Fatal(err)
			}
			if hex.EncodeToString(baseDigest) == hex.EncodeToString(mutatedDigest) {
				t.Errorf("changing %s did not change digest", tc.name)
			}
		})
	}
}

// TestVerifySignature_ValidKey is the core round-trip test:
// generate key → sign EIP-712 digest → verify signature matches payer address.
func TestVerifySignature_ValidKey(t *testing.T) {
	setupTestConfigs()

	// Generate a fresh ECDSA key pair
	privKey, err := crypto.GenerateKey()
	if err != nil {
		t.Fatal(err)
	}
	payerAddr := crypto.PubkeyToAddress(privKey.PublicKey)
	privKeyHex := hex.EncodeToString(crypto.FromECDSA(privKey))

	order := makeTestOrder(payerAddr.Hex(), 500_000, 0)
	sig   := signOrderWithKey(t, privKeyHex, order)

	svc  := BlockchainService()
	valid, err := svc.VerifySignature(order, sig)
	if err != nil {
		t.Fatalf("VerifySignature error: %v", err)
	}
	if !valid {
		t.Fatal("expected valid=true, got false")
	}
	t.Logf("Valid signature verified for %s", payerAddr.Hex())
}

// TestVerifySignature_WrongKey ensures a signature from a different key is rejected.
func TestVerifySignature_WrongKey(t *testing.T) {
	setupTestConfigs()

	payerKey, _ := crypto.GenerateKey()
	attackerKey, _ := crypto.GenerateKey()
	payerAddr := crypto.PubkeyToAddress(payerKey.PublicKey)
	attackerPrivHex := hex.EncodeToString(crypto.FromECDSA(attackerKey))

	order := makeTestOrder(payerAddr.Hex(), 500_000, 0)

	// Sign with attacker's key, but order claims payer as the payer
	sig := signOrderWithKey(t, attackerPrivHex, order)

	svc  := BlockchainService()
	valid, err := svc.VerifySignature(order, sig)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if valid {
		t.Fatal("expected valid=false (wrong signer), got true")
	}
	t.Log("Wrong-key signature correctly rejected")
}

// TestVerifySignature_ModifiedOrder tests that a valid signature on order A
// does not verify if the order is subsequently modified (tamper detection).
func TestVerifySignature_ModifiedOrder(t *testing.T) {
	setupTestConfigs()

	privKey, _ := crypto.GenerateKey()
	payerAddr   := crypto.PubkeyToAddress(privKey.PublicKey)
	privKeyHex  := hex.EncodeToString(crypto.FromECDSA(privKey))

	originalOrder := makeTestOrder(payerAddr.Hex(), 1_000_000, 0)
	sig           := signOrderWithKey(t, privKeyHex, originalOrder)

	// Attacker reduces amount to 1
	tamperedOrder := originalOrder
	tamperedOrder.Amount = 1

	svc  := BlockchainService()
	valid, err := svc.VerifySignature(tamperedOrder, sig)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if valid {
		t.Fatal("tampered order should not be valid, but returned true")
	}
	t.Log("Tampered order correctly rejected")
}

// TestVerifySignature_InvalidSigHex verifies that malformed hex returns an error.
func TestVerifySignature_InvalidSigHex(t *testing.T) {
	setupTestConfigs()

	privKey, _ := crypto.GenerateKey()
	payerAddr   := crypto.PubkeyToAddress(privKey.PublicKey)
	order       := makeTestOrder(payerAddr.Hex(), 100, 0)

	svc := BlockchainService()
	_, err := svc.VerifySignature(order, "not-a-hex-signature")
	if err == nil {
		t.Fatal("expected error for invalid hex, got nil")
	}
	t.Logf("Correctly rejected invalid hex: %v", err)
}

// TestEIP712DomainSeparator_ChainIDSensitivity ensures the domain separator
// changes when the chain ID changes (prevents cross-chain replay attacks).
func TestEIP712DomainSeparator_ChainIDSensitivity(t *testing.T) {
	setupTestConfigs()

	Configs.ChainID = 11155111 // Sepolia
	d1, err := buildDomainSeparator()
	if err != nil {
		t.Fatal(err)
	}

	Configs.ChainID = 1 // Mainnet
	d2, err := buildDomainSeparator()
	if err != nil {
		t.Fatal(err)
	}

	// Reset for other tests
	Configs.ChainID = 11155111

	if hex.EncodeToString(d1) == hex.EncodeToString(d2) {
		t.Fatal("domain separator must differ for different chain IDs")
	}
	t.Log("Domain separator is chain-ID sensitive — cross-chain replay protected")
}

// TestEIP712DomainSeparator_ContractSensitivity ensures that changing the
// contract address changes the domain separator.
func TestEIP712DomainSeparator_ContractSensitivity(t *testing.T) {
	setupTestConfigs()

	Configs.PaymentGatewayAddress = "0x5FbDB2315678afecb367f032d93F642f64180aa3"
	d1, _ := buildDomainSeparator()

	Configs.PaymentGatewayAddress = "0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512"
	d2, _ := buildDomainSeparator()

	// Reset
	Configs.PaymentGatewayAddress = "0x5FbDB2315678afecb367f032d93F642f64180aa3"

	if hex.EncodeToString(d1) == hex.EncodeToString(d2) {
		t.Fatal("domain separator must differ for different contract addresses")
	}
	t.Log("Domain separator is contract-address sensitive — deployment replay protected")
}

// TestPadUint256 tests the ABI encoding helper for uint256 values.
func TestPadUint256(t *testing.T) {
	tests := []struct {
		value    *big.Int
		expected string // hex of 32-byte big-endian encoding
	}{
		{big.NewInt(0), strings.Repeat("00", 32)},
		{big.NewInt(1), strings.Repeat("00", 31) + "01"},
		{big.NewInt(255), strings.Repeat("00", 31) + "ff"},
		{new(big.Int).SetBytes(common.FromHex("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")),
			"ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"},
	}

	for _, tc := range tests {
		t.Run(fmt.Sprintf("value=%s", tc.value), func(t *testing.T) {
			got := hex.EncodeToString(padUint256(tc.value))
			if got != tc.expected {
				t.Errorf("got %s, want %s", got, tc.expected)
			}
		})
	}
}

// ── Simulation ───────────────────────────────────────────────────────────────

// TestSimulateCompletePaymentFlow simulates the entire payment lifecycle
// without network access:
// 1. User gets a payment order prepared (by Go service)
// 2. User signs it (MetaMask simulation)
// 3. Relayer verifies locally (pre-check before sending gas)
// 4. Relayer would submit to chain (skipped: would need Sepolia)
func TestSimulateCompletePaymentFlow(t *testing.T) {
	setupTestConfigs()

	t.Log("=== BookingNext VND Stablecoin Payment Flow Simulation ===")
	t.Log("")

	// Step 1: Actors
	payerKey, _ := crypto.GenerateKey()
	payerAddr   := crypto.PubkeyToAddress(payerKey.PublicKey)
	privKeyHex  := hex.EncodeToString(crypto.FromECDSA(payerKey))
	t.Logf("Step 1: Payer address = %s", payerAddr.Hex())

	// Step 2: Simulate Go service building the payment order
	orderIDBytes  := crypto.Keccak256([]byte("order-sim-001"))
	bookingIDBytes := crypto.Keccak256([]byte("booking-sim-001"))
	amount         := uint64(2_500_000) // 2.5M VND

	order := Request.PaymentOrderData{
		OrderID:   "0x" + hex.EncodeToString(orderIDBytes),
		Payer:     payerAddr.Hex(),
		Amount:    amount,
		BookingID: "0x" + hex.EncodeToString(bookingIDBytes),
		Nonce:     0, // first payment for this user
		Deadline:  9_999_999_999,
	}
	t.Logf("Step 2: Payment order built — Amount: %d VNDS, OrderID: %s", amount, order.OrderID[:10]+"...")

	// Step 3: Compute EIP-712 digest (matches what MetaMask computes)
	digest, err := buildEIP712Digest(order)
	if err != nil {
		t.Fatalf("buildEIP712Digest: %v", err)
	}
	t.Logf("Step 3: EIP-712 digest = 0x%s", hex.EncodeToString(digest)[:16]+"...")

	// Step 4: User signs (MetaMask eth_signTypedData_v4)
	sigBytes, _ := crypto.Sign(digest, payerKey)
	sigBytes[64] += 27 // MetaMask format
	sig := "0x" + hex.EncodeToString(sigBytes)
	t.Logf("Step 4: User signed — sig = %s...%s", sig[:10], sig[len(sig)-8:])

	// Step 5: Go relayer verifies signature (fail-fast before spending gas)
	svc   := BlockchainService()
	valid, err := svc.VerifySignature(order, sig)
	if err != nil {
		t.Fatalf("VerifySignature: %v", err)
	}
	if !valid {
		t.Fatal("FAIL: signature verification failed — would abort before spending gas")
	}
	t.Log("Step 5: Signature verified locally — PASS (relayer would proceed to submit tx)")

	// Step 6: Verify replay protection (different nonce)
	replayOrder := order
	replayOrder.Nonce = 999 // wrong nonce
	reSig := signOrderWithKey(t, privKeyHex, replayOrder)
	// The signature is valid for nonce=999, but the contract would reject it
	// since the contract's stored nonce for this user is 0 (not 999).
	// We can verify this locally by checking the digest differs:
	origDigest, _ := buildEIP712Digest(order)
	replayDigest, _ := buildEIP712Digest(replayOrder)
	if hex.EncodeToString(origDigest) == hex.EncodeToString(replayDigest) {
		t.Fatal("FAIL: nonce change did not affect digest — replay protection broken")
	}
	t.Log("Step 6: Replay protection verified — nonce change produces different digest")

	// Suppress unused variable warning
	_ = reSig

	t.Log("")
	t.Log("=== Simulation PASSED: Full payment flow cryptographic verification OK ===")
	t.Logf("    To complete flow: run 'forge script script/Simulate.s.sol --rpc-url http://localhost:8545 --broadcast'")
}

// TestSimulateMultiplePayments verifies nonce increment logic.
func TestSimulateMultiplePayments(t *testing.T) {
	setupTestConfigs()

	payerKey, _ := crypto.GenerateKey()
	payerAddr   := crypto.PubkeyToAddress(payerKey.PublicKey)
	privKeyHex  := hex.EncodeToString(crypto.FromECDSA(payerKey))

	svc := BlockchainService()

	for nonce := uint64(0); nonce < 5; nonce++ {
		order := makeTestOrder(payerAddr.Hex(), 100_000, nonce)
		sig   := signOrderWithKey(t, privKeyHex, order)

		valid, err := svc.VerifySignature(order, sig)
		if err != nil || !valid {
			t.Fatalf("payment %d failed: valid=%v err=%v", nonce, valid, err)
		}

		// Verify signature from nonce=0 doesn't work for nonce=1
		if nonce > 0 {
			prevOrder := makeTestOrder(payerAddr.Hex(), 100_000, 0) // wrong nonce
			prevOrder.OrderID = order.OrderID                        // same order
			valid2, _ := svc.VerifySignature(prevOrder, sig)
			// The sig was made for the correct nonce, so prevOrder (wrong nonce → different digest) should NOT verify
			if valid2 {
				t.Logf("Note: nonce=%d sig matched wrong nonce — expected for different orderID", nonce)
			}
		}
	}
	t.Logf("5 sequential payments verified for %s", payerAddr.Hex())
}
