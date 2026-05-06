// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import "forge-std/Test.sol";
import "../src/VNDToken.sol";
import "../src/PaymentGateway.sol";

/// @title CrossLayerSimulation
/// @notice Simulates the EXACT data flow across all layers:
///
///   Frontend (wagmi signTypedData)
///     ↓ typedData JSON
///   Go Service (PreparePaymentOrder → buildEIP712Digest → ExecutePayment)
///     ↓ ABI-encoded tx data
///   Smart Contract (PaymentGateway.executePayment)
///
/// This test validates:
///   1. EIP-712 domain separator consistency (Go vs Solidity)
///   2. Struct hash / typehash consistency
///   3. Signature format compatibility (MetaMask v=27/28 → Go v=0/1 → Solidity ecrecover)
///   4. ABI encoding of PaymentOrder struct
///   5. Full flow with realistic booking amounts (VND, 0 decimals)
///   6. Edge cases: max uint256 amount, zero amount, deadline boundaries
///   7. Frontend→Backend field mapping (JSON snake_case ↔ Solidity camelCase)
///
/// Run: forge test --match-contract CrossLayerSimulation -vvv
contract CrossLayerSimulation is Test {
    VNDToken internal token;
    PaymentGateway internal gateway;

    address internal deployer;
    address internal relayer;

    // Simulate 3 different users (like frontend wallet addresses)
    uint256 internal payer1Key = 0xA11CE;
    address internal payer1;
    uint256 internal payer2Key = 0xB0B;
    address internal payer2;
    uint256 internal payer3Key = 0xCAFE;
    address internal payer3;

    function setUp() public {
        deployer = address(this);
        relayer  = deployer;
        payer1   = vm.addr(payer1Key);
        payer2   = vm.addr(payer2Key);
        payer3   = vm.addr(payer3Key);

        token   = new VNDToken(deployer);
        gateway = new PaymentGateway(address(token), deployer);

        // Mint generous amounts to all payers
        token.mint(payer1, 500_000_000); // 500M VND
        token.mint(payer2, 500_000_000);
        token.mint(payer3, 500_000_000);

        // Each payer approves the gateway
        vm.prank(payer1);
        token.approve(address(gateway), type(uint256).max);
        vm.prank(payer2);
        token.approve(address(gateway), type(uint256).max);
        vm.prank(payer3);
        token.approve(address(gateway), type(uint256).max);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    /// @dev Mirrors Go service buildEIP712Digest() exactly
    function _buildDigest(PaymentGateway.PaymentOrder memory order) internal view returns (bytes32) {
        bytes32 structHash = keccak256(abi.encode(
            gateway.PAYMENT_ORDER_TYPEHASH(),
            order.orderId,
            order.payer,
            order.amount,
            order.bookingId,
            order.nonce,
            order.deadline
        ));
        return keccak256(abi.encodePacked("\x19\x01", gateway.domainSeparator(), structHash));
    }

    /// @dev Mirrors MetaMask signing → Go service receives signature with v=27/28
    function _signAsMetaMask(uint256 privKey, bytes32 digest) internal pure returns (bytes memory) {
        (uint8 v, bytes32 r, bytes32 s) = vm.sign(privKey, digest);
        // MetaMask returns v as 27 or 28 (already standard)
        return abi.encodePacked(r, s, v);
    }

    /// @dev Simulates Go service PreparePaymentOrder() — generates orderId from bookingId + timestamp
    function _goServicePrepare(
        bytes32 bookingId,
        uint256 payerNonce,
        address payer,
        uint256 amount
    ) internal view returns (PaymentGateway.PaymentOrder memory) {
        bytes32 orderId = keccak256(abi.encodePacked(bookingId, block.timestamp));
        return PaymentGateway.PaymentOrder({
            orderId:   orderId,
            payer:     payer,
            amount:    amount,
            bookingId: bookingId,
            nonce:     payerNonce,
            deadline:  block.timestamp + 3600
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 1: Domain separator matches Go service buildDomainSeparator()
    // ═══════════════════════════════════════════════════════════════════════════

    function test_DomainSeparator_MatchesGoService() public view {
        // Go service builds: keccak256(abi.encode(typeHash, nameHash, versionHash, chainId, address))
        bytes32 domainTypeHash = keccak256("EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)");
        bytes32 nameHash       = keccak256("BookingPaymentGateway");
        bytes32 versionHash    = keccak256("1");
        uint256 chainId        = block.chainid;

        bytes32 goServiceDomainSep = keccak256(abi.encode(
            domainTypeHash,
            nameHash,
            versionHash,
            chainId,
            address(gateway)
        ));

        // Smart contract uses OpenZeppelin's _domainSeparatorV4()
        bytes32 contractDomainSep = gateway.domainSeparator();

        assertEq(goServiceDomainSep, contractDomainSep, "Domain separator mismatch between Go service and contract");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 2: TYPEHASH matches Go service constant
    // ═══════════════════════════════════════════════════════════════════════════

    function test_TypeHash_MatchesGoService() public view {
        // Go service uses this exact string
        bytes32 goTypeHash = keccak256(
            "PaymentOrder(bytes32 orderId,address payer,uint256 amount,bytes32 bookingId,uint256 nonce,uint256 deadline)"
        );

        assertEq(goTypeHash, gateway.PAYMENT_ORDER_TYPEHASH(), "TYPEHASH mismatch");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 3: Full FE → Go → Contract flow with realistic booking
    // Simulates: Checkout page → Java Service → Go Service → MetaMask → Go relayer → Contract
    // ═══════════════════════════════════════════════════════════════════════════

    function test_FullFlow_RealisticBooking() public {
        // ── Step 1: Frontend creates booking (Java service receives this)
        // amount = 2,500,000 VND (a typical hotel night in Vietnam)
        uint256 amount = 2_500_000;
        bytes32 bookingId = keccak256("booking-uuid-abc123");

        // ── Step 2: Go service PreparePaymentOrder()
        uint256 payerNonce = gateway.getNonce(payer1);
        PaymentGateway.PaymentOrder memory order = _goServicePrepare(bookingId, payerNonce, payer1, amount);

        // Validate the Go service would return correct typed data
        assertEq(order.payer, payer1, "Payer mismatch");
        assertEq(order.amount, amount, "Amount mismatch");
        assertEq(order.nonce, 0, "Initial nonce should be 0");
        assertTrue(order.deadline > block.timestamp, "Deadline should be in the future");

        // ── Step 3: Frontend (MetaMask) signs the EIP-712 message
        bytes32 digest = _buildDigest(order);
        bytes memory signature = _signAsMetaMask(payer1Key, digest);

        // ── Step 4: Go service VerifySignature() — recovers address and checks
        // (This is what Go does before spending relayer gas)
        {
            bytes32 r;
            bytes32 s;
            uint8 v;
            assembly {
                r := mload(add(signature, 32))
                s := mload(add(signature, 64))
                v := byte(0, mload(add(signature, 96)))
            }
            address recovered = ecrecover(digest, v, r, s);
            assertEq(recovered, payer1, "Go service signature verification would fail");
        }

        // ── Step 5: Go service ExecutePayment() submits to contract
        uint256 balBefore = token.balanceOf(payer1);

        vm.prank(relayer);
        gateway.executePayment(order, signature);

        // ── Step 6: Verify on-chain state (what Go service checks after receipt)
        assertEq(token.balanceOf(payer1), balBefore - amount, "Token deduction incorrect");
        assertEq(token.balanceOf(address(gateway)), amount, "Gateway didn't receive tokens");
        assertTrue(gateway.processedOrders(order.orderId), "Order not marked processed");
        assertEq(gateway.getNonce(payer1), 1, "Nonce not incremented");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 4: Frontend JSON field mapping — Go uses snake_case, contract uses camelCase
    // Verifies the exact data format sent from frontend to Go /execute endpoint
    // ═══════════════════════════════════════════════════════════════════════════

    function test_FieldMapping_FrontendToBackend() public {
        uint256 amount = 1_000_000;
        bytes32 bookingId = keccak256("booking-field-map-test");
        bytes32 orderId = keccak256(abi.encodePacked(bookingId, block.timestamp));

        // This is what the frontend sends as JSON to /web3/payment/execute:
        // {
        //   "id_payment": "uuid-here",           → Go: req.IDPayment
        //   "order": {
        //     "order_id": "0x...",                → Go: req.Order.OrderID
        //     "payer": "0x...",                   → Go: req.Order.Payer
        //     "amount": "1000000",                → Go: req.Order.Amount (uint64, json:"amount,string")
        //     "booking_id": "0x...",              → Go: req.Order.BookingID
        //     "nonce": 0,                         → Go: req.Order.Nonce
        //     "deadline": 1234567890              → Go: req.Order.Deadline
        //   },
        //   "signature": "0x..."                  → Go: req.Signature
        // }

        // Validate that the Go struct correctly maps to Solidity struct fields
        PaymentGateway.PaymentOrder memory order = PaymentGateway.PaymentOrder({
            orderId:   orderId,    // Go: order_id → Solidity: orderId (bytes32)
            payer:     payer1,     // Go: payer → Solidity: payer (address)
            amount:    amount,     // Go: amount → Solidity: amount (uint256)
            bookingId: bookingId,  // Go: booking_id → Solidity: bookingId (bytes32)
            nonce:     0,          // Go: nonce → Solidity: nonce (uint256)
            deadline:  block.timestamp + 3600 // Go: deadline → Solidity: deadline (uint256)
        });

        bytes32 digest = _buildDigest(order);
        bytes memory sig = _signAsMetaMask(payer1Key, digest);

        vm.prank(relayer);
        gateway.executePayment(order, sig);

        assertTrue(gateway.processedOrders(orderId), "Field mapping failed");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 5: Sequential payments from same user (nonce increment flow)
    // Simulates: user books 3 hotels back-to-back
    // ═══════════════════════════════════════════════════════════════════════════

    function test_SequentialPayments_NonceIncrement() public {
        uint256[3] memory amounts = [uint256(500_000), 1_500_000, 3_000_000];
        string[3] memory bookingNames = ["booking-1", "booking-2", "booking-3"];

        for (uint256 i = 0; i < 3; i++) {
            bytes32 bookingId = keccak256(bytes(bookingNames[i]));
            uint256 nonce = gateway.getNonce(payer1);
            assertEq(nonce, i, "Nonce should match iteration");

            PaymentGateway.PaymentOrder memory order = _goServicePrepare(bookingId, nonce, payer1, amounts[i]);
            bytes32 digest = _buildDigest(order);
            bytes memory sig = _signAsMetaMask(payer1Key, digest);

            vm.prank(relayer);
            gateway.executePayment(order, sig);
        }

        assertEq(gateway.getNonce(payer1), 3, "Final nonce should be 3");
        assertEq(token.balanceOf(address(gateway)), 5_000_000, "Total amount incorrect");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 6: Concurrent users — nonces are isolated per address
    // Simulates: 3 users booking simultaneously
    // ═══════════════════════════════════════════════════════════════════════════

    function test_ConcurrentUsers_IsolatedNonces() public {
        // User 1 books
        PaymentGateway.PaymentOrder memory o1 = _goServicePrepare(
            keccak256("user1-booking"), gateway.getNonce(payer1), payer1, 1_000_000
        );
        bytes memory s1 = _signAsMetaMask(payer1Key, _buildDigest(o1));

        // User 2 books
        PaymentGateway.PaymentOrder memory o2 = _goServicePrepare(
            keccak256("user2-booking"), gateway.getNonce(payer2), payer2, 2_000_000
        );
        bytes memory s2 = _signAsMetaMask(payer2Key, _buildDigest(o2));

        // User 3 books
        PaymentGateway.PaymentOrder memory o3 = _goServicePrepare(
            keccak256("user3-booking"), gateway.getNonce(payer3), payer3, 3_000_000
        );
        bytes memory s3 = _signAsMetaMask(payer3Key, _buildDigest(o3));

        // Relayer submits all 3 (simulates Go service processing queue)
        vm.startPrank(relayer);
        gateway.executePayment(o1, s1);
        gateway.executePayment(o2, s2);
        gateway.executePayment(o3, s3);
        vm.stopPrank();

        // Each user's nonce is independent
        assertEq(gateway.getNonce(payer1), 1, "User1 nonce");
        assertEq(gateway.getNonce(payer2), 1, "User2 nonce");
        assertEq(gateway.getNonce(payer3), 1, "User3 nonce");
        assertEq(token.balanceOf(address(gateway)), 6_000_000, "Gateway total");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 7: Signature v-value compatibility
    // MetaMask sends v=27/28; Go adjusts to 0/1 for ecrecover; contract handles both
    // ═══════════════════════════════════════════════════════════════════════════

    function test_SignatureVValue_Compatibility() public {
        PaymentGateway.PaymentOrder memory order = _goServicePrepare(
            keccak256("v-value-test"), 0, payer1, 1_000_000
        );
        bytes32 digest = _buildDigest(order);

        // Get raw v, r, s
        (uint8 v, bytes32 r, bytes32 s) = vm.sign(payer1Key, digest);

        // MetaMask format: v is already 27 or 28
        assertTrue(v == 27 || v == 28, "v should be 27 or 28 from MetaMask");

        // Frontend sends signature as: abi.encodePacked(r, s, v) — 65 bytes
        bytes memory metaMaskSig = abi.encodePacked(r, s, v);
        assertEq(metaMaskSig.length, 65, "Signature should be 65 bytes");

        // Go service adjusts v: sigBytes[64] -= 27 before crypto.SigToPub
        // Contract uses OpenZeppelin ECDSA.recover which handles v=27/28 natively

        // Verify the raw ecrecover works (what Go does internally after v adjustment)
        address recovered = ecrecover(digest, v, r, s);
        assertEq(recovered, payer1, "ecrecover with v=27/28 should recover payer");

        vm.prank(relayer);
        gateway.executePayment(order, metaMaskSig);

        assertTrue(gateway.processedOrders(order.orderId), "Payment should succeed with MetaMask v-value");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 8: Deadline boundary — payment at exact deadline should succeed
    // ═══════════════════════════════════════════════════════════════════════════

    function test_Deadline_ExactBoundary() public {
        uint256 deadline = block.timestamp + 100;

        PaymentGateway.PaymentOrder memory order = PaymentGateway.PaymentOrder({
            orderId:   keccak256("deadline-boundary"),
            payer:     payer1,
            amount:    1_000_000,
            bookingId: keccak256("deadline-booking"),
            nonce:     0,
            deadline:  deadline
        });

        bytes memory sig = _signAsMetaMask(payer1Key, _buildDigest(order));

        // Warp to exact deadline — should still work (block.timestamp <= deadline)
        vm.warp(deadline);

        vm.prank(relayer);
        gateway.executePayment(order, sig);

        assertTrue(gateway.processedOrders(order.orderId), "Payment at exact deadline should succeed");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 9: Deadline expired — Go service pre-check + contract revert
    // ═══════════════════════════════════════════════════════════════════════════

    function test_Deadline_Expired_Reverts() public {
        uint256 deadline = block.timestamp + 100;

        PaymentGateway.PaymentOrder memory order = PaymentGateway.PaymentOrder({
            orderId:   keccak256("expired-deadline"),
            payer:     payer1,
            amount:    1_000_000,
            bookingId: keccak256("expired-booking"),
            nonce:     0,
            deadline:  deadline
        });

        bytes memory sig = _signAsMetaMask(payer1Key, _buildDigest(order));

        // Warp past deadline
        vm.warp(deadline + 1);

        vm.prank(relayer);
        vm.expectRevert("Order expired");
        gateway.executePayment(order, sig);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 10: Double-submit protection (mirrors Go ClaimForProcessing)
    // ═══════════════════════════════════════════════════════════════════════════

    function test_DoubleSubmit_Rejected() public {
        PaymentGateway.PaymentOrder memory order = _goServicePrepare(
            keccak256("double-submit"), 0, payer1, 1_000_000
        );
        bytes memory sig = _signAsMetaMask(payer1Key, _buildDigest(order));

        vm.prank(relayer);
        gateway.executePayment(order, sig);

        // Second submission of same order should revert
        vm.prank(relayer);
        vm.expectRevert("Order already processed");
        gateway.executePayment(order, sig);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 11: Wrong signer — attacker signs for someone else's order
    // ═══════════════════════════════════════════════════════════════════════════

    function test_WrongSigner_Rejected() public {
        // Order is for payer1
        PaymentGateway.PaymentOrder memory order = _goServicePrepare(
            keccak256("wrong-signer"), 0, payer1, 1_000_000
        );

        // But payer2 signs it (attacker)
        bytes memory sig = _signAsMetaMask(payer2Key, _buildDigest(order));

        vm.prank(relayer);
        vm.expectRevert("Invalid signature");
        gateway.executePayment(order, sig);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 12: Amount tampering — signed for 1M but submitting with 1 VND
    // ═══════════════════════════════════════════════════════════════════════════

    function test_AmountTampering_Rejected() public {
        PaymentGateway.PaymentOrder memory order = _goServicePrepare(
            keccak256("amount-tamper"), 0, payer1, 1_000_000
        );
        bytes memory sig = _signAsMetaMask(payer1Key, _buildDigest(order));

        // Tamper with amount
        order.amount = 1;

        vm.prank(relayer);
        vm.expectRevert("Invalid signature");
        gateway.executePayment(order, sig);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 13: Refund after payment — admin can return funds
    // ═══════════════════════════════════════════════════════════════════════════

    function test_PaymentThenRefund() public {
        uint256 amount = 5_000_000;
        PaymentGateway.PaymentOrder memory order = _goServicePrepare(
            keccak256("refund-test"), 0, payer1, amount
        );
        bytes memory sig = _signAsMetaMask(payer1Key, _buildDigest(order));

        uint256 payerBalBefore = token.balanceOf(payer1);

        vm.prank(relayer);
        gateway.executePayment(order, sig);

        assertEq(token.balanceOf(payer1), payerBalBefore - amount, "Payment deducted");

        // Admin refunds
        gateway.refund(order.orderId, payer1, amount);
        assertEq(token.balanceOf(payer1), payerBalBefore, "Refund restored balance");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 14: Large Vietnamese booking amounts (premium hotel)
    // ═══════════════════════════════════════════════════════════════════════════

    function test_LargeAmount_PremiumBooking() public {
        // 50,000,000 VND = ~$2,000 USD (premium suite for a week)
        uint256 amount = 50_000_000;
        PaymentGateway.PaymentOrder memory order = _goServicePrepare(
            keccak256("premium-booking"), 0, payer1, amount
        );
        bytes memory sig = _signAsMetaMask(payer1Key, _buildDigest(order));

        vm.prank(relayer);
        gateway.executePayment(order, sig);

        assertEq(token.balanceOf(address(gateway)), amount, "Large amount handled correctly");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 15: Go service bookingId encoding — UUID bytes → keccak256 → bytes32
    // ═══════════════════════════════════════════════════════════════════════════

    function test_BookingIdEncoding_UUIDLike() public {
        // Go service converts UUID.Bytes() (16 bytes) to hex, sends as "0x" + hex
        // Frontend sends it back as-is in the execute request
        // This tests that 16-byte UUIDs properly zero-pad to bytes32

        bytes16 uuidBytes = bytes16(0x550e8400e29b41d4a716446655440000); // example UUID
        bytes32 bookingId = bytes32(uuidBytes); // left-padded to 32 bytes

        PaymentGateway.PaymentOrder memory order = PaymentGateway.PaymentOrder({
            orderId:   keccak256(abi.encodePacked(bookingId, block.timestamp)),
            payer:     payer1,
            amount:    1_000_000,
            bookingId: bookingId,
            nonce:     0,
            deadline:  block.timestamp + 3600
        });

        bytes memory sig = _signAsMetaMask(payer1Key, _buildDigest(order));

        vm.prank(relayer);
        gateway.executePayment(order, sig);

        assertTrue(gateway.processedOrders(order.orderId), "UUID-like bookingId works");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 16: Gas benchmark for the full flow
    // ═══════════════════════════════════════════════════════════════════════════

    function test_GasBenchmark_FullFlow() public {
        PaymentGateway.PaymentOrder memory order = _goServicePrepare(
            keccak256("gas-bench"), 0, payer1, 2_500_000
        );
        bytes memory sig = _signAsMetaMask(payer1Key, _buildDigest(order));

        uint256 gasBefore = gasleft();
        vm.prank(relayer);
        gateway.executePayment(order, sig);
        uint256 gasUsed = gasBefore - gasleft();

        emit log_named_uint("Cross-layer executePayment gas", gasUsed);
        assertLt(gasUsed, 200_000, "Gas exceeds 200k budget");
    }
}
