#!/usr/bin/env bash
# ============================================================
#  BookingNext — EIP-712 VND Stablecoin Payment Simulation
#  Run this script from: booking-be/contracts/
# ============================================================
set -e

CONTRACTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$CONTRACTS_DIR"

# ── Colour helpers ──────────────────────────────────────────
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; CYAN='\033[0;36m'; NC='\033[0m'
ok()   { echo -e "${GREEN}✓ $*${NC}"; }
info() { echo -e "${CYAN}▶ $*${NC}"; }
warn() { echo -e "${YELLOW}⚠ $*${NC}"; }
fail() { echo -e "${RED}✗ $*${NC}"; exit 1; }

echo ""
echo -e "${CYAN}============================================================${NC}"
echo -e "${CYAN}  BookingNext EIP-712 Payment Simulation${NC}"
echo -e "${CYAN}============================================================${NC}"
echo ""

# ── 1. Check dependencies ───────────────────────────────────
info "Checking dependencies..."
command -v forge >/dev/null 2>&1 || fail "forge not found. Install Foundry: https://getfoundry.sh"
command -v anvil >/dev/null 2>&1 || fail "anvil not found. Install Foundry: https://getfoundry.sh"
ok  "Foundry (forge + anvil) found"

# ── 2. Install forge-std if missing ────────────────────────
if [ ! -d "lib/forge-std" ]; then
  info "Installing forge-std..."
  forge install foundry-rs/forge-std --no-git
  ok "forge-std installed"
fi

# ── 3. Install OpenZeppelin if missing ──────────────────────
if [ ! -d "lib/openzeppelin-contracts" ]; then
  info "Installing OpenZeppelin contracts..."
  forge install OpenZeppelin/openzeppelin-contracts --no-git
  ok "OpenZeppelin installed"
fi

# ── 4. Build contracts ──────────────────────────────────────
info "Building contracts..."
forge build --silent
ok "Contracts compiled"

# ── 5. Run unit tests ────────────────────────────────────────
echo ""
info "Running unit tests..."
echo ""

forge test --match-contract "VNDTokenTest" -v 2>&1 | grep -E "(PASS|FAIL|Running|Gas)" || true
forge test --match-contract "PaymentGatewayTest" -v 2>&1 | grep -E "(PASS|FAIL|Running|Gas)" || true

echo ""
info "Running full flow simulation tests..."
echo ""
forge test --match-contract "FullFlowSimulation" -vv 2>&1

# Check test results
if forge test --silent 2>&1 | grep -q "FAIL"; then
  fail "One or more tests failed. Fix before simulation."
fi
ok "All tests passed"

# ── 6. Start local anvil node ────────────────────────────────
echo ""
info "Starting local Anvil node on port 8545..."
anvil --silent &
ANVIL_PID=$!
sleep 2

# Ensure cleanup on exit
trap "kill $ANVIL_PID 2>/dev/null; ok 'Anvil stopped'" EXIT

ok "Anvil running (PID: $ANVIL_PID)"

# ── 7. Run simulation script ─────────────────────────────────
echo ""
info "Running end-to-end simulation on local Anvil..."
echo ""

forge script script/Simulate.s.sol \
  --rpc-url http://localhost:8545 \
  --broadcast \
  -vv 2>&1

echo ""
ok "Simulation complete!"
echo ""
echo -e "${GREEN}============================================================${NC}"
echo -e "${GREEN}  Summary${NC}"
echo -e "${GREEN}============================================================${NC}"
echo "  • VNDToken (ERC-20 + Permit): DEPLOYED & TESTED"
echo "  • PaymentGateway (EIP-712):   DEPLOYED & TESTED"
echo "  • Gasless signature flow:     VERIFIED"
echo "  • Replay protection:          VERIFIED"
echo "  • Refund flow:                VERIFIED"
echo ""
echo "Next steps:"
echo "  1. Set env vars in .env (SEPOLIA_RPC_URL, PRIVATE_KEY, etc.)"
echo "  2. forge script script/Deploy.s.sol --rpc-url \$SEPOLIA_RPC_URL --broadcast --verify"
echo "  3. Add VND_TOKEN_ADDRESS and PAYMENT_GATEWAY_ADDRESS to Go service .env"
echo "  4. Run: go mod tidy && go run . (Go service)"
echo "  5. Run: npm install wagmi viem @tanstack/react-query (frontend)"
echo ""
