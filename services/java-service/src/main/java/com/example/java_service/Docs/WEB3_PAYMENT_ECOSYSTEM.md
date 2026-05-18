# Blockchain/Web3 Payment Ecosystem - Booking System Documentation

> **Document Version:** 1.0  
> **Last Updated:** 2026-05-17  
> **Authors:** Research from codebase analysis

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Payment Flow](#2-payment-flow)
3. [Configuration](#3-configuration)
4. [Database Schema](#4-database-schema)
5. [API Endpoints](#5-api-endpoints)
6. [Smart Contracts](#6-smart-contracts)
7. [Services & Classes](#7-services--classes)
8. [Booking System Integration](#8-booking-system-integration)
9. [Deployment Setup](#9-deployment-setup)
10. [Security Considerations](#10-security-considerations)
11. [Supported Chains & Networks](#11-supported-chains--networks)
12. [EIP-712 Flow Detail](#12-eip-712-flow-detail)
13. [Testing](#13-testing)

---

## 1. Architecture Overview

### 1.1 System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              BOOKING SYSTEM                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐    ┌──────────────────┐    ┌─────────────────────────────┐ │
│  │   Frontend  │───▶│  Golang Service  │───▶│  Java Service (Spring Boot) │ │
│  │  (Android) │    │   Port: 8081/8083 │    │    Port: 8080/8082           │ │
│  └─────────────┘    └──────────────────┘    └─────────────────────────────┘ │
│         │                   │                         │                      │
│         │                   │                         │                      │
│         ▼                   ▼                         │                      │
│  ┌─────────────┐    ┌──────────────────┐             │                      │
│  │  MetaMask   │───▶│  Blockchain       │             │                      │
│  │  (Wallet)  │    │  (Sepolia Testnet)│             │                      │
│  └─────────────┘    └──────────────────┘             │                      │
│                              │                         │                      │
│                              ▼                         │                      │
│                    ┌──────────────────┐               │                      │
│                    │  PaymentGateway   │◀──────────────┘                      │
│                    │  (Smart Contract) │                                    │
│                    │  Sepolia: 0x0589 │                                    │
│                    └──────────────────┘                                    │
│                              │                                            │
│                              ▼                                            │
│                    ┌──────────────────┐                                    │
│                    │   VNDToken       │                                    │
│                    │  (ERC-20 Stable) │                                    │
│                    │  Sepolia: 0x3f8A │                                    │
│                    └──────────────────┘                                    │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Payment Methods

| Payment Method | Type | Gas Fee | User Experience |
|----------------|------|---------|-----------------|
| ZaloPay | Traditional | User pays via app | App-based payment |
| VND Stablecoin | Blockchain (EIP-712) | Relayer pays (gasless) | MetaMask wallet signature |

### 1.3 Technology Stack

| Component | Technology | Language |
|-----------|------------|----------|
| API Gateway | Gin Framework | Go |
| Business Logic | Spring Boot | Java |
| Smart Contracts | Solidity | Solidity |
| Blockchain Network | Ethereum Sepolia Testnet | - |
| Wallet Integration | EIP-712 (MetaMask compatible) | - |
| Deployment | Docker, Docker Compose | - |

---

## 2. Payment Flow

### 2.1 ZaloPay Payment Flow

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          ZaloPay Payment Flow                                │
└──────────────────────────────────────────────────────────────────────────────┘

  Customer                Frontend              Golang Service            ZaloPay Server
    │                        │                       │                          │
    │  1. Initiate Payment   │                       │                          │
    │──────────────────────▶│                       │                          │
    │                        │  2. Create Order      │                          │
    │                        │─────────────────────▶│                          │
    │                        │                       │  3. Create Order         │
    │                        │                       │─────────────────────────▶│
    │                        │                       │                          │
    │                        │                       │  4. ZaloPay QR URL       │
    │                        │                       │◀─────────────────────────│
    │                        │  5. Return QR URL    │                          │
    │  6. Show QR Code       │◀─────────────────────│                          │
    │◀───────────────────────│                       │                          │
    │                        │                       │                          │
    │  7. User pays via      │                       │                          │
    │     ZaloPay App        │                       │                          │
    │                        │                       │  8. Callback (POST)     │
    │                        │                       │◀─────────────────────────│
    │                        │                       │                          │
    │                        │                       │  9. Process Callback    │
    │                        │                       │  ─────────────────────  │
    │                        │                       │  • UpdateStatus()       │
    │                        │                       │  • CreateRevenue()      │
    │                        │                       │  • notifyJavaService()   │
    │                        │                       │                          │
    │                        │                       │                          │──────────────┐
    │                        │                       │                          │              ▼
    │                        │                       │                  ┌───────────────┐   ┌────────────────┐
    │                        │                       │                  │ Java Service  │──▶│ CompanyBalance │
    │                        │                       │                  │ /internal     │   │ Service        │
    │                        │                       │                  │ /payment-     │   │                │
    │                        │                       │                  │ success       │   │                │
    │                        │                       │                  └───────────────┘   └────────────────┘
```

### 2.2 VND Stablecoin (Web3) Payment Flow

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    VND Stablecoin (Web3) Payment Flow                       │
└──────────────────────────────────────────────────────────────────────────────┘

  Customer                Frontend              Golang Service           Smart Contract
    │                        │                       │                        │
    │  1. Initiate Payment   │                       │                        │
    │──────────────────────▶│                       │                        │
    │                        │  2. Prepare Payment   │                        │
    │                        │─────────────────────▶│                        │
    │                        │                       │  3. Get Nonce          │
    │                        │                       │───────────────────────▶│
    │                        │                       │  4. Return Nonce       │
    │                        │                       │◀───────────────────────│
    │                        │                       │                        │
    │                        │  5. Return EIP-712    │                        │
    │                        │     TypedData         │                        │
    │  6. Sign with MetaMask │◀──────────────────────│                        │
    │◀───────────────────────│                       │                        │
    │                        │                       │                        │
    │  7. Send Execute req   │                       │                        │
    │     (signed data)      │                       │                        │
    │───────────────────────▶│  8. Execute Payment   │                        │
    │                        │─────────────────────▶│                        │
    │                        │                       │  9. Verify Signature   │
    │                        │                       │  ────────────────────  │
    │                        │                       │  • buildEIP712Digest   │
    │                        │                       │  • recover signer      │
    │                        │                       │  • compare addresses   │
    │                        │                       │                        │
    │                        │                       │  10. Execute via       │
    │                        │                       │      Relayer           │
    │                        │                       │───────────────────────▶│
    │                        │                       │                        │
    │                        │                       │  11. Tx Submitted      │
    │                        │                       │     (wait 2 min)       │
    │                        │                       │                        │
    │                        │                       │  12. Update Payment    │
    │                        │                       │     Status (Success)   │
    │                        │                       │                        │
    │                        │                       │  13. Trigger Loyalty   │
    │                        │                       │     Points (goroutine) │
    │                        │                       │                        │
    │                        │                       │  14. notifyJavaService │
    │                        │                       │     Balance Update      │
    │                        │                       │◀──────────────────────│
    │                        │                       │                        │
    │  15. Payment Success   │                       │                        │
    │◀───────────────────────│                       │                        │
```

---

## 3. Configuration

### 3.1 Golang Service Environment Variables

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/.env`

```bash
# =============================================================================
# SERVER CONFIGURATION
# =============================================================================
PORT=8081

# =============================================================================
# POSTGRESQL DATABASE
# =============================================================================
DB_HOST=ep-patient-grass-a107mkcf-pooler.ap-southeast-1.aws.neon.tech
DB_PORT=5432
DB_USER=neondb_owner
DB_PASSWORD=npg_uMynB94hZUcH
DB_NAME=neondb
DB_SSLMODE=require
DB_CHANNELBINDING=require

# =============================================================================
# ZALOPAY CONFIGURATION
# =============================================================================
ZALOPAY_APP_ID=2554
ZALOPAY_KEY1=sdngKKJmqEMzvh5QQcdD2A9XBSKUNaYn
ZALOPAY_KEY2=trMrHtvjo6myautxDUiAcYsVtaeQ8nhf
ZALOPAY_CREATE_ORDER_ENDPOINT=https://sb-openapi.zalopay.vn/v2/create
APP_DOMAIN=https://oyster-app-bn6ec.ondigitalocean.app/golang-service

# =============================================================================
# BLOCKCHAIN / EIP-712 (VND STABLECOIN PAYMENT)
# =============================================================================
SEPOLIA_RPC_URL=https://ethereum-sepolia-rpc.publicnode.com
RELAYER_PRIVATE_KEY=0xcc639f4a21e2266c47dd95b24679e5e839197f50232d43dd71207aa44fb3243e
VND_TOKEN_ADDRESS=0x3f8A265E3e616E485343476419aC90736e8Af3c3
PAYMENT_GATEWAY_ADDRESS=0x05893F2f540B2A2524ff950a1c5e0276811EE92B

# =============================================================================
# SWAGGER DOCUMENTATION
# =============================================================================
SWAGGER_HOST=localhost:8081
SWAGGER_SCHEME=http
```

### 3.2 Blockchain Configuration (Go)

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/Configs/BlockchainConfig.go`

```go
package Configs

import (
    "github.com/joho/godotenv"
    "os"
    "log"
)

var (
    SepoliaRPCURL         string
    RelayerPrivateKey     string
    VNDTokenAddress       string
    PaymentGatewayAddress string
    ChainID               int64 = 11155111 // Sepolia Testnet
)

func BlockchainConfig() error {
    err := godotenv.Load()
    if err != nil {
        return err
    }

    SepoliaRPCURL = os.Getenv("SEPOLIA_RPC_URL")
    RelayerPrivateKey = os.Getenv("RELAYER_PRIVATE_KEY")
    VNDTokenAddress = os.Getenv("VND_TOKEN_ADDRESS")
    PaymentGatewayAddress = os.Getenv("PAYMENT_GATEWAY_ADDRESS")
    ChainID = 11155111 // Sepolia

    return nil
}
```

### 3.3 Foundry Configuration

**File:** `D:/Project_Android_Java/Booking-System/contracts/foundry.toml`

```toml
[profile.default]
src = "src"
out = "out"
libs = ["lib"]
solc = "0.8.28"
optimizer = true
optimizer_runs = 200
via_ir = true

[rpc_endpoints]
sepolia = "${SEPOLIA_RPC_URL}"

[etherscan]
sepolia = { key = "${ETHERSCAN_API_KEY}" }
```

### 3.4 Contracts Environment Example

**File:** `D:/Project_Android_Java/Booking-System/contracts/.env.example`

```bash
SEPOLIA_RPC_URL=
PRIVATE_KEY=
ETHERSCAN_API_KEY=
VND_TOKEN_ADDRESS=
PAYMENT_GATEWAY_ADDRESS=
```

---

## 4. Database Schema

### 4.1 Payment Table (Golang Service)

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/Internal/Models/Payment.go`

```sql
CREATE TABLE payment (
    id_payment UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id_user UUID NOT NULL,
    id_booking UUID,
    id_booking_tour UUID,
    id_booking_transport UUID,
    amount NUMERIC(12, 2) NOT NULL,
    method VARCHAR(20) NOT NULL,           -- 'ZaloPay' or 'VNDStablecoin'
    status VARCHAR(20) NOT NULL,          -- 'Pending', 'Processing', 'Success', 'Failed'
    id_app_trans VARCHAR(100),            -- Application transaction ID
    id_zp_trans VARCHAR(100),             -- ZaloPay transaction ID
    tx_hash VARCHAR(66),                  -- Blockchain transaction hash (for VNDStablecoin)
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Fields:**
- `tx_hash`: Stores blockchain transaction hash for VND Stablecoin payments (66 chars = 32 bytes + "0x" prefix)
- `method`: Payment method identifier (`ZaloPay` or `VNDStablecoin`)

### 4.2 Payment Table (Java Service)

**File:** `D:/Project_Android_Java/Booking-System/services/java-service/src/main/java/com/example/java_service/Models/Payment.java`

```java
@Entity
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_payment", nullable = false, updatable = false)
    private UUID idPayment;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "method", length = 20)
    private String method;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "id_app_trans", length = 100)
    private String idAppTrans;

    @Column(name = "id_zp_trans", length = 100)
    private String idZpTrans;

    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @CreationTimestamp
    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createAt;

    @ManyToOne
    @JoinColumn(name = "id_user", referencedColumnName = "id_user")
    private Users users;

    @ManyToOne
    @JoinColumn(name = "id_booking", referencedColumnName = "id_booking")
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "id_booking_tour", referencedColumnName = "id_booking_tour")
    private BookingTour bookingTour;

    @ManyToOne
    @JoinColumn(name = "id_booking_transport", referencedColumnName = "id_booking_transport")
    private BookingTransport bookingTransport;
}
```

### 4.3 Company Balance Table

**File:** `D:/Project_Android_Java/Booking-System/services/java-service/src/main/java/com/example/java_service/Services/CompanyBalanceService.java`

```
| Column           | Type           | Description                          |
|------------------|----------------|--------------------------------------|
| id_user          | UUID (PK)      | Doanh nghiệp (Business Owner)        |
| total_earned    | DECIMAL(14,2)  | Tổng tiền đã nhận                    |
| total_withdrawn | DECIMAL(14,2)  | Tổng tiền đã rút                     |
| locked_amount   | DECIMAL(14,2)  | Số tiền bị khóa                      |

Computed: available_balance = total_earned - total_withdrawn - locked_amount
```

### 4.4 Payment Status Constants

**Golang:** `D:/Project_Android_Java/Booking-System/services/golang-service/Constants/ConstantStatusPayment.go`

```go
const (
    StatusSuccess    = "Success"
    StatusFailed     = "Failed"
    StatusPending    = "Pending"
    StatusProcessing = "Processing"
)
```

**Java:** `D:/Project_Android_Java/Booking-System/services/java-service/src/main/java/com/example/java_service/Constant/PredefinedPaymentStatus.java`

```java
public class PredefinedPaymentStatus {
    public static final String UNPAID = "UNPAID";
    public static final String PAID = "PAID";
    public static final String REFUNDED = "REFUNDED";
}
```

**Java Payment Method:** `D:/Project_Android_Java/Booking-System/services/java-service/src/main/java/com/example/java_service/Constant/PredefinedMethodPayment.java`

```java
public class PredefinedMethodPayment {
    public static final String ZALOPAY_METHOD = "ZaloPay";
    public static final String VND_STABLE_COIN = "VNDStablecoin";
}
```

---

## 5. API Endpoints

### 5.1 ZaloPay Payment Endpoints

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/Routes/PaymentRouter.go`

```go
paymentRoutes := router.Group("/api/v1/payments")
{
    paymentRoutes.POST("/", paymentController.CreatePayment)
    paymentRoutes.POST("/callback", paymentController.HandleCallBack)
}
```

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/payments` | Create new ZaloPay payment order |
| POST | `/api/v1/payments/callback` | Handle ZaloPay payment callback |

### 5.2 Web3/VND Stablecoin Payment Endpoints

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/Routes/PaymentRouter.go`

```go
vndRoutes := router.Group("/web3/payment")
{
    vndRoutes.POST("/prepare", vndPaymentController.PrepareVndPayment)
    vndRoutes.POST("/execute", vndPaymentController.ExecuteVndPayment)
    vndRoutes.GET("/nonce/:address", vndPaymentController.GetNonce)
}
```

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/web3/payment/prepare` | Prepare EIP-712 typed data for signing |
| POST | `/web3/payment/execute` | Execute payment (verify signature + submit tx) |
| GET | `/web3/payment/nonce/:address` | Get wallet nonce from PaymentGateway |

### 5.3 Internal Java Endpoints

**File:** `D:/Project_Android_Java/Booking-System/services/java-service/src/main/java/com/example/java_service/Controllers/PaymentInternalController.java`

```java
@RestController
@RequestMapping("/internal")
public class PaymentInternalController {
    @PostMapping("/payment-success")
    public ResponseEntity<Void> handlePaymentSuccess(
            @RequestBody PaymentSuccessNotification notification) {
        companyBalanceService.addEarnings(notification);
        return ResponseEntity.ok().build();
    }
}
```

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/internal/payment-success` | Receive payment success notification from Golang service |

---

## 6. Smart Contracts

### 6.1 PaymentGateway Contract

**File:** `D:/Project_Android_Java/Booking-System/contracts/src/PaymentGateway.sol`

**Key Features:**
- Implements **EIP-712** for typed data signing
- Manages payment nonces to prevent replay attacks
- Tracks processed orders for idempotency
- Supports ERC-20 token transfers

**Contract Interface:**

```solidity
contract PaymentGateway is EIP712, Ownable {
    // Type hash for EIP-712 structured data
    bytes32 public constant PAYMENT_ORDER_TYPEHASH = keccak256(
        "PaymentOrder(bytes32 orderId,address payer,uint256 amount,bytes32 bookingId,uint256 nonce,uint256 deadline)"
    );

    // Immutable token reference
    VNDToken public immutable vndToken;

    // State mappings
    mapping(address => uint256) public nonces;                    // User nonce counter
    mapping(bytes32 => bool) public processedOrders;              // Idempotency tracking

    // Events
    event PaymentProcessed(bytes32 indexed orderId, address indexed payer, uint256 amount, bytes32 bookingId, uint256 timestamp);
    event Refunded(bytes32 indexed orderId, address indexed payer, uint256 amount);

    // Payment order struct
    struct PaymentOrder {
        bytes32 orderId;
        address payer;
        uint256 amount;
        bytes32 bookingId;
        uint256 nonce;
        uint256 deadline;
    }

    // Core function - executes payment with EIP-712 signature verification
    function executePayment(PaymentOrder calldata order, bytes calldata signature) external {
        require(order.amount > 0, "Amount must be positive");
        require(!processedOrders[order.orderId], "Order already processed");
        require(block.timestamp <= order.deadline, "Order expired");
        require(nonces[order.payer] == order.nonce, "Invalid nonce");

        // EIP-712 signature verification
        bytes32 structHash = keccak256(abi.encode(PAYMENT_ORDER_TYPEHASH, ...));
        bytes32 digest = _hashTypedDataV4(structHash);
        address signer = digest.recover(signature);
        require(signer == order.payer, "Invalid signature");

        // Mark as processed (CEI pattern)
        processedOrders[order.orderId] = true;
        nonces[order.payer]++;

        // Transfer tokens from payer to this contract
        bool success = vndToken.transferFrom(order.payer, address(this), order.amount);
        require(success, "Token transfer failed");

        emit PaymentProcessed(order.orderId, order.payer, order.amount, order.bookingId, block.timestamp);
    }

    // Admin functions
    function refund(bytes32 orderId, address payer, uint256 amount) external onlyOwner { ... }
    function withdraw(address to, uint256 amount) external onlyOwner { ... }
    function getNonce(address user) external view returns (uint256) { ... }
}
```

**Deployed Address (Sepolia):** `0x05893F2f540B2A2524ff950a1c5e0276811EE92B`

### 6.2 VNDToken Contract

**File:** `D:/Project_Android_Java/Booking-System/contracts/src/VNDToken.sol`

**Key Features:**
- ERC-20 compliant with **EIP-2612 Permit** extension for gasless approvals
- **0 decimals** (VND has no fractional units)
- Owner can mint tokens (for testing)

```solidity
contract VNDToken is ERC20, ERC20Permit, Ownable {
    // VND uses 0 decimals - no fractional units
    function decimals() public pure override returns (uint8) {
        return 0;
    }

    // Mint new tokens (owner only)
    function mint(address to, uint256 amount) external onlyOwner {
        _mint(to, amount);
    }
}
```

**Deployed Address (Sepolia):** `0x3f8A265E3e616E485343476419aC90736e8Af3c3`

### 6.3 Deployment Script

**File:** `D:/Project_Android_Java/Booking-System/contracts/script/Deploy.s.sol`

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import "forge-std/Script.sol";
import "forge-std/console.sol";
import "../src/PaymentGateway.sol";
import "../src/VNDToken.sol";

contract Deploy is Script {
    function run() external {
        uint256 deployerPrivateKey = vm.envUint("PRIVATE_KEY");
        address deployer = vm.addr(deployerPrivateKey);

        vm.startBroadcast(deployerPrivateKey);

        // 1. Deploy VNDToken first (PaymentGateway depends on it)
        VNDToken vndToken = new VNDToken(deployer);
        console.log("VNDToken deployed at:", address(vndToken));

        // 2. Deploy PaymentGateway with VNDToken address
        PaymentGateway gateway = new PaymentGateway(address(vndToken), deployer);
        console.log("PaymentGateway deployed at:", address(gateway));

        vm.stopBroadcast();
    }
}
```

**Deploy Command:**
```bash
forge script script/Deploy.s.sol --rpc-url sepolia --broadcast --verify
```

### 6.4 Contract ABI Definitions

**PaymentGateway ABI (Go):**

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/Internal/Services/BlockchainService.go` (Lines 42-72)

```json
[
  {
    "inputs": [
      {
        "components": [
          {"internalType": "bytes32", "name": "orderId", "type": "bytes32"},
          {"internalType": "address", "name": "payer", "type": "address"},
          {"internalType": "uint256", "name": "amount", "type": "uint256"},
          {"internalType": "bytes32", "name": "bookingId", "type": "bytes32"},
          {"internalType": "uint256", "name": "nonce", "type": "uint256"},
          {"internalType": "uint256", "name": "deadline", "type": "uint256"}
        ],
        "internalType": "struct PaymentGateway.PaymentOrder",
        "name": "order",
        "type": "tuple"
      },
      {"internalType": "bytes", "name": "signature", "type": "bytes"}
    ],
    "name": "executePayment",
    "outputs": [],
    "stateMutability": "nonpayable",
    "type": "function"
  },
  {
    "inputs": [{"internalType": "address", "name": "user", "type": "address"}],
    "name": "getNonce",
    "outputs": [{"internalType": "uint256", "name": "", "type": "uint256"}],
    "stateMutability": "view",
    "type": "function"
  }
]
```

**VNDToken ABI (Permit function):**

```json
[
  {
    "inputs": [
      {"internalType": "address", "name": "owner", "type": "address"},
      {"internalType": "address", "name": "spender", "type": "address"},
      {"internalType": "uint256", "name": "value", "type": "uint256"},
      {"internalType": "uint256", "name": "deadline", "type": "uint256"},
      {"internalType": "uint8", "name": "v", "type": "uint8"},
      {"internalType": "bytes32", "name": "r", "type": "bytes32"},
      {"internalType": "bytes32", "name": "s", "type": "bytes32"}
    ],
    "name": "permit",
    "outputs": [],
    "stateMutability": "nonpayable",
    "type": "function"
  }
]
```

---

## 7. Services & Classes

### 7.1 Blockchain Service (Golang)

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/Internal/Services/BlockchainService.go`

```go
type IBlockchainService interface {
    PreparePaymentOrder(idBooking uuid.UUID, payerAddress string, amount float64) (*Response.PrepareVndPaymentResponse, error)
    ExecutePayment(idPayment uuid.UUID, order Request.PaymentOrderData, signature string) (string, error)
    ExecutePermit(owner, spender string, value *big.Int, deadline int64, v uint8, r, s string) (string, error)
    VerifySignature(order Request.PaymentOrderData, signature string) (bool, error)
    GetNonce(payerAddress string) (uint64, error)
}

type blockchainService struct {
    _paymentRepository   repositories.IPaymentRepository
    _blockchainConfig     *configs.BlockchainConfig
    _vndTokenAddress     common.Address
    _paymentGateway      common.Address
    _authTransactor      *bind.TransactOpts
    _paymentGatewayABI   string
    _vndTokenABI         string
}
```

**Key Methods:**

| Method | Description |
|--------|-------------|
| `PreparePaymentOrder()` | Builds EIP-712 typed data for frontend signing |
| `ExecutePayment()` | Verifies signature and submits transaction via relayer |
| `ExecutePermit()` | Executes gasless ERC20 approval via relayer |
| `VerifySignature()` | Verifies EIP-712 signature locally |
| `GetNonce()` | Fetches on-chain nonce from PaymentGateway |

### 7.2 Payment Service (Golang)

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/Internal/Services/PaymentService.go`

```go
type IPaymentService interface {
    CreatePayment(paymentRequest Request.PaymentRequest) (*Response.PaymentResponse, error)
    ResolveCallBackZaloPay(zaloPayCallBackRequest Request.ZaloPayCallBackRequest) error
}
```

### 7.3 Payment Repository (Golang)

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/Internal/Repositories/PaymentRepository.go`

```go
type IPaymentRepository interface {
    Create(payment *Models.Payment) (*Models.Payment, error)
    FindByAppTransID(appTransID string) (*Models.Payment, error)
    FindByIDPayment(idPayment uuid.UUID) (*Models.Payment, error)
    UpdateStatus(idPayment uuid.UUID, zpTransId string, status string) error
    CreateVndPayment(...) (*Models.Payment, error)
    UpdateVndPaymentSuccess(idPayment uuid.UUID, txHash string) error
    ClaimForProcessing(idPayment uuid.UUID) (bool, error)
    UpdateVndPaymentFailed(idPayment uuid.UUID, txHash string) error
    GetDB() *gorm.DB
}
```

### 7.4 VND Payment Controller (Golang)

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/Internal/Controllers/VndPaymentController.go`

| Method | Handler | Description |
|--------|---------|-------------|
| `PrepareVndPayment()` | POST `/web3/payment/prepare` | Prepare EIP-712 data |
| `ExecuteVndPayment()` | POST `/web3/payment/execute` | Execute signed payment |
| `GetNonce()` | GET `/web3/payment/nonce/:address` | Get wallet nonce |

### 7.5 Company Balance Service (Java)

**File:** `D:/Project_Android_Java/Booking-System/services/java-service/src/main/java/com/example/java_service/Services/CompanyBalanceService.java`

```java
@Service
@RequiredArgsConstructor
public class CompanyBalanceService {
    public void addEarnings(PaymentSuccessNotification notification) { ... }
    public CompanyBalanceResponse getBalance(UUID idUser) { ... }
    public CompanyBankAccountResponse updateBankAccount(UUID idUser, CompanyBankAccountRequest request) { ... }
    public CompanyBankAccountResponse getBankAccount(UUID idUser) { ... }
    public WithdrawalRequestResponse createWithdrawalRequest(UUID idUser, WithdrawalRequestCreation request) { ... }
    public List<WithdrawalRequestResponse> getUserWithdrawalRequests(UUID idUser) { ... }
    public List<WithdrawalRequestResponse> getWithdrawalRequests(String status, int page, int size) { ... }
    public void approveWithdrawal(UUID withdrawalId, String transferReference) { ... }
    public void rejectWithdrawal(UUID withdrawalId, String adminNote) { ... }
}
```

---

## 8. Booking System Integration

### 8.1 Payment-to-Booking Connection

The Payment model links to three booking types, each with a different business owner lookup:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Business Owner Lookup Flow                            │
└─────────────────────────────────────────────────────────────────────────────┘

    Payment ─────────────────────────────────────────────────────────────────┐
         │                                                                  │
         ├───────────────────▶ id_booking ────▶ Booking ────▶ Accommodation │
         │                                          └───▶ id_user (owner)   │
         │                                                                  │
         ├───────────────────▶ id_booking_tour ──▶ BookingTour ────▶ Tour   │
         │                                            └───▶ id_user (owner) │
         │                                                                  │
         └───────────────────▶ id_booking_transport ──▶ BookingTransport    │
                                               └───▶ TransportTicket         │
                                                     └───▶ id_user (owner)  │
```

**Implementation:** `D:/Project_Android_Java/Booking-System/services/golang-service/Internal/Controllers/VndPaymentController.go` (Lines 348-372)

```go
func (vc *vndPaymentController) determineBusinessOwner(payment *Models.Payment) uuid.UUID {
    // Accommodation: booking -> accommodation -> id_user
    if payment.IDBooking != nil && payment.Booking != nil && payment.Booking.IDAccommodation != uuid.Nil {
        var accommodation Models.Acccommodation
        if err := vc._paymentRepository.GetDB().Where("id_accommodation = ?", payment.Booking.IDAccommodation).First(&accommodation).Error; err == nil {
            return accommodation.IDUser
        }
    }

    // Tour: booking_tour -> tour -> id_user
    if payment.IDBookingTour != nil && payment.BookingTour != nil {
        var tour Models.Tour
        if err := vc._paymentRepository.GetDB().Where("id_tour = ?", payment.BookingTour.IDTour).First(&tour).Error; err == nil {
            return tour.IDUser
        }
    }

    // Transport: booking_transport -> transport_ticket -> id_user
    if payment.IDBookingTransport != nil && payment.BookingTransport != nil {
        var ticket Models.TransportTicket
        if err := vc._paymentRepository.GetDB().Where("id_ticket = ?", payment.BookingTransport.IDTicket).First(&ticket).Error; err == nil {
            return ticket.IDUser
        }
    }

    return uuid.Nil
}
```

### 8.2 Payment Notification Flow

After successful payment, Golang service notifies Java service:

```
Golang Service                          Java Service
      │                                     │
      │  POST /internal/payment-success      │
      │  {                                   │
      │    idPayment: "uuid",                │
      │    idUser: "business-owner-uuid",    │
      │    amount: 1000000,                  │
      │    method: "VNDStablecoin"           │
      │  }                                   │
      │─────────────────────────────────────▶│
      │                                     │
      │                        CompanyBalanceService.addEarnings()
      │                                     │
      │                        total_earned += amount
      │                                     │
```

---

## 9. Deployment Setup

### 9.1 Golang Service Dockerfile

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/Dockerfile`

```dockerfile
# =============================================================================
# BUILD STAGE
# =============================================================================
FROM golang:1.24-alpine AS builder
ENV CGO_ENABLED=0 GOOS=linux GOARCH=amd64
WORKDIR /src

# Install build dependencies
RUN apk add --no-cache git ca-certificates

# Copy and download dependencies
COPY go.mod go.sum ./
RUN go mod download

# Copy source code and build
COPY . .
RUN go build -trimpath -ldflags="-s -w" -o /src/server .

# =============================================================================
# RUNTIME STAGE
# =============================================================================
FROM alpine:3.18

# Install runtime dependencies
RUN apk add --no-cache ca-certificates tzdata && update-ca-certificates

# Create non-root user
RUN addgroup -S app && adduser -S -G app app

# Setup working directory
WORKDIR /app
COPY --from=builder /src/server /app/server
RUN chown -R app:app /app
USER app

# Expose port
EXPOSE 8080

# Environment
ENV PORT=8080
ENV GIN_MODE=release

# Start server
CMD ["/app/server"]
```

### 9.2 Docker Compose

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/docker-compose.yml`

```yaml
version: '3.8'

services:
  api-service:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: golang-service
    ports:
      - "8083:8081"  # Host:Container
    env_file:
      - .env
    restart: unless-stopped
    deploy:
      resources:
        limits:
          cpus: '0.5'
          memory: 128M
```

### 9.3 Main Entry Point

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/main.go`

```go
func main() {
    // Initialize database
    db := Configs.ConnectToDB()

    // Initialize payment configurations (warnings logged if missing)
    if err := Configs.ZaloPayConfig(); err != nil {
        log.Printf("Warning: ZaloPay config failed (%v) — ZaloPay payment unavailable", err)
    }

    if err := Configs.BlockchainConfig(); err != nil {
        log.Printf("Warning: Blockchain config failed (%v) — VND Stablecoin payment unavailable", err)
    }

    // Setup Gin router
    r := gin.Default()

    // Register routes
    Routes.SwaggerRouter(r)
    Routes.PaymentRouter(r, db)
    Routes.ReportRevenueRouter(r, db)
    Routes.ReportRevenueDetailRouter(r, db)
    Routes.ReportRoomBookingRouter(r, db)

    // Start server
    port := os.Getenv("PORT")
    if port == "" {
        port = "8081"
    }
    r.Run(fmt.Sprintf(":%s", port))
}
```

---

## 10. Security Considerations

### 10.1 Private Key Management

**Location:** `D:/Project_Android_Java/Booking-System/services/golang-service/.env` (Line 24)

```bash
RELAYER_PRIVATE_KEY=0xcc639f4a21e2266c47dd95b24679e5e839197f50232d43dd71207aa44fb3243e
```

**Security Measures:**
- Stored as environment variable (not hardcoded in source)
- Prefixed with `0x` and stripped before use in code
- Relayer pattern: Users sign off-chain, relayer submits transactions on-chain

**Key Usage Pattern:**
```go
privKeyHex := strings.TrimPrefix(Configs.RelayerPrivateKey, "0x")
privateKey, err := crypto.HexToECDSA(privKeyHex)
```

### 10.2 Transaction Signing (EIP-155)

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/Internal/Services/BlockchainService.go`

```go
// EIP-155 signer provides replay attack protection
chainID := big.NewInt(Configs.ChainID) // 11155111 (Sepolia)
tx := types.NewTransaction(nonce, gatewayAddr, big.NewInt(0), gasLimit, gasPrice, data)
signer := types.NewEIP155Signer(chainID)
signedTx, err := types.SignTx(tx, signer, privateKey)
```

**Security Features:**
- **EIP-155 signer** prevents cross-chain replay attacks by including Chain ID
- **Chain ID validation** (Sepolia: 11155111)

### 10.3 Signature Verification

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/Internal/Services/BlockchainService.go` (Lines 189-214)

```go
func (b *blockchainService) VerifySignature(order Request.PaymentOrderData, signature string) (bool, error) {
    digest, err := buildEIP712Digest(order)
    sigBytes, err := hexToBytes(signature)

    // Adjust v value for MetaMask compatibility
    // MetaMask sends 27/28, but crypto.SigToPub expects 0/1
    if len(sigBytes) == 65 && (sigBytes[64] == 27 || sigBytes[64] == 28) {
        sigBytes[64] -= 27
    }

    pubKey, err := crypto.SigToPub(digest, sigBytes)
    recoveredAddr := crypto.PubkeyToAddress(*pubKey)
    return strings.EqualFold(recoveredAddr.Hex(), expectedAddr.Hex()), nil
}
```

**Security Measures:**
- EIP-712 digest construction ensures signature is order-specific
- `v` adjustment handles MetaMask format (27/28 vs 0/1)
- Case-insensitive address comparison prevents timing attacks

### 10.4 On-Chain Security (Smart Contract)

**File:** `D:/Project_Android_Java/Booking-System/contracts/src/PaymentGateway.sol`

```solidity
function executePayment(PaymentOrder calldata order, bytes calldata signature) external {
    // === CHECKS ===
    require(order.amount > 0, "Amount must be positive");           // Zero amount check
    require(!processedOrders[order.orderId], "Order already processed"); // Idempotency
    require(block.timestamp <= order.deadline, "Order expired");    // Deadline check
    require(nonces[order.payer] == order.nonce, "Invalid nonce");   // Nonce check

    // === SIGNATURE VERIFICATION ===
    bytes32 structHash = keccak256(abi.encode(PAYMENT_ORDER_TYPEHASH, ...));
    bytes32 digest = _hashTypedDataV4(structHash);
    address signer = digest.recover(signature);
    require(signer == order.payer, "Invalid signature");             // Signer check

    // === EFFECTS (CEI Pattern) ===
    processedOrders[order.orderId] = true;                          // Mark first
    nonces[order.payer]++;                                           // Increment nonce

    // === INTERACTIONS ===
    bool success = vndToken.transferFrom(order.payer, address(this), order.amount);
    require(success, "Token transfer failed");
}
```

**Security Features:**
| Protection | Mechanism |
|------------|-----------|
| Zero amount attack | `require(order.amount > 0)` |
| Double-spending | `processedOrders[order.orderId]` mapping |
| Stale order execution | `block.timestamp <= order.deadline` |
| Signature replay | `nonces[order.payer]` incremented after use |
| Wrong signer | EIP-712 `require(signer == order.payer)` |
| Reentrancy | CEI pattern (Checks-Effects-Interactions) |

### 10.5 ZaloPay HMAC Verification

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/Internal/Services/PaymentService.go` (Lines 141-145)

```go
func (p *paymentService) ResolveCallBackZaloPay(zaloPayCallBackRequest Request.ZaloPayCallBackRequest) error {
    mac := computeHmacSha256(zaloPayCallBackRequest.Data, Configs.ZaloPayKey2)
    if mac != zaloPayCallBackRequest.Mac {
        return fmt.Errorf("Chữ ký không hợp lệ!")
    }
    // ...
}
```

**Security:** HMAC-SHA256 signature verification ensures callback authenticity

---

## 11. Supported Chains & Networks

### 11.1 Current Configuration

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/Configs/BlockchainConfig.go` (Line 13)

| Setting | Value |
|---------|-------|
| Chain Name | Ethereum Sepolia Testnet |
| Chain ID | 11155111 |
| RPC URL | https://ethereum-sepolia-rpc.publicnode.com |
| Network Type | Testnet (no real value) |

### 11.2 RPC Endpoint

**Provider:** Public Ethereum Sepolia RPC (publicnode.com)

**Note:** For production, consider using a dedicated RPC provider (Infura, Alchemy, QuickNode) for:
- Higher rate limits
- Reliability guarantees
- Priority support
- Analytics

### 11.3 EIP-712 Domain Configuration

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/Internal/Services/BlockchainService.go` (Lines 36-39)

```go
const (
    domainName    = "BookingPaymentGateway"
    domainVersion = "1"
)
```

**Domain Parameters:**
| Parameter | Value |
|-----------|-------|
| Name | `BookingPaymentGateway` |
| Version | `1` |
| Chain ID | `11155111` (Sepolia) |
| Verifying Contract | `0x05893F2f540B2A2524ff950a1c5e0276811EE92B` |

---

## 12. EIP-712 Flow Detail

### 12.1 EIP-712 Typed Data Structure

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/Internal/Services/BlockchainService.go` (Lines 146-178)

```go
typedData := Response.EIP712TypedData{
    Domain: Response.EIP712Domain{
        Name:              domainName,           // "BookingPaymentGateway"
        Version:           domainVersion,        // "1"
        ChainID:           Configs.ChainID,       // 11155111
        VerifyingContract: Configs.PaymentGatewayAddress, // "0x05893F2..."
    },
    Types: map[string][]Response.TypeField{
        "EIP712Domain": {
            {Name: "name", Type: "string"},
            {Name: "version", Type: "string"},
            {Name: "chainId", Type: "uint256"},
            {Name: "verifyingContract", Type: "address"},
        },
        "PaymentOrder": {
            {Name: "orderId", Type: "bytes32"},
            {Name: "payer", Type: "address"},
            {Name: "amount", Type: "uint256"},
            {Name: "bookingId", Type: "bytes32"},
            {Name: "nonce", Type: "uint256"},
            {Name: "deadline", Type: "uint256"},
        },
    },
    PrimaryType: "PaymentOrder",
    Message: map[string]interface{}{
        "orderId":   orderID,
        "payer":     payerAddress,
        "amount":    amount,
        "bookingId": bookingID,
        "nonce":     nonce,
        "deadline":  deadline,
    },
}
```

### 12.2 Step-by-Step Flow

#### Step 1: Prepare Payment Order
```
Frontend                          Backend                           Blockchain
   │                                 │                                  │
   │  POST /web3/payment/prepare     │                                  │
   │  {                              │                                  │
   │    bookingId: "uuid",           │                                  │
   │    payerAddress: "0x...",       │                                  │
   │    amount: 1000000              │                                  │
   │  }                              │                                  │
   │────────────────────────────────▶│                                  │
   │                                 │                                  │
   │                                 │  Generate orderId                │
   │                                 │  orderId = keccak256(bookingId + timestamp)
   │                                 │                                  │
   │                                 │  Get Nonce                       │
   │                                 │  GetNonce(payerAddress)           │
   │                                 │──────────────────────────────────▶│
   │                                 │  Return nonce                    │
   │                                 │◀──────────────────────────────────│
   │                                 │                                  │
   │                                 │  Build EIP-712 TypedData          │
   │                                 │  Set deadline = now + 1 hour      │
   │                                 │                                  │
   │  Response:                      │                                  │
   │  {                              │                                  │
   │    orderId: "0x...",           │                                  │
   │    typedData: { ... },          │                                  │
   │    message: { ... }            │                                  │
   │  }                              │                                  │
   │◀────────────────────────────────│                                  │
```

#### Step 2: User Signs with MetaMask
```
Frontend                          User (MetaMask)
   │                                 │
   │  Display payment details        │
   │  {                              │
   │    amount: 1,000,000 VND        │
   │    merchant: "Booking System"  │
   │    orderId: "0x..."            │
   │  }                              │
   │                                 │
   │  user clicks "Sign"            │
   │────────────────────────────────▶│
   │                                 │
   │  MetaMask popup appears        │
   │  with EIP-712 signing request  │
   │                                 │
   │  User approves                 │
   │◀────────────────────────────────│
   │                                 │
   │  Return signature              │
   │  "0x[signed-data]"             │
   │◀────────────────────────────────│
```

#### Step 3: Execute Payment
```
Frontend                          Backend                           Blockchain
   │                                 │                                  │
   │  POST /web3/payment/execute     │                                  │
   │  {                              │                                  │
   │    orderId: "0x...",           │                                  │
   │    payerAddress: "0x...",      │                                  │
   │    amount: 1000000,            │                                  │
   │    bookingId: "uuid",          │                                  │
   │    nonce: 5,                   │                                  │
   │    deadline: 1234567890,        │                                  │
   │    signature: "0x..."          │                                  │
   │  }                              │                                  │
   │────────────────────────────────▶│                                  │
   │                                 │                                  │
   │                                 │  Verify Signature (off-chain)    │
   │                                 │  ──────────────────────────────── │
   │                                 │  • buildEIP712Digest()           │
   │                                 │  • recover(digest, signature)    │
   │                                 │  • compare(signer, payer)        │
   │                                 │                                  │
   │                                 │  if invalid: return error        │
   │                                 │                                  │
   │                                 │  Submit Transaction via Relayer   │
   │                                 │  ──────────────────────────────── │
   │                                 │  • Build transaction             │
   │                                 │  • Sign with relayer private key │
   │                                 │  • Send to Sepolia              │
   │                                 │──────────────────────────────────▶│
   │                                 │                                  │
   │                                 │  Wait for confirmation           │
   │                                 │  (timeout: 2 minutes)          │
   │                                 │                                  │
   │                                 │  Update Payment Status          │
   │                                 │  ─────────────────────────────── │
   │                                 │  • UpdateVndPaymentSuccess()     │
   │                                 │  • Save tx_hash                  │
   │                                 │                                  │
   │                                 │  Trigger async tasks             │
   │                                 │  ─────────────────────────────── │
   │                                 │  • addLoyaltyPoints()            │
   │                                 │  • notifyJavaService()           │
   │                                 │                                  │
   │  Response: { success: true }   │                                  │
   │◀────────────────────────────────│                                  │
```

### 12.3 Nonce Management

**Purpose:** Prevents signature replay attacks

```
User Address: 0x1234...
Nonce: 0 (on-chain)

When user signs order with nonce=0:
1. Contract verifies: nonces[user] == 0 ✓
2. Contract increments: nonces[user] = 1
3. Order marked as processed

If attacker tries to replay same signature:
1. Contract verifies: nonces[user] == 1 (not 0) ✗
2. Transaction REJECTED - "Invalid nonce"
```

---

## 13. Testing

### 13.1 Golang EIP-712 Test Suite

**File:** `D:/Project_Android_Java/Booking-System/services/golang-service/Internal/Services/blockchain_eip712_test.go`

| Test | Line | Description |
|------|------|-------------|
| `TestEIP712Digest_Deterministic` | 78 | Verifies same input always produces same digest |
| `TestEIP712Digest_FieldSensitivity` | 100 | Verifies changing any field changes digest |
| `TestVerifySignature_ValidKey` | 136 | Round-trip signature verification |
| `TestVerifySignature_WrongKey` | 162 | Rejects signatures from wrong key |
| `TestVerifySignature_ModifiedOrder` | 188 | Detects tampered order data |
| `TestVerifySignature_InvalidSigHex` | 214 | Handles malformed signatures |
| `TestEIP712DomainSeparator_ChainIDSensitivity` | 231 | Cross-chain replay prevention |
| `TestEIP712DomainSeparator_ContractSensitivity` | 257 | Contract-specific verification |
| `TestPadUint256` | 276 | ABI encoding correctness |
| `TestSimulateCompletePaymentFlow` | 306 | Full payment flow simulation |
| `TestSimulateMultiplePayments` | 380 | Nonce increment verification |

### 13.2 Solidity Tests

**File:** `D:/Project_Android_Java/Booking-System/contracts/test/PaymentGateway.t.sol`

---

## Appendix: File Reference

| File Path | Purpose |
|-----------|---------|
| `services/golang-service/.env` | Environment variables (secrets) |
| `services/golang-service/Configs/BlockchainConfig.go` | Blockchain config loader |
| `services/golang-service/Configs/ZaloPayConfig.go` | ZaloPay config loader |
| `services/golang-service/Internal/Services/BlockchainService.go` | Web3 payment core logic |
| `services/golang-service/Internal/Services/PaymentService.go` | Payment business logic |
| `services/golang-service/Internal/Repositories/PaymentRepository.go` | Payment data access |
| `services/golang-service/Internal/Controllers/VndPaymentController.go` | Web3 payment REST endpoints |
| `services/golang-service/Routes/PaymentRouter.go` | Route definitions |
| `services/golang-service/Internal/Services/blockchain_eip712_test.go` | EIP-712 unit tests |
| `services/java-service/.../Services/CompanyBalanceService.java` | Company balance management |
| `services/java-service/.../Models/Payment.java` | Payment entity (Java) |
| `services/java-service/.../Constant/PredefinedMethodPayment.java` | Payment method constants |
| `services/java-service/.../Constant/PredefinedPaymentStatus.java` | Payment status constants |
| `contracts/src/PaymentGateway.sol` | Payment smart contract |
| `contracts/src/VNDToken.sol` | VND stablecoin token |
| `contracts/script/Deploy.s.sol` | Deployment script |
| `contracts/foundry.toml` | Foundry configuration |
| `contracts/.env.example` | Contract deployment template |
| `services/golang-service/Dockerfile` | Docker build file |
| `services/golang-service/docker-compose.yml` | Docker compose config |
| `services/golang-service/main.go` | Application entry point |
| `services/golang-service/BUSINESS_FLOW_BALANCE.md` | Business flow documentation |

---

## Deployment Checklist

### Pre-Deployment

- [ ] Update `SEPOLIA_RPC_URL` if using private RPC provider
- [ ] Generate new `RELAYER_PRIVATE_KEY` for production
- [ ] Deploy contracts to production network (Mainnet/Ethereum)
- [ ] Update `VND_TOKEN_ADDRESS` and `PAYMENT_GATEWAY_ADDRESS`
- [ ] Update `ChainID` for production network

### Security Checklist

- [ ] Remove test private keys from `.env`
- [ ] Use secrets management (Vault, AWS Secrets Manager)
- [ ] Enable HTTPS on all endpoints
- [ ] Implement rate limiting on payment endpoints
- [ ] Add monitoring for failed transactions
- [ ] Setup alerts for unusual payment patterns

---

*Document generated from codebase analysis on 2026-05-17*