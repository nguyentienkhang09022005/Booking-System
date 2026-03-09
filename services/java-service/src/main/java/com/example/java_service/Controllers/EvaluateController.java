package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.EvaluateCreationRequest;
import com.example.java_service.Dto.Request.EvaluateUpdateRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.EvaluateResponse;
import com.example.java_service.Services.EvaluatesService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/evaluate")
public class EvaluateController
{
    EvaluatesService evaluatesService;

    // Endpoint lấy danh sách đánh giá theo paging 5
    @GetMapping("/getListEvaluate")
    public ApiResponse<Page<EvaluateResponse>> getListEvaluate(@RequestParam String refId,
                                                             @RequestParam String refType,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "3") int size)
    {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.<Page<EvaluateResponse>>builder()
                .message("Lấy danh sách đánh giá thành công!")
                .data(evaluatesService.getListEvaluateByPaging(pageable, refId, refType))
                .build();
    }

    // Endpoint tạo đánh giá
    @PostMapping( "/creation")
    public ApiResponse<EvaluateResponse> createEvaluate(@RequestBody EvaluateCreationRequest request)
    {
        return ApiResponse.<EvaluateResponse>builder()
                .message("Tạo đánh giá thành công!")
                .data(evaluatesService.saveEvaluate(request))
                .build();
    }

    // Endpoint xóa đánh giá qua id
    @DeleteMapping( "/delete")
    public ApiResponse<EvaluateResponse> deleteEvaluate(@RequestParam String idEvaluate)
    {
        var response = evaluatesService.deleteEvaluate(idEvaluate);
        return ApiResponse.<EvaluateResponse>builder()
                .message(response.getBody())
                .build();
    }

    // Endpoint sửa đánh giá
    @PatchMapping( "/update")
    public ApiResponse<EvaluateResponse> updateEvaluate(@RequestBody EvaluateUpdateRequest request,
                                                        @RequestParam String idEvaluate)
    {
        return ApiResponse.<EvaluateResponse>builder()
                .message("Sửa đánh giá thành công!")
                .data(evaluatesService.updateEvaluate(request, idEvaluate))
                .build();
    }
}
