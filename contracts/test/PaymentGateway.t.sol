// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import "forge-std/Test.sol";
import "../src/VNDToken.sol";
import "../src/PaymentGateway.sol";

contract PaymentGatewayTest is Test {
    VNDToken internal vndToken;
    PaymentGateway internal gateway;

    address internal owner;
    uint256 internal payerPrivKey = 0xBEEF;
    address internal payer;

    uint256 internal constant INITIAL_MINT = 10_000_000; // 10M VNDS

    function setUp() public {
        owner = address(this);
        payer = vm.addr(payerPrivKey);

        vndToken = new VNDToken(owner);
        gateway = new PaymentGateway(address(vndToken), owner);

        // Mint tokens to payer and approve gateway
        vndToken.mint(payer, INITIAL_MINT);
        vm.prank(payer);
        vndToken.approve(address(gateway), type(uint256).max);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    function _buildOrder(
        bytes32 orderId,
        uint256 amount,
        bytes32 bookingId,
        uint256 nonce,
        uint256 deadline
    ) internal view returns (PaymentGateway.PaymentOrder memory) {
        return PaymentGateway.PaymentOrder({
            orderId:   orderId,
            payer:     payer,
            amount:    amount,
            bookingId: bookingId,
            nonce:     nonce,
            deadline:  deadline
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

        bytes32 digest = keccak256(abi.encodePacked(
            "\x19\x01",
            gateway.domainSeparator(),
            structHash
        ));

        (uint8 v, bytes32 r, bytes32 s) = vm.sign(payerPrivKey, digest);
        return abi.encodePacked(r, s, v);
    }

    // ─── Happy path ───────────────────────────────────────────────────────────

    function test_HappyPath() public {
        bytes32 orderId   = keccak256("order-1");
        bytes32 bookingId = keccak256("booking-1");
        uint256 amount    = 500_000;
        uint256 deadline  = block.timestamp + 3600;
        uint256 nonce     = gateway.getNonce(payer);

        PaymentGateway.PaymentOrder memory order = _buildOrder(orderId, amount, bookingId, nonce, deadline);
        bytes memory sig = _sign(order);

        uint256 payerBefore   = vndToken.balanceOf(payer);
        uint256 gatewayBefore = vndToken.balanceOf(address(gateway));

        vm.expectEmit(true, true, false, true);
        emit PaymentGateway.PaymentProcessed(orderId, payer, amount, bookingId, block.timestamp);

        gateway.executePayment(order, sig);

        assertEq(vndToken.balanceOf(payer), payerBefore - amount);
        assertEq(vndToken.balanceOf(address(gateway)), gatewayBefore + amount);
        assertTrue(gateway.processedOrders(orderId));
        assertEq(gateway.getNonce(payer), nonce + 1);
    }

    // ─── Replay attack ────────────────────────────────────────────────────────

    function test_RevertIf_ReplayAttack() public {
        bytes32 orderId   = keccak256("order-replay");
        bytes32 bookingId = keccak256("booking-replay");
        uint256 nonce     = gateway.getNonce(payer);

        PaymentGateway.PaymentOrder memory order = _buildOrder(orderId, 100_000, bookingId, nonce, block.timestamp + 3600);
        bytes memory sig = _sign(order);

        gateway.executePayment(order, sig);

        // Second call with same orderId must revert
        vm.expectRevert("Order already processed");
        gateway.executePayment(order, sig);
    }

    // ─── Expired deadline ─────────────────────────────────────────────────────

    function test_RevertIf_ExpiredDeadline() public {
        bytes32 orderId   = keccak256("order-expired");
        bytes32 bookingId = keccak256("booking-expired");
        uint256 deadline  = block.timestamp - 1; // already past
        uint256 nonce     = gateway.getNonce(payer);

        PaymentGateway.PaymentOrder memory order = _buildOrder(orderId, 100_000, bookingId, nonce, deadline);
        bytes memory sig = _sign(order);

        vm.expectRevert("Order expired");
        gateway.executePayment(order, sig);
    }

    // ─── Wrong nonce ──────────────────────────────────────────────────────────

    function test_RevertIf_WrongNonce() public {
        bytes32 orderId   = keccak256("order-nonce");
        bytes32 bookingId = keccak256("booking-nonce");
        uint256 wrongNonce = 999;

        PaymentGateway.PaymentOrder memory order = _buildOrder(orderId, 100_000, bookingId, wrongNonce, block.timestamp + 3600);
        bytes memory sig = _sign(order);

        vm.expectRevert("Invalid nonce");
        gateway.executePayment(order, sig);
    }

    // ─── Invalid signer ───────────────────────────────────────────────────────

    function test_RevertIf_InvalidSigner() public {
        bytes32 orderId   = keccak256("order-signer");
        bytes32 bookingId = keccak256("booking-signer");
        uint256 nonce     = gateway.getNonce(payer);

        PaymentGateway.PaymentOrder memory order = _buildOrder(orderId, 100_000, bookingId, nonce, block.timestamp + 3600);

        // Sign with a different private key
        uint256 attackerKey = 0xDEAD;
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
        (uint8 v, bytes32 r, bytes32 s) = vm.sign(attackerKey, digest);
        bytes memory badSig = abi.encodePacked(r, s, v);

        vm.expectRevert("Invalid signature");
        gateway.executePayment(order, badSig);
    }

    // ─── Refund flow ──────────────────────────────────────────────────────────

    function test_Refund() public {
        // First, execute a valid payment
        bytes32 orderId   = keccak256("order-refund");
        bytes32 bookingId = keccak256("booking-refund");
        uint256 amount    = 200_000;
        uint256 nonce     = gateway.getNonce(payer);

        PaymentGateway.PaymentOrder memory order = _buildOrder(orderId, amount, bookingId, nonce, block.timestamp + 3600);
        gateway.executePayment(order, _sign(order));

        uint256 payerBefore = vndToken.balanceOf(payer);

        vm.expectEmit(true, true, false, true);
        emit PaymentGateway.Refunded(orderId, payer, amount);

        gateway.refund(orderId, payer, amount);

        assertEq(vndToken.balanceOf(payer), payerBefore + amount);
    }

    // ─── Non-owner cannot refund/withdraw ─────────────────────────────────────

    function test_RevertIf_NonOwnerRefund() public {
        address attacker = address(0xBAD);
        vm.prank(attacker);
        vm.expectRevert();
        gateway.refund(keccak256("x"), payer, 100);
    }

    function test_RevertIf_NonOwnerWithdraw() public {
        address attacker = address(0xBAD);
        vm.prank(attacker);
        vm.expectRevert();
        gateway.withdraw(attacker, 100);
    }

    // ─── Additional edge cases ────────────────────────────────────────────────

    function test_RevertIf_RefundToZeroAddress() public {
        vm.expectRevert("Invalid payer address");
        gateway.refund(keccak256("x"), address(0), 100);
    }

    function test_RevertIf_WithdrawToZeroAddress() public {
        vm.expectRevert("Invalid recipient address");
        gateway.withdraw(address(0), 100);
    }

    function test_GetNonce_StartsAtZero() public view {
        assertEq(gateway.getNonce(payer), 0);
        assertEq(gateway.getNonce(address(0xCAFE)), 0);
    }

    function test_Nonce_NotSharedBetweenUsers() public {
        // User1 makes a payment
        PaymentGateway.PaymentOrder memory order1 = _buildOrder(
            keccak256("nonce-isolation-1"), 1_000, keccak256("b1"), gateway.getNonce(payer), block.timestamp + 3600
        );
        gateway.executePayment(order1, _sign(order1));
        assertEq(gateway.getNonce(payer), 1);

        // User2's nonce is still 0
        address user2 = address(0xABCD);
        assertEq(gateway.getNonce(user2), 0);
    }

    function test_DomainSeparator_IsNotZero() public view {
        assertNotEq(gateway.domainSeparator(), bytes32(0));
    }

    function test_TYPEHASH_IsCorrect() public view {
        bytes32 expected = keccak256(
            "PaymentOrder(bytes32 orderId,address payer,uint256 amount,bytes32 bookingId,uint256 nonce,uint256 deadline)"
        );
        assertEq(gateway.PAYMENT_ORDER_TYPEHASH(), expected);
    }

    function test_VndToken_IsCorrectlyLinked() public view {
        assertEq(address(gateway.vndToken()), address(vndToken));
    }

    function test_RevertIf_InsufficientAllowance() public {
        // Revoke allowance
        vm.prank(payer);
        vndToken.approve(address(gateway), 0);

        PaymentGateway.PaymentOrder memory order = _buildOrder(
            keccak256("no-allowance"), 1_000, keccak256("b"), gateway.getNonce(payer), block.timestamp + 3600
        );
        bytes memory sig = _sign(order);

        vm.expectRevert();
        gateway.executePayment(order, sig);
    }

    function test_RevertIf_InsufficientBalance() public {
        // Fresh address with zero balance
        uint256 brokePrivKey = 0xB40CE; // valid hex, no funds
        address broke = vm.addr(brokePrivKey);
        // Give allowance but no tokens
        vm.prank(broke);
        vndToken.approve(address(gateway), type(uint256).max);

        PaymentGateway.PaymentOrder memory order = PaymentGateway.PaymentOrder({
            orderId:   keccak256("broke-order"),
            payer:     broke,
            amount:    1_000,
            bookingId: keccak256("broke-booking"),
            nonce:     gateway.getNonce(broke),
            deadline:  block.timestamp + 3600
        });

        bytes32 structHash = keccak256(abi.encode(
            gateway.PAYMENT_ORDER_TYPEHASH(),
            order.orderId, order.payer, order.amount,
            order.bookingId, order.nonce, order.deadline
        ));
        bytes32 digest = keccak256(abi.encodePacked("\x19\x01", gateway.domainSeparator(), structHash));
        (uint8 v, bytes32 r, bytes32 s) = vm.sign(brokePrivKey, digest);
        bytes memory sig = abi.encodePacked(r, s, v);

        vm.expectRevert();
        gateway.executePayment(order, sig);
    }

    // ─── Fuzz tests ───────────────────────────────────────────────────────────

    function testFuzz_HappyPath(uint128 amount) public {
        vm.assume(amount > 0);
        vm.assume(amount <= INITIAL_MINT);

        bytes32 orderId   = keccak256(abi.encode("fuzz-order", amount));
        bytes32 bookingId = keccak256(abi.encode("fuzz-booking", amount));

        PaymentGateway.PaymentOrder memory order = _buildOrder(
            orderId, amount, bookingId, gateway.getNonce(payer), block.timestamp + 3600
        );
        gateway.executePayment(order, _sign(order));

        assertEq(vndToken.balanceOf(address(gateway)), amount);
    }
}
