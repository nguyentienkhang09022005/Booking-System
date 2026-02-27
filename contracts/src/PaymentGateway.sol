// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import "@openzeppelin/contracts/utils/cryptography/EIP712.sol";
import "@openzeppelin/contracts/utils/cryptography/ECDSA.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "./VNDToken.sol";

/// @title PaymentGateway — EIP-712 structured-data payment processor for BookingNext
/// @notice The Go relayer calls executePayment() on behalf of users (no gas for users).
///         Users sign a PaymentOrder message off-chain; the relayer submits the tx.
contract PaymentGateway is EIP712, Ownable {
    using ECDSA for bytes32;

    // ─── Type hash ────────────────────────────────────────────────────────────
    bytes32 public constant PAYMENT_ORDER_TYPEHASH = keccak256(
        "PaymentOrder(bytes32 orderId,address payer,uint256 amount,bytes32 bookingId,uint256 nonce,uint256 deadline)"
    );

    // ─── State ────────────────────────────────────────────────────────────────
    VNDToken public immutable vndToken;

    /// @notice Per-user nonce prevents signature replay attacks.
    mapping(address => uint256) public nonces;

    /// @notice Tracks processed orderIds for idempotency.
    mapping(bytes32 => bool) public processedOrders;

    // ─── Events ───────────────────────────────────────────────────────────────
    event PaymentProcessed(
        bytes32 indexed orderId,
        address indexed payer,
        uint256 amount,
        bytes32 bookingId,
        uint256 timestamp
    );

    event Refunded(
        bytes32 indexed orderId,
        address indexed payer,
        uint256 amount
    );

    // ─── Structs ──────────────────────────────────────────────────────────────
    struct PaymentOrder {
        bytes32 orderId;
        address payer;
        uint256 amount;
        bytes32 bookingId;
        uint256 nonce;
        uint256 deadline;
    }

    // ─── Constructor ──────────────────────────────────────────────────────────
    constructor(address _vndToken, address initialOwner)
        EIP712("BookingPaymentGateway", "1")
        Ownable(initialOwner)
    {
        vndToken = VNDToken(_vndToken);
    }

    // ─── Core payment function ────────────────────────────────────────────────

    /// @notice Execute a signed payment order.
    ///         Called by the Go relayer on behalf of the user.
    /// @param order     Structured payment data signed by the user
    /// @param signature EIP-712 signature from order.payer
    function executePayment(PaymentOrder calldata order, bytes calldata signature) external {
        require(!processedOrders[order.orderId], "Order already processed");
        require(block.timestamp <= order.deadline, "Order expired");
        require(nonces[order.payer] == order.nonce, "Invalid nonce");

        // Build and verify EIP-712 digest
        bytes32 structHash = keccak256(abi.encode(
            PAYMENT_ORDER_TYPEHASH,
            order.orderId,
            order.payer,
            order.amount,
            order.bookingId,
            order.nonce,
            order.deadline
        ));

        bytes32 digest = _hashTypedDataV4(structHash);
        address signer = digest.recover(signature);
        require(signer == order.payer, "Invalid signature");

        // Mark order processed and increment nonce before external calls
        processedOrders[order.orderId] = true;
        nonces[order.payer]++;

        // Transfer tokens from payer to this contract
        bool success = vndToken.transferFrom(order.payer, address(this), order.amount);
        require(success, "Token transfer failed");

        emit PaymentProcessed(order.orderId, order.payer, order.amount, order.bookingId, block.timestamp);
    }

    // ─── Admin functions ──────────────────────────────────────────────────────

    /// @notice Refund a payment to a payer. Only callable by owner (admin).
    /// @param orderId Order identifier for traceability
    /// @param payer   Recipient of the refund
    /// @param amount  Amount to refund in VNDS
    function refund(bytes32 orderId, address payer, uint256 amount) external onlyOwner {
        require(payer != address(0), "Invalid payer address");
        bool success = vndToken.transfer(payer, amount);
        require(success, "Refund transfer failed");
        emit Refunded(orderId, payer, amount);
    }

    /// @notice Withdraw VNDS from the gateway. Only callable by owner.
    /// @param to     Recipient address
    /// @param amount Amount to withdraw in VNDS
    function withdraw(address to, uint256 amount) external onlyOwner {
        require(to != address(0), "Invalid recipient address");
        bool success = vndToken.transfer(to, amount);
        require(success, "Withdraw transfer failed");
    }

    /// @notice Get the current nonce for a user address.
    function getNonce(address user) external view returns (uint256) {
        return nonces[user];
    }

    /// @notice Get the EIP-712 domain separator (for frontend verification).
    function domainSeparator() external view returns (bytes32) {
        return _domainSeparatorV4();
    }
}
