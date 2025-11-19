package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.ExpenseCreationRequest;
import com.example.java_service.Dto.Request.ExpenseUpdateRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.ExpenseResponse;
import com.example.java_service.Services.ExpenseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/expense")
public class ExpenseController
{
    ExpenseService expenseService;

    // Endpoint tạo chi tiêu mới
    @PostMapping("/creation")
    public ApiResponse<ExpenseResponse> creationExpense(@RequestBody ExpenseCreationRequest request)
    {
        return ApiResponse.<ExpenseResponse>builder()
                .message("Tạo chi tiêu thành công!")
                .data(expenseService.saveExpense(request))
                .build();
    }

    // Endpoint lấy danh sách chi tiêu của khách sạn
    @GetMapping("/get-list-expense-of-hotel")
    public ApiResponse<List<ExpenseResponse>> getExpenses(@RequestParam UUID idHotel)
    {
        return ApiResponse.<List<ExpenseResponse>>builder()
                .message("Lấy danh sách chi tiêu thành công!")
                .data(expenseService.getListExpense(idHotel))
                .build();
    }

    // Endpoint lấy thông tin chi tiết chi tiêu
    @GetMapping( "/get-inf-expense")
    public ApiResponse<ExpenseResponse> getInfExpense(@RequestParam UUID idExpense)
    {
        return ApiResponse.<ExpenseResponse>builder()
                .message("Lấy thông tin chi tiết chi tiêu thành công!")
                .data(expenseService.getInfExpense(idExpense))
                .build();
    }

    // Endpoint xóa chi tiêu
    @DeleteMapping( "/delele")
    public ApiResponse<ResponseEntity<String>> deleteExpense(@RequestParam UUID idExpense)
    {
        var response = expenseService.deleteExpense(idExpense);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    // Endpoint cập nhật chi tiêu
    @PatchMapping( "/update")
    public ApiResponse<ExpenseResponse> updateExpense(@RequestParam UUID idExpense,
                                                      @RequestBody ExpenseUpdateRequest request)
    {
        return ApiResponse.<ExpenseResponse>builder()
                .message("Cập nhật thông tin chi tiêu thành công!")
                .data(expenseService.updateExpense(request, idExpense))
                .build();
    }
}
