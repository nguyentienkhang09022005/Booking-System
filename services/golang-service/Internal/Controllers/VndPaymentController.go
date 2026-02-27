package Controllers

import (
	"fmt"
	"net/http"
	"time"

	"github.com/ethereum/go-ethereum/common"
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"

	"payment-service/Constants"
	"payment-service/Internal/Dtos/Request"
	"payment-service/Internal/Dtos/Response"
	"payment-service/Internal/Repositories"
	"payment-service/Internal/Services"
)

type vndPaymentController struct {
	_blockchainService          Services.IBlockchainService
	_paymentRepository          Repositories.IPaymentRepository
	_reportRevenueDetailService Services.IReportRevenueDetailService
}

func VndPaymentController(
	blockchainService Services.IBlockchainService,
	paymentRepository Repositories.IPaymentRepository,
	reportRevenueDetailService Services.IReportRevenueDetailService,
) *vndPaymentController {
	return &vndPaymentController{
		_blockchainService:          blockchainService,
		_paymentRepository:          paymentRepository,
		_reportRevenueDetailService: reportRevenueDetailService,
	}
}

// PrepareVndPayment prepares EIP-712 typed data for the user to sign.
// POST /api/v1/payments/vnd/prepare
func (vc *vndPaymentController) PrepareVndPayment(c *gin.Context) {
	var req Request.PrepareVndPaymentRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	result, err := vc._blockchainService.PreparePaymentOrder(req.IDBooking, req.PayerAddress, req.Amount)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": fmt.Sprintf("Failed to prepare payment order: %v", err)})
		return
	}

	payment, err := vc._paymentRepository.CreateVndPayment(
		result.IDPayment,
		req.IDUser,
		req.IDBooking,
		req.Amount,
		result.OrderID,
	)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": fmt.Sprintf("Failed to create payment record: %v", err)})
		return
	}

	resp := &Response.PrepareVndPaymentResponse{
		IDPayment: payment.IDPayment,
		TypedData: result.TypedData,
		OrderID:   result.OrderID,
	}

	c.JSON(http.StatusCreated, resp)
}

// ExecuteVndPayment verifies the signature and submits the on-chain transaction.
// POST /api/v1/payments/vnd/execute
func (vc *vndPaymentController) ExecuteVndPayment(c *gin.Context) {
	var req Request.ExecuteVndPaymentRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	idPayment, err := uuid.Parse(req.IDPayment)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid id_payment format"})
		return
	}

	payment, err := vc._paymentRepository.FindByIDPayment(idPayment)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Payment record not found"})
		return
	}

	if payment.Method != "VNDStablecoin" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Payment method mismatch"})
		return
	}

	// FIX 2: Atomic claim — prevents double-submit race condition
	claimed, err := vc._paymentRepository.ClaimForProcessing(idPayment)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to claim payment"})
		return
	}
	if !claimed {
		c.JSON(http.StatusConflict, gin.H{"error": "Payment is not in Pending state or already being processed"})
		return
	}

	// Fail-fast: verify signature locally before spending relayer gas
	valid, err := vc._blockchainService.VerifySignature(req.Order, req.Signature)
	if err != nil || !valid {
		errMsg := "Invalid signature"
		if err != nil {
			errMsg = fmt.Sprintf("Signature verification failed: %v", err)
		}
		_ = vc._paymentRepository.UpdateVndPaymentFailed(idPayment, "")
		c.JSON(http.StatusBadRequest, gin.H{"error": errMsg})
		return
	}

	// FIX 3: Pre-check deadline before spending relayer gas
	if time.Now().Unix() > req.Order.Deadline {
		_ = vc._paymentRepository.UpdateVndPaymentFailed(idPayment, "")
		c.JSON(http.StatusBadRequest, gin.H{"error": "Payment order has expired"})
		return
	}

	// Submit transaction to Sepolia via relayer
	txHash, err := vc._blockchainService.ExecutePayment(idPayment, req.Order, req.Signature)
	if err != nil {
		_ = vc._paymentRepository.UpdateVndPaymentFailed(idPayment, txHash)
		errResp := gin.H{"error": fmt.Sprintf("Transaction failed: %v", err)}
		if txHash != "" {
			errResp["tx_hash"] = txHash
		}
		c.JSON(http.StatusInternalServerError, errResp)
		return
	}

	// Update Payment status to Success with tx hash
	if err := vc._paymentRepository.UpdateVndPaymentSuccess(idPayment, txHash); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to update payment status"})
		return
	}

	// Trigger revenue creation (mirrors ZaloPay callback flow)
	if payment.Booking.IDHotel != uuid.Nil {
		now := time.Now()
		revenueEvent := Request.RevenueCreatedEvent{
			IDBooking: &payment.IDBooking,
			IDExpense: nil,
			IDHotel:   payment.Booking.IDHotel,
			Amount:    payment.Amount,
			Month:     int(now.Month()),
			Year:      now.Year(),
		}
		if _, err := vc._reportRevenueDetailService.CreateReportRevenueDetail(revenueEvent); err != nil {
			fmt.Printf("Revenue creation failed for payment %s: %v\n", idPayment, err)
		}
	}

	explorerURL := fmt.Sprintf("https://sepolia.etherscan.io/tx/%s", txHash)

	c.JSON(http.StatusOK, &Response.ExecuteVndPaymentResponse{
		IDPayment:   idPayment,
		TxHash:      txHash,
		Status:      Constants.StatusSuccess,
		ExplorerURL: explorerURL,
	})
}

// GetNonce returns the current on-chain nonce for a wallet address.
// GET /api/v1/payments/vnd/nonce/:address
func (vc *vndPaymentController) GetNonce(c *gin.Context) {
	address := c.Param("address")
	if address == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Address is required"})
		return
	}

	// FIX 5: Validate Ethereum address format before RPC call
	if !common.IsHexAddress(address) {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid Ethereum address format"})
		return
	}

	nonce, err := vc._blockchainService.GetNonce(address)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": fmt.Sprintf("Failed to get nonce: %v", err)})
		return
	}

	c.JSON(http.StatusOK, gin.H{"address": address, "nonce": nonce})
}
