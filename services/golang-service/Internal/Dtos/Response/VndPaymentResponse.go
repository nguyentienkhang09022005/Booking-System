package Response

import "github.com/google/uuid"

// EIP712Domain represents the domain parameters for EIP-712 typed data.
type EIP712Domain struct {
	Name              string `json:"name"`
	Version           string `json:"version"`
	ChainID           int64  `json:"chainId"`
	VerifyingContract string `json:"verifyingContract"`
}

// TypeField represents a single field in an EIP-712 type definition.
type TypeField struct {
	Name string `json:"name"`
	Type string `json:"type"`
}

// EIP712TypedData is the full EIP-712 typed data structure sent to the frontend
// for use with wagmi's signTypedData / MetaMask eth_signTypedData_v4.
type EIP712TypedData struct {
	Domain      EIP712Domain              `json:"domain"`
	Types       map[string][]TypeField    `json:"types"`
	PrimaryType string                    `json:"primaryType"`
	Message     map[string]interface{}    `json:"message"`
}

type PrepareVndPaymentResponse struct {
	IDPayment       uuid.UUID       `json:"id_payment"`
	TypedData       EIP712TypedData `json:"typed_data"`
	OrderID         string          `json:"order_id"`
	VndTokenAddress string          `json:"vnd_token_address"`
}

type ExecuteVndPaymentResponse struct {
	IDPayment   uuid.UUID `json:"id_payment"`
	TxHash      string    `json:"tx_hash"`
	Status      string    `json:"status"`
	ExplorerURL string    `json:"explorer_url"`
}
