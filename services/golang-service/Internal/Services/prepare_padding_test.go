package Services

import (
	"strings"
	"testing"

	"github.com/google/uuid"

	"payment-service/Configs"
)

// TestPreparePaymentOrder_BookingIDPadding verifies the bookingId hex returned
// in the EIP-712 typed-data is exactly 32 bytes (64 hex chars + "0x"), with the
// raw UUID left-aligned and trailing zero padding — matching what the on-chain
// ABI encoder produces from copy(bookingIDBytes[:], uuid[:]).
//
// Regression guard for the bug where a raw 16-byte UUID hex caused
// viem v2 to reject the typed-data with "Expected bytes32, got bytes16".
func TestPreparePaymentOrder_BookingIDPadding(t *testing.T) {
	// Use a public Sepolia RPC for the nonce lookup; PaymentGateway address
	// from a known deployment so getNonce returns successfully.
	Configs.SepoliaRPCURL = "https://ethereum-sepolia-rpc.publicnode.com"
	Configs.PaymentGatewayAddress = "0x05893F2f540B2A2524ff950a1c5e0276811EE92B"
	Configs.VNDTokenAddress = "0x3f8A265E3e616E485343476419aC90736e8Af3c3"
	Configs.ChainID = 11155111

	bookingID := uuid.MustParse("a9342d64-ffe9-4569-8c8c-68df2fbe7690")
	payer := "0x000000000000000000000000000000000000dEaD"

	resp, err := BlockchainService().PreparePaymentOrder(bookingID, payer, 260000)
	if err != nil {
		t.Fatalf("PreparePaymentOrder failed: %v", err)
	}

	bookingIDHex, ok := resp.TypedData.Message["bookingId"].(string)
	if !ok {
		t.Fatalf("bookingId missing from typed data message")
	}

	// Must be "0x" + 64 hex chars = 32 bytes total
	if len(bookingIDHex) != 66 {
		t.Fatalf("bookingId hex length = %d (want 66 = 0x + 64 hex chars). got: %s",
			len(bookingIDHex), bookingIDHex)
	}

	// Must start with the UUID hex (no dashes)
	rawUUID := strings.ReplaceAll(bookingID.String(), "-", "")
	if !strings.EqualFold(bookingIDHex[2:34], rawUUID) {
		t.Fatalf("bookingId prefix mismatch: got %s, want %s", bookingIDHex[2:34], rawUUID)
	}

	// Trailing 16 bytes must be zero
	if bookingIDHex[34:] != strings.Repeat("0", 32) {
		t.Fatalf("bookingId trailing pad = %s (want 32 zeros)", bookingIDHex[34:])
	}

	t.Logf("bookingId = %s (len=%d) ✓", bookingIDHex, len(bookingIDHex))
}
