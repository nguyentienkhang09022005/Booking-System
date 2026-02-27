// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import "forge-std/Script.sol";
import "forge-std/console.sol";
import "../src/VNDToken.sol";
import "../src/PaymentGateway.sol";

/// @notice Full end-to-end simulation of the EIP-712 payment flow on a local anvil node.
///         Mirrors the exact production flow: Java → Go relayer → Sepolia.
///
/// Run on local anvil:
///   anvil &
///   forge script script/Simulate.s.sol --rpc-url http://localhost:8545 --broadcast -vvv
///
/// Run on Sepolia:
///   forge script script/Simulate.s.sol --rpc-url $SEPOLIA_RPC_URL --broadcast --verify -vvv
contract Simulate is Script {

    // ─── Test actors ──────────────────────────────────────────────────────────
    // These are well-known anvil test keys (DO NOT use on mainnet)
    uint256 constant DEPLOYER_KEY = 0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80;
    uint256 constant PAYER_KEY    = 0x59c6995e998f97a5a0044966f0945389dc9e86dae88c7a8412f4603b6b78690d;

    function run() external {
        address deployer = vm.addr(DEPLOYER_KEY);
        address payer    = vm.addr(PAYER_KEY);

        console.log("==============================================");
        console.log("  BookingNext VND Stablecoin Payment Simulation");
        console.log("==============================================");
        console.log("Deployer / Relayer:", deployer);
        console.log("Payer:             ", payer);
        console.log("");

        // ── Step 1: Deploy contracts ──────────────────────────────────────────
        vm.startBroadcast(DEPLOYER_KEY);

        VNDToken token = new VNDToken(deployer);
        PaymentGateway gateway = new PaymentGateway(address(token), deployer);

        console.log("Step 1: Contracts deployed");
        console.log("  VNDToken:       ", address(token));
        console.log("  PaymentGateway: ", address(gateway));

        // ── Step 2: Mint VNDS to payer ────────────────────────────────────────
        uint256 mintAmount = 50_000_000; // 50M VNDS
        token.mint(payer, mintAmount);

        console.log("Step 2: Minted", mintAmount, "VNDS to payer");
        console.log("  Payer balance:", token.balanceOf(payer));

        vm.stopBroadcast();

        // ── Step 3: Payer approves the gateway ────────────────────────────────
        vm.startBroadcast(PAYER_KEY);

        token.approve(address(gateway), type(uint256).max);

        console.log("Step 3: Payer approved gateway (max allowance)");

        vm.stopBroadcast();

        // ── Step 4: Build payment order (simulates Go service PreparePaymentOrder) ──
        uint256 paymentAmount = 2_500_000; // 2.5M VND
        bytes32 bookingId     = keccak256("booking-sim-001");
        bytes32 orderId       = keccak256(abi.encodePacked(bookingId, block.timestamp));
        uint256 nonce         = gateway.getNonce(payer);
        uint256 deadline      = block.timestamp + 3600;

        console.log("");
        console.log("Step 4: Payment order prepared");
        console.log("  Amount (VND): ", paymentAmount);
        console.log("  Nonce:        ", nonce);
        console.log("  Deadline:     ", deadline);

        // ── Step 5: Payer signs EIP-712 message (simulates MetaMask signing) ──
        bytes32 structHash = keccak256(abi.encode(
            gateway.PAYMENT_ORDER_TYPEHASH(),
            orderId,
            payer,
            paymentAmount,
            bookingId,
            nonce,
            deadline
        ));
        bytes32 digest = keccak256(abi.encodePacked("\x19\x01", gateway.domainSeparator(), structHash));
        (uint8 v, bytes32 r, bytes32 s) = vm.sign(PAYER_KEY, digest);
        bytes memory signature = abi.encodePacked(r, s, v);

        console.log("Step 5: EIP-712 signature generated");
        console.log("  Domain:      BookingPaymentGateway v1");

        // ── Step 6: Relayer verifies signature and submits transaction ─────────
        PaymentGateway.PaymentOrder memory order = PaymentGateway.PaymentOrder({
            orderId:   orderId,
            payer:     payer,
            amount:    paymentAmount,
            bookingId: bookingId,
            nonce:     nonce,
            deadline:  deadline
        });

        uint256 payerBalanceBefore = token.balanceOf(payer);

        vm.startBroadcast(DEPLOYER_KEY); // Relayer submits tx

        gateway.executePayment(order, signature);

        vm.stopBroadcast();

        // ── Step 7: Verify results ────────────────────────────────────────────
        uint256 payerBalanceAfter   = token.balanceOf(payer);
        uint256 gatewayBalance      = token.balanceOf(address(gateway));
        bool    orderProcessed      = gateway.processedOrders(orderId);
        uint256 newNonce            = gateway.getNonce(payer);

        console.log("");
        console.log("Step 6: Relayer submitted executePayment transaction");
        console.log("");
        console.log("=== Results ===");
        console.log("Payer balance before:   ", payerBalanceBefore);
        console.log("Payer balance after:    ", payerBalanceAfter);
        console.log("Gateway balance:        ", gatewayBalance);
        console.log("Order processed:        ", orderProcessed);
        console.log("Payer nonce (new):      ", newNonce);
        console.log("");

        // Assertions
        require(payerBalanceAfter == payerBalanceBefore - paymentAmount, "FAIL: Payer balance incorrect");
        require(gatewayBalance == paymentAmount, "FAIL: Gateway balance incorrect");
        require(orderProcessed, "FAIL: Order not marked as processed");
        require(newNonce == nonce + 1, "FAIL: Nonce not incremented");

        console.log("All assertions PASSED!");
        console.log("");
        console.log("=== Simulation Complete ===");
        console.log("The EIP-712 VND Stablecoin payment flow works correctly.");
        console.log("Users sign gaslessly - the relayer pays gas on their behalf.");
    }
}
