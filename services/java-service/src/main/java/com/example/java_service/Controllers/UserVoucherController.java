package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.UserVoucherRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.UserVoucherResponse;
import com.example.java_service.Services.UserVoucherService;
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
@RequestMapping("/user-voucher")
public class UserVoucherController
{
    UserVoucherService userVoucherService;

    // Endpoint lấy danh sách voucher đã lưu và còn hạn
    @GetMapping("/list-voucher-saved-and-valid")
    public ApiResponse<List<UserVoucherResponse>> listVoucherSavedAndValid(@RequestParam UUID idUser)
    {
        return ApiResponse.<List<UserVoucherResponse>>builder()
                .message("Lấy danh sách voucher đã lưu và còn hạn thành công!")
                .data(userVoucherService.listUserVoucherSavedAndValid(idUser))
                .build();
    }

    // Endpoint lưu voucher
    @PostMapping("/save-voucher")
    public ApiResponse<ResponseEntity<String>> saveVoucher(@RequestBody UserVoucherRequest request)
    {
        var response = userVoucherService.saveUserVoucher(request);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }
}
