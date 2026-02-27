#!/usr/bin/env bash
# ============================================================
#  BookingNext Go Service — EIP-712 Unit Tests
#  Run from: booking-be/services/golang-service/
# ============================================================
set -e

SERVICE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SERVICE_DIR"

GREEN='\033[0;32m'; CYAN='\033[0;36m'; RED='\033[0;31m'; NC='\033[0m'
ok()   { echo -e "${GREEN}✓ $*${NC}"; }
info() { echo -e "${CYAN}▶ $*${NC}"; }
fail() { echo -e "${RED}✗ $*${NC}"; exit 1; }

echo ""
echo -e "${CYAN}============================================================${NC}"
echo -e "${CYAN}  BookingNext Go EIP-712 Tests${NC}"
echo -e "${CYAN}============================================================${NC}"
echo ""

# 1. Ensure go-ethereum is present
info "Fetching go-ethereum dependency..."
go get github.com/ethereum/go-ethereum@v1.15.0 2>/dev/null || true
go mod tidy
ok "Dependencies resolved"

# 2. Build check
info "Building service..."
go build ./... && ok "Build successful"

# 3. Run EIP-712 unit tests (no network required)
echo ""
info "Running EIP-712 unit tests..."
echo ""

go test ./Internal/Services/... \
  -run "TestEIP712|TestVerifySignature|TestPadUint256|TestSimulate" \
  -v \
  -timeout 30s \
  2>&1

echo ""
ok "All EIP-712 tests completed"
echo ""
echo -e "${GREEN}============================================================${NC}"
echo "Next: run the Solidity simulation:"
echo "  cd ../../contracts && ./simulate.sh"
echo ""
