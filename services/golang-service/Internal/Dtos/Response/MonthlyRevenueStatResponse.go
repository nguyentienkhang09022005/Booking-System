package Response

type MonthlyRevenueStatResponse struct {
	Month        int     `json:"month"`
	TotalIncome  float64 `json:"total_income"`
	TotalExpense float64 `json:"total_expense"`
}
