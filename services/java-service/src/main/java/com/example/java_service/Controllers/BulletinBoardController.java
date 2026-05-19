package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.BulletinBoardApprovalRequest;
import com.example.java_service.Dto.Request.BulletinBoardCreationRequest;
import com.example.java_service.Dto.Request.BulletinBoardUpdateRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.BulletinBoardResponse;
import com.example.java_service.Services.BulletinBoardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/bulletin")
public class BulletinBoardController {

    BulletinBoardService bulletinBoardService;

    // Tạo bản tin mới
    @PostMapping("/creation")
    public ApiResponse<BulletinBoardResponse> createBulletin(@RequestBody BulletinBoardCreationRequest request) {
        return ApiResponse.<BulletinBoardResponse>builder()
                .message("Tạo bản tin thành công!")
                .data(bulletinBoardService.createBulletin(request))
                .build();
    }

    // Lấy danh sách bản tin active
    @GetMapping("/list")
    public ApiResponse<List<BulletinBoardResponse>> getBulletins(
            @RequestParam(defaultValue = "CUSTOMER") String audience) {
        return ApiResponse.<List<BulletinBoardResponse>>builder()
                .message("Lấy danh sách bản tin thành công!")
                .data(bulletinBoardService.getActiveBulletins(audience))
                .build();
    }

    // Lấy chi tiết bản tin
    @GetMapping("/{id}")
    public ApiResponse<BulletinBoardResponse> getBulletinDetail(@PathVariable UUID id) {
        return ApiResponse.<BulletinBoardResponse>builder()
                .message("Lấy thông tin chi tiết bản tin thành công!")
                .data(bulletinBoardService.getBulletinDetail(id))
                .build();
    }

    // Cập nhật bản tin
    @PatchMapping("/{id}")
    public ApiResponse<BulletinBoardResponse> updateBulletin(
            @PathVariable UUID id,
            @RequestBody BulletinBoardUpdateRequest request) {
        return ApiResponse.<BulletinBoardResponse>builder()
                .message("Cập nhật bản tin thành công!")
                .data(bulletinBoardService.updateBulletin(id, request, request.getIdUser()))
                .build();
    }

    // Xóa bản tin
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBulletin(
            @PathVariable UUID id,
            @RequestParam UUID idUser) {
        bulletinBoardService.deleteBulletin(id, idUser);
        return ApiResponse.<Void>builder()
                .message("Xóa bản tin thành công!")
                .build();
    }

    // Lấy danh sách bản tin chờ duyệt (Admin)
    @GetMapping("/pending")
    public ApiResponse<Page<BulletinBoardResponse>> getPendingBulletins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.<Page<BulletinBoardResponse>>builder()
                .message("Lấy danh sách bản tin chờ duyệt thành công!")
                .data(bulletinBoardService.getPendingBulletins(pageRequest))
                .build();
    }

    // Lấy danh sách bản tin đã tạo (của user hiện tại, có filter theo status)
    @GetMapping("/my-bulletins")
    public ApiResponse<List<BulletinBoardResponse>> getMyBulletins(
            @RequestParam UUID idUser,
            @RequestParam(required = false) String status) {
        return ApiResponse.<List<BulletinBoardResponse>>builder()
                .message("Lấy danh sách bản tin đã tạo thành công!")
                .data(bulletinBoardService.getMyBulletins(idUser, status))
                .build();
    }

    // Admin duyệt bản tin
    @PatchMapping("/{id}/approve")
    public ApiResponse<BulletinBoardResponse> approveBulletin(@PathVariable UUID id) {
        return ApiResponse.<BulletinBoardResponse>builder()
                .message("Phê duyệt bản tin thành công!")
                .data(bulletinBoardService.approveBulletin(id))
                .build();
    }

    // Admin từ chối bản tin
    @PatchMapping("/{id}/reject")
    public ApiResponse<BulletinBoardResponse> rejectBulletin(
            @PathVariable UUID id,
            @RequestBody BulletinBoardApprovalRequest request) {
        return ApiResponse.<BulletinBoardResponse>builder()
                .message("Từ chối bản tin thành công!")
                .data(bulletinBoardService.rejectBulletin(id, request))
                .build();
    }
}