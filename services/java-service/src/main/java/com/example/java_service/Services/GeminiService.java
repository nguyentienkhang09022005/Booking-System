package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedRoleChattingAI;
import com.example.java_service.Dto.Request.GeminiRequest;
import com.example.java_service.Dto.Request.MessageHistoryItem;
import com.example.java_service.Dto.Response.GeminiResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GeminiService
{
    @NonFinal
    @Value("${gemini.apiKey.user}")
    protected String GEMINI_API_KEY_USER;

    @NonFinal
    @Value("${gemini.apiKey.company}")
    protected String GEMINI_API_KEY_COMPANY;

    WebClient aiModel;

    public String GenerateChatUserModelResponse(String systemInstruction,
                                                List<MessageHistoryItem> historyItems,
                                                String userMessage)
    {
        GeminiRequest request = new GeminiRequest();

        GeminiRequest.SystemInstruction sys = new GeminiRequest.SystemInstruction(
                List.of(new GeminiRequest.GeminiPart(systemInstruction))
        );
        request.setSystemInstruction(sys);

        List<GeminiRequest.GeminiContent> contents =
                historyItems.stream()
                        .map(item -> new GeminiRequest.GeminiContent(
                                item.getRole(),
                                List.of(new GeminiRequest.GeminiPart(item.getMessage()))
                        ))
                        .collect(Collectors.toCollection(ArrayList::new));


        contents.add(new GeminiRequest.GeminiContent(
                PredefinedRoleChattingAI.USER_ROLE,
                List.of(new GeminiRequest.GeminiPart(userMessage))
        ));
        request.setContents(contents);

        try
        {
            GeminiResponse geminiResponse = aiModel
                    .post()
                    .uri("?key=" + GEMINI_API_KEY_USER)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .block();

            if (geminiResponse == null ||
                    geminiResponse.getCandidates() == null ||
                    geminiResponse.getCandidates().isEmpty()) {

                return "Xin lỗi, tôi không thể tạo phản hồi lúc này.";
            }

            return geminiResponse.getCandidates()
                    .get(0)
                    .getContent()
                    .getParts()
                    .get(0)
                    .getText();
        }
        catch (Exception ex) {
            log.error("Gemini API error", ex);
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
