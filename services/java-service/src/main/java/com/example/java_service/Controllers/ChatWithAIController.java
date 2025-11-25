package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.ChatRequest;
import com.example.java_service.Dto.Request.MessageHistoryItem;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.ChatResponse;
import com.example.java_service.Services.ChatWithAIService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/AI")
public class ChatWithAIController
{
    ChatWithAIService chatWithAIService;

    // Endpoint lấy lịch sử trò chuyện với AI
    @GetMapping( "/list-chatting-history")
    public ApiResponse<List<MessageHistoryItem>> getListChattingHistory(@RequestParam String idUser)
    {
        return ApiResponse.<List<MessageHistoryItem>>builder()
                .message("Lấy lịch sử trò chuyện thành công!")
                .data(chatWithAIService.ListChatHistory(idUser))
                .build();
    }

    // Endpoint gửi tin nhắn cho AI
    @PostMapping( "/send-message")
    public ApiResponse<ChatResponse> sendMessage(@RequestBody ChatRequest chatRequest)
    {
        return ApiResponse.<ChatResponse>builder()
                .message("Gửi tin nhắn cho AI thành công!")
                .data(chatWithAIService.sendChatForAI(chatRequest))
                .build();
    }

    // Endpoint xóa lịch sử nhắn tin với AI
    @DeleteMapping( "/delete-history")
    public ApiResponse<ResponseEntity<String>> deleteHistory(@RequestParam String idUser)
    {
        var response = chatWithAIService.deleteChatHistory(idUser);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }
}
