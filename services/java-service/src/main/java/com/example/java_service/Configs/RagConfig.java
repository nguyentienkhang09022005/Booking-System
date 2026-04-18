package com.example.java_service.Configs;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel; // Keep for embedding model (RAG still needs it)
// import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel; // COMMENTED: Using Groq instead for chat
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.time.Duration;

@Configuration
public class RagConfig
{
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.base.url}")
    private String groqBaseUrl;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private static final int MAX_RESULTS = 5;
    private static final double MIN_SCORE = 0.7;
    private static final int VECTOR_DIMENSION = 768;

    // @Bean
    // public ChatLanguageModel geminiChatModel()
    // {
    //     return GoogleAiGeminiChatModel.builder()
    //             .apiKey(geminiApiKey)
    //             .modelName("gemini-2.5-flash")
    //             .temperature(0.3)
    //             .timeout(Duration.ofSeconds(60))
    //             .build();
    // }

    @Bean
    public ChatLanguageModel groqChatModel()
    {
        return OpenAiChatModel.builder()
                .apiKey(groqApiKey)
                .baseUrl(groqBaseUrl)
                .modelName("llama-3.3-70b-versatile")
                .temperature(0.1)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel()
    {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(geminiApiKey)
                .modelName("gemini-embedding-001")
                .outputDimensionality(VECTOR_DIMENSION)
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore()
    {
        try {
            String cleanUrl = dbUrl.replaceFirst("jdbc:", "");
            URI uri = new URI(cleanUrl);

            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String database = uri.getPath().substring(1);

            return PgVectorEmbeddingStore.builder()
                    .host(host)
                    .port(port)
                    .database(database)
                    .user(dbUser)
                    .password(dbPassword)
                    .table("booking_knowledge_base")
                    .dimension(VECTOR_DIMENSION)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi phân tích cú pháp spring.datasource.url: " + dbUrl, e);
        }
    }

    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore,
                                             EmbeddingModel embeddingModel)
    {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(MAX_RESULTS)
                .minScore(MIN_SCORE)
                .build();
    }
}
