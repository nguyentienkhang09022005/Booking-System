package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.GenerateScheduleRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.ItineraryResponse;
import com.example.java_service.Services.AISchedulerService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/ai-scheduler")
public class AISchedulerController {

    AISchedulerService aiSchedulerService;

    @PostMapping("/generate")
    public ApiResponse<ItineraryResponse> generateSchedule(@RequestBody GenerateScheduleRequest request) {
        return ApiResponse.<ItineraryResponse>builder()
                .message("Tạo lịch trình AI thành công!")
                .data(aiSchedulerService.generateSchedule(request.getIdItinerary()))
                .build();
    }
}