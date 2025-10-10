package Request

type ZaloPayCallBackRequest struct {
	Data string `form:"data" binding:"required"`
	Mac  string `form:"mac" binding:"required"`
}
