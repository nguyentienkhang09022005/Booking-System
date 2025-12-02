package Helper

import (
	"github.com/gin-gonic/gin"
	"net/http"
	"payment-service/Internal/Dtos/Response"
)

func Success(c *gin.Context, message string, data interface{}) {
	c.JSON(http.StatusOK, Response.ApiResponse{
		Code:    http.StatusOK,
		Message: message,
		Data:    data,
	})
}

func Created(c *gin.Context, message string, data interface{}) {
	c.JSON(http.StatusCreated, Response.ApiResponse{
		Code:    http.StatusCreated,
		Message: message,
		Data:    data,
	})
}

func Error(c *gin.Context, status int, message string) {
	c.JSON(status, Response.ApiResponse{
		Code:    status,
		Message: message,
	})
}
