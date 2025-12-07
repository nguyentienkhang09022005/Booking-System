package Errors

import "fmt"

type ApiError struct {
	Status  int
	Code    int
	Message string
}

func (e *ApiError) Error() string {
	return fmt.Sprintf("%d - %s", e.Code, e.Message)
}

func New(status int, errCode ErrorCode) *ApiError {
	return &ApiError{
		Status:  status,
		Code:    errCode.Code,
		Message: errCode.Message,
	}
}
