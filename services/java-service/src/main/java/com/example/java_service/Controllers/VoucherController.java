package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.BusinessVoucherCreationRequest;
import com.example.java_service.Dto.Request.SystemVoucherCreationRequest;
import com.example.java_service.Dto.Request.VoucherUpdateRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.VoucherEditResponse;
import com.example.java_service.Dto.Response.VoucherViewAllResponse;
import com.example.java_service.Dto.Response.VoucherViewResponse;
import com.example.java_service.Services.VoucherService;
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
@RequestMapping("/voucher")
public class VoucherController
{
    VoucherService voucherService;

    // Endpoint lấy danh sách voucher của nơi cư trú đã được duyệt
    @GetMapping("/list-voucher-approved-by-accommodation")
    public ApiResponse<List<VoucherViewResponse>> listVoucherApprovedByAccommodation(@RequestParam UUID idAccommodation, @RequestParam UUID idUser)
    {
        return ApiResponse.<List<VoucherViewResponse>>builder()
                .message("Lấy danh sách voucher của nơi cư trú thành công!")
                .data(voucherService.listVouchersApprovedByAccommodation(idAccommodation, idUser))
                .build();
    }

    // Endpoint lấy danh sách voucher đã được duyệt
    @GetMapping("/list-voucher-approved")
    public ApiResponse<List<VoucherViewAllResponse>> listVoucherApproved(@RequestParam UUID idUser)
    {
        return ApiResponse.<List<VoucherViewAllResponse>>builder()
                .message("Lấy danh sách voucher thành công!")
                .data(voucherService.listVouchersApproved(idUser))
                .build();
    }

    // Endpoint lấy danh sách voucher chưa phê duyệt
    @GetMapping("/list-voucher-not-approved")
    public ApiResponse<List<VoucherEditResponse>> listVoucherNotApproved()
    {
        return ApiResponse.<List<VoucherEditResponse>>builder()
                .message("Lấy danh sách voucher chưa phê duyệt thành công!")
                .data(voucherService.listVouchersNotApproved())
                .build();
    }

    // Endpoint lấy danh sách voucher của nơi cư trú
    @GetMapping("/list-voucher")
    public ApiResponse<List<VoucherEditResponse>> listVoucher(@RequestParam UUID idAccommodation)
    {
        return ApiResponse.<List<VoucherEditResponse>>builder()
                .message("Lấy danh sách voucher của nơi cư trú thành công!")
                .data(voucherService.listVouchersOfAccommodation(idAccommodation))
                .build();
    }

    @PostMapping("/business/creation")
    public ApiResponse<VoucherViewResponse> creationBusinessVoucher(@RequestBody BusinessVoucherCreationRequest request)
    {
        return ApiResponse.<VoucherViewResponse>builder()
                .message("Doanh nghiệp tạo voucher thành công!")
                .data(voucherService.saveBusinessVoucher(request))
                .build();
    }

    @PatchMapping("/business/update")
    public ApiResponse<VoucherViewResponse> updateBusinessVoucher(@RequestBody VoucherUpdateRequest request,
                                                                  @RequestParam UUID idVoucher)
    {
        return ApiResponse.<VoucherViewResponse>builder()
                .message("Thay đổi thông tin voucher doanh nghiệp thành công!")
                .data(voucherService.updateBusinessVoucher(request, idVoucher))
                .build();
    }

    @PostMapping("/system/creation")
    public ApiResponse<VoucherViewResponse> creationSystemVoucher(@RequestBody SystemVoucherCreationRequest request)
    {
        return ApiResponse.<VoucherViewResponse>builder()
                .message("Hệ thống tạo voucher thành công!")
                .data(voucherService.saveSystemVoucher(request))
                .build();
    }

    @PatchMapping("/system/update")
    public ApiResponse<VoucherViewResponse> updateSystemVoucher(@RequestBody VoucherUpdateRequest request,
                                                                @RequestParam UUID idVoucher)
    {
        return ApiResponse.<VoucherViewResponse>builder()
                .message("Thay đổi thông tin voucher hệ thống thành công!")
                .data(voucherService.updateSystemVoucher(request, idVoucher))
                .build();
    }

    // Endpoint xóa voucher
    @DeleteMapping( "/delele")
    public ApiResponse<ResponseEntity<String>> deleteVoucher(@RequestParam UUID idVoucher)
    {
        var response = voucherService.deleteVoucher(idVoucher);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }
}
