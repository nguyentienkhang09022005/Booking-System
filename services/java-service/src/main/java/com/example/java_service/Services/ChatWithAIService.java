package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedRoleChattingAI;
import com.example.java_service.Dto.Request.ChatRequest;
import com.example.java_service.Dto.Request.MessageHistoryItem;
import com.example.java_service.Dto.Response.ChatResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Repositories.Jpa.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatWithAIService
{
    ChatHistoryService chatHistoryService;
    GeminiService geminiService;
    UserRepository userRepository;

    static final Long DURATION_HISTORY_CHAT_WITH_AI = 604800L;

    public ChatResponse sendChatForAI(ChatRequest chatRequest)
    {
        userRepository.findById(UUID.fromString(chatRequest.getIdUser())).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        String systemIntroduction = "chat with AI";

        List<MessageHistoryItem> historyItems = chatHistoryService.getHistoryChat(chatRequest.getIdUser());
        String aiMessage = geminiService.GenerateChatUserModelResponse(
                systemIntroduction,
                historyItems,
                chatRequest.getUserMessage());

        aiMessage = aiMessage.replace("\\n", "\n").replace("\\r", "");

        // Lưu redis
        chatHistoryService.saveChat(
                chatRequest.getIdUser(),
                new MessageHistoryItem(
                        PredefinedRoleChattingAI.USER_ROLE,
                        chatRequest.getUserMessage()
                ),
                DURATION_HISTORY_CHAT_WITH_AI
        );
        chatHistoryService.saveChat(
                chatRequest.getIdUser(),
                new MessageHistoryItem(
                        PredefinedRoleChattingAI.MODEL_ROLE,
                        aiMessage
                ),
                DURATION_HISTORY_CHAT_WITH_AI
        );

        return ChatResponse.builder()
                .AiResponse(aiMessage)
                .build();
    }

    public List<MessageHistoryItem> ListChatHistory(String idUser) {
        userRepository.findById(UUID.fromString(idUser)).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        return chatHistoryService.getHistoryChat(idUser);
    }

    public ResponseEntity<String> deleteChatHistory(String idUser) {
        userRepository.findById(UUID.fromString(idUser)).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        chatHistoryService.deleteRefreshToken(idUser);
        return ResponseEntity.ok("Xóa lịch sử trò chuyện với AI thành công!");
    }
}
