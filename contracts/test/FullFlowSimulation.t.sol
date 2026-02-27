// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import "forge-std/Test.sol";
import "../src/VNDToken.sol";
import "../src/PaymentGateway.sol";

/// @title FullFlowSimulation
/// @notice End-to-end simulation of the EIP-712 VND Stablecoin payment flow.
///         Mirrors the exact sequence: Frontend → Java → Go relayer → Sepolia.
///
/// Run: forge test --match-contract FullFlowSimulation -vvv
contract FullFlowSimulation is Test {
    VNDToken    internal token;
    PaymentGateway internal gateway;

    address internal owner;         // Deployer / relayer
    uint256 internal payerPrivKey = 0xDEADBEEF1234;
    address internal payer;
    address internal relayer;       // Same as owner in this test

    // ─── Setup ────────────────────────────────────────────────────────────────

    function setUp() public {
        owner   = address(this);
        relayer = owner;
        payer   = vm.addr(payerPrivKey);

        // 1. Deploy contracts
        token   = new VNDToken(owner);
        gateway = new PaymentGateway(address(token), owner);

        // 2. Mint VNDS to payer (simulate user acquiring stablecoins)
        token.mint(payer, 100_000_000); // 100M VNDS

        // 3. User approves gateway (max allowance — mirrors "approve all" in dApp)
        vm.prank(payer);
        token.approve(address(gateway), type(uint256).max);
    }

    // ─── Helper: build & sign ─────────────────────────────────────────────────

    function _makeOrder(
        bytes32 orderId,
        uint256 amount,
        bytes32 bookingId
    ) internal view returns (PaymentGateway.PaymentOrder memory) {
        return PaymentGateway.PaymentOrder({
            orderId:   orderId,
            payer:     payer,
            amount:    amount,
            bookingId: bookingId,
            nonce:     gateway.getNonce(payer),
            deadline:  block.timestamp + 3600
        });
    }

    function _sign(PaymentGateway.PaymentOrder memory order) internal view returns (bytes memory) {
        bytes32 structHash = keccak256(abi.encode(
            gateway.PAYMENT_ORDER_TYPEHASH(),
            order.orderId,
            order.payer,
            order.amount,
            order.bookingId,
            order.nonce,
            order.deadline
        ));
        bytes32 digest = keccak256(abi.encodePacked("\x19\x01", gateway.domainSeparator(), structHash));
        (uint8 v, bytes32 r, bytes32 s) = vm.sign(payerPrivKey, digest);
        return abi.encodePacked(r, s, v);
    }

    // ─── Scenario 1: Standard payment flow ───────────────────────────────────

    function test_Scenario_StandardPayment() public {
        uint256 amount    = 2_000_000; // 2,000,000 VND
        bytes32 orderId   = keccak256(abi.encodePacked("booking-001", block.timestamp));
        bytes32 bookingId = keccak256("booking-001");

        PaymentGateway.PaymentOrder memory order = _makeOrder(orderId, amount, bookingId);
        bytes memory sig = _sign(order);

        // Snapshot balances before
        uint256 payerBefore   = token.balanceOf(payer);
        uint256 gatewayBefore = token.balanceOf(address(gateway));

        // Relayer submits the payment (user doesn't spend gas)
        vm.prank(relayer);

        vm.expectEmit(true, true, false, true);
        emit PaymentGateway.PaymentProcessed(orderId, payer, amount, bookingId, block.timestamp);

        gateway.executePayment(order, sig);

        // Assertions
        assertEq(token.balanceOf(payer),            payerBefore - amount,   "Payer balance decreased");
        assertEq(token.balanceOf(address(gateway)), gatewayBefore + amount, "Gateway balance increased");
        assertTrue(gateway.processedOrders(orderId),                        "Order marked processed");
        assertEq(gateway.getNonce(payer), 1,                                "Nonce incremented");
    }

    // ─── Scenario 2: Multiple sequential payments ─────────────────────────────

    function test_Scenario_MultiplePayments() public {
        uint256[3] memory amounts = [uint256(500_000), 1_200_000, 3_500_000];
        uint256 totalExpected = 0;

        for (uint256 i = 0; i < amounts.length; i++) {
            bytes32 orderId   = keccak256(abi.encodePacked("order", i));
            bytes32 bookingId = keccak256(abi.encodePacked("booking", i));

            PaymentGateway.PaymentOrder memory order = _makeOrder(orderId, amounts[i], bookingId);
            bytes memory sig = _sign(order);

            vm.prank(relayer);
            gateway.executePayment(order, sig);

            totalExpected += amounts[i];
        }

        assertEq(token.balanceOf(address(gateway)), totalExpected, "Total gateway balance correct");
        assertEq(gateway.getNonce(payer), 3, "Nonce = 3 after 3 payments");
    }

    // ─── Scenario 3: Permit-based approval + payment ──────────────────────────

    function test_Scenario_PermitApprovalThenPayment() public {
        // Create a fresh payer (no prior approval)
        uint256 freshPrivKey = 0xFEED;
        address freshPayer   = vm.addr(freshPrivKey);
        uint256 amount       = 1_000_000;

        token.mint(freshPayer, 10_000_000);

        // Payer signs a permit (gasless approval via EIP-2612)
        uint256 deadline    = block.timestamp + 3600;
        uint256 permitNonce = token.nonces(freshPayer);

        bytes32 permitHash = keccak256(abi.encodePacked(
            "\x19\x01",
            token.DOMAIN_SEPARATOR(),
            keccak256(abi.encode(
                keccak256("Permit(address owner,address spender,uint256 value,uint256 nonce,uint256 deadline)"),
                freshPayer,
                address(gateway),
                type(uint256).max,
                permitNonce,
                deadline
            ))
        ));
        (uint8 pv, bytes32 pr, bytes32 ps) = vm.sign(freshPrivKey, permitHash);

        // Anyone (relayer) can submit the permit on-chain
        token.permit(freshPayer, address(gateway), type(uint256).max, deadline, pv, pr, ps);
        assertEq(token.allowance(freshPayer, address(gateway)), type(uint256).max, "Allowance set via permit");

        // Now create and sign the payment order
        bytes32 orderId   = keccak256("permit-order-1");
        bytes32 bookingId = keccak256("booking-permit-1");

        PaymentGateway.PaymentOrder memory order = PaymentGateway.PaymentOrder({
            orderId:   orderId,
            payer:     freshPayer,
            amount:    amount,
            bookingId: bookingId,
            nonce:     gateway.getNonce(freshPayer),
            deadline:  block.timestamp + 3600
        });

        bytes32 structHash = keccak256(abi.encode(
            gateway.PAYMENT_ORDER_TYPEHASH(),
            order.orderId, order.payer, order.amount,
            order.bookingId, order.nonce, order.deadline
        ));
        bytes32 digest = keccak256(abi.encodePacked("\x19\x01", gateway.domainSeparator(), structHash));
        (uint8 v, bytes32 r, bytes32 s) = vm.sign(freshPrivKey, digest);
        bytes memory sig = abi.encodePacked(r, s, v);

        vm.prank(relayer);
        gateway.executePayment(order, sig);

        assertEq(token.balanceOf(freshPayer), 10_000_000 - amount, "Permit + payment OK");
    }

    // ─── Scenario 4: Refund flow ──────────────────────────────────────────────

    function test_Scenario_PaymentAndRefund() public {
        uint256 amount    = 5_000_000;
        bytes32 orderId   = keccak256("refund-order-1");
        bytes32 bookingId = keccak256("refund-booking-1");

        PaymentGateway.PaymentOrder memory order = _makeOrder(orderId, amount, bookingId);
        bytes memory sig = _sign(order);

        uint256 payerBalanceBefore = token.balanceOf(payer);

        // Execute payment
        vm.prank(relayer);
        gateway.executePayment(order, sig);

        assertEq(token.balanceOf(payer), payerBalanceBefore - amount, "Payment deducted");

        // Admin issues refund
        vm.expectEmit(true, true, false, true);
        emit PaymentGateway.Refunded(orderId, payer, amount);

        gateway.refund(orderId, payer, amount);

        assertEq(token.balanceOf(payer), payerBalanceBefore, "Refund restored balance");
    }

    // ─── Scenario 5: Concurrent orders from different users ──────────────────

    function test_Scenario_MultipleUsers() public {
        uint256 user2PrivKey = 0xBEEFCAFE;
        address user2        = vm.addr(user2PrivKey);
        token.mint(user2, 10_000_000);
        vm.prank(user2);
        token.approve(address(gateway), type(uint256).max);

        // User 1 payment
        PaymentGateway.PaymentOrder memory order1 = _makeOrder(keccak256("order-u1"), 1_000_000, keccak256("booking-u1"));
        bytes memory sig1 = _sign(order1);
        vm.prank(relayer);
        gateway.executePayment(order1, sig1);

        // User 2 payment (different nonce namespace)
        PaymentGateway.PaymentOrder memory order2 = PaymentGateway.PaymentOrder({
            orderId:   keccak256("order-u2"),
            payer:     user2,
            amount:    2_000_000,
            bookingId: keccak256("booking-u2"),
            nonce:     gateway.getNonce(user2),
            deadline:  block.timestamp + 3600
        });

        bytes32 sh = keccak256(abi.encode(
            gateway.PAYMENT_ORDER_TYPEHASH(),
            order2.orderId, order2.payer, order2.amount,
            order2.bookingId, order2.nonce, order2.deadline
        ));
        bytes32 d = keccak256(abi.encodePacked("\x19\x01", gateway.domainSeparator(), sh));
        (uint8 v, bytes32 r, bytes32 s) = vm.sign(user2PrivKey, d);
        bytes memory sig2 = abi.encodePacked(r, s, v);

        vm.prank(relayer);
        gateway.executePayment(order2, sig2);

        // Nonces are independent
        assertEq(gateway.getNonce(payer), 1,  "User1 nonce = 1");
        assertEq(gateway.getNonce(user2), 1,  "User2 nonce = 1");
        assertEq(token.balanceOf(address(gateway)), 3_000_000, "Gateway holds both payments");
    }

    // ─── Scenario 6: Gas usage benchmarks ────────────────────────────────────

    function test_Benchmark_ExecutePaymentGas() public {
        PaymentGateway.PaymentOrder memory order = _makeOrder(keccak256("bench-order"), 1_000_000, keccak256("bench-booking"));
        bytes memory sig = _sign(order);

        uint256 gasBefore = gasleft();
        vm.prank(relayer);
        gateway.executePayment(order, sig);
        uint256 gasUsed = gasBefore - gasleft();

        emit log_named_uint("executePayment gas used", gasUsed);
        // Ensure it's under 200k gas (reasonable budget for Sepolia)
        assertLt(gasUsed, 200_000, "Gas usage within budget");
    }

    // ─── Security: tampered signature cannot execute ──────────────────────────

    function test_Security_TamperedSignatureRejected() public {
        PaymentGateway.PaymentOrder memory order = _makeOrder(keccak256("tamper-order"), 1_000_000, keccak256("tamper-booking"));
        bytes memory sig = _sign(order);

        // Flip a bit in the signature
        sig[32] ^= 0xFF;

        vm.prank(relayer);
        vm.expectRevert();
        gateway.executePayment(order, sig);
    }

    // ─── Security: order amount cannot be changed after signing ──────────────

    function test_Security_ModifiedAmountRejected() public {
        PaymentGateway.PaymentOrder memory order = _makeOrder(keccak256("mod-order"), 1_000_000, keccak256("mod-booking"));
        bytes memory sig = _sign(order);

        // Attacker tries to modify amount to 1 (pay less)
        order.amount = 1;

        vm.prank(relayer);
        vm.expectRevert("Invalid signature");
        gateway.executePayment(order, sig);
    }

    // ─── Withdraw ─────────────────────────────────────────────────────────────

    function test_Withdraw() public {
        uint256 amount = 3_000_000;
        PaymentGateway.PaymentOrder memory order = _makeOrder(keccak256("withdraw-order"), amount, keccak256("withdraw-booking"));
        bytes memory sig = _sign(order);

        vm.prank(relayer);
        gateway.executePayment(order, sig);

        address treasury = makeAddr("treasury");
        gateway.withdraw(treasury, amount);
        assertEq(token.balanceOf(treasury), amount, "Treasury received funds");
    }
}
