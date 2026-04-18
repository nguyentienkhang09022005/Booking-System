package com.example.java_service.Controllers;

import com.example.java_service.Constant.PredefinedRoleChattingAI;
import com.example.java_service.Dto.Request.ChatbotRequest;
import com.example.java_service.Dto.Request.MessageHistoryItem;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Services.ChatHistoryService;
import com.example.java_service.Services.Interfaces.IBookingAssistantAgent;
import com.example.java_service.Services.KnowledgeBaseSyncService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/chatbot")
public class ChatbotController
{
    IBookingAssistantAgent assistantAgent;
    KnowledgeBaseSyncService knowledgeBaseSyncService;
    ChatHistoryService chatHistoryService;

    static final Long DURATION_HISTORY_CHAT_WITH_AI = 604800L;

    @PostMapping("/ask")
    public ApiResponse<String> askChatbot(@RequestBody ChatbotRequest request)
    {
        if (request.getIdUser() == null || request.getIdUser().isBlank()) {
            throw new IllegalArgumentException("idUser không được để trống");
        }
        if (request.getUserMessage() == null || request.getUserMessage().isBlank()) {
            throw new IllegalArgumentException("userMessage không được để trống");
        }

        log.info("Chat request - idUser: {}, message: {}", request.getIdUser(), request.getUserMessage());

        List<MessageHistoryItem> history = chatHistoryService.getHistoryChat(request.getIdUser());
        
        String answer = assistantAgent.chat(request.getIdUser(), request.getUserMessage(), history);

        chatHistoryService.saveChat(
                request.getIdUser(),
                MessageHistoryItem.builder()
                        .role(PredefinedRoleChattingAI.USER_ROLE)
                        .message(request.getUserMessage())
                        .build(),
                DURATION_HISTORY_CHAT_WITH_AI
        );
        chatHistoryService.saveChat(
                request.getIdUser(),
                MessageHistoryItem.builder()
                        .role(PredefinedRoleChattingAI.MODEL_ROLE)
                        .message(answer)
                        .build(),
                DURATION_HISTORY_CHAT_WITH_AI
        );

        log.info("Chat response - idUser: {}", request.getIdUser());
        return ApiResponse.<String>builder()
                .message("Trả lời chatbot thành công!")
                .data(answer)
                .build();
    }

    @PostMapping("/admin/sync-knowledge")
    public ApiResponse<String> syncKnowledgeBase()
    {
        log.info("Admin trigger sync knowledge base");
        String result = knowledgeBaseSyncService.syncDataToVectorDb();
        return ApiResponse.<String>builder()
                .message(result)
                .build();
    }

    @DeleteMapping("/memory")
    public ApiResponse<String> clearMemory(@RequestParam String idUser)
    {
        log.info("Clear chat memory - idUser: {}", idUser);
        chatHistoryService.deleteHistoryChat(idUser);
        return ApiResponse.<String>builder()
                .message("Đã xóa lịch sử chat!")
                .build();
    }
}
