package Controllers

import (
	"fmt"
	"math/big"
	"net/http"
	"time"

	"github.com/ethereum/go-ethereum/common"
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"

	"payment-service/Configs"
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
// @Summary Prepare VND Payment
// @Description Prepares EIP-712 typed data for the user to sign before executing the payment.
// @Tags VndPayment
// @Accept json
// @Produce json
// @Param request body Request.PrepareVndPaymentRequest true "Prepare Payment Request"
// @Success 201 {object} Response.PrepareVndPaymentResponse
// @Failure 400 {object} map[string]string "Bad Request"
// @Failure 500 {object} map[string]string "Internal Server Error"
// @Router /api/v1/payments/vnd/prepare [post]
func (vc *vndPaymentController) PrepareVndPayment(c *gin.Context) {
	var req Request.PrepareVndPaymentRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	result, err := vc._blockchainService.PreparePaymentOrder(*req.IDBooking, req.PayerAddress, req.Amount)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": fmt.Sprintf("Failed to prepare payment order: %v", err)})
		return
	}

	payment, err := vc._paymentRepository.CreateVndPayment(
		result.IDPayment,
		req.IDUser,
		req.IDBooking,
		req.IDBookingTour,
		req.IDBookingTransport,
		req.Amount,
		result.OrderID,
	)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": fmt.Sprintf("Failed to create payment record: %v", err)})
		return
	}

	resp := &Response.PrepareVndPaymentResponse{
		IDPayment:       payment.IDPayment,
		TypedData:       result.TypedData,
		OrderID:         result.OrderID,
		VndTokenAddress: Configs.VNDTokenAddress,
	}

	c.JSON(http.StatusCreated, resp)
}

// ExecuteVndPayment verifies the signature and submits the on-chain transaction.
// @Summary Execute VND Payment
// @Description Verifies the signature and submits the on-chain transaction to Sepolia.
// @Tags VndPayment
// @Accept json
// @Produce json
// @Param request body Request.ExecuteVndPaymentRequest true "Execute Payment Request"
// @Success 200 {object} Response.ExecuteVndPaymentResponse
// @Failure 400 {object} map[string]string "Bad Request or Invalid Signature"
// @Failure 404 {object} map[string]string "Payment record not found"
// @Failure 409 {object} map[string]string "Conflict - Already being processed"
// @Failure 500 {object} map[string]string "Internal Server Error"
// @Router /api/v1/payments/vnd/execute [post]
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

	// Pre-check deadline with 60s buffer for mining time — avoids wasting relayer gas
	bufferSeconds := int64(60)
	if time.Now().Unix()+bufferSeconds > req.Order.Deadline {
		_ = vc._paymentRepository.UpdateVndPaymentFailed(idPayment, "")
		c.JSON(http.StatusBadRequest, gin.H{"error": "Payment order has expired or will expire before mining"})
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

	// If permit signature provided, execute permit first (gasless ERC20 approval)
	if req.Permit != nil && req.Permit.R != "" {
		permitValue := new(big.Int)
		permitValue.SetString(req.Permit.Value, 10)
		_, err := vc._blockchainService.ExecutePermit(
			req.Order.Payer,
			Configs.PaymentGatewayAddress,
			permitValue,
			req.Permit.Deadline,
			req.Permit.V,
			req.Permit.R,
			req.Permit.S,
		)
		if err != nil {
			_ = vc._paymentRepository.UpdateVndPaymentFailed(idPayment, "")
			c.JSON(http.StatusInternalServerError, gin.H{"error": fmt.Sprintf("Permit transaction failed: %v", err)})
			return
		}
	}

	// Submit payment transaction to Sepolia via relayer
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
	if payment.Booking != nil && payment.Booking.IDAccommodation != uuid.Nil {
		now := time.Now()
		revenueEvent := Request.RevenueCreatedEvent{
			IDBooking:       payment.IDBooking,
			IDExpense:       nil,
			IDAccommodation: payment.Booking.IDAccommodation,
			Amount:          payment.Amount,
			Month:           int(now.Month()),
			Year:            now.Year(),
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
// @Summary Get Wallet Nonce
// @Description Returns the current on-chain nonce for a specific Ethereum wallet address.
// @Tags VndPayment
// @Produce json
// @Param address path string true "Ethereum Wallet Address"
// @Success 200 {object} map[string]interface{}
// @Failure 400 {object} map[string]string "Bad Request - Invalid Address"
// @Failure 500 {object} map[string]string "Internal Server Error"
// @Router /api/v1/payments/vnd/nonce/{address} [get]
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
