# Chatbot AI Module Specification

## 1. Overview

Chatbot AI là trợ lý ảo trả lời câu hỏi về dịch vụ du lịch trong hệ thống BookingNext, xây dựng trên
**LangChain4j** với mô hình **function-calling (Tools)**.

- **LLM (chat):** Groq `meta-llama/llama-4-scout-17b-16e-instruct` (temperature 0.3, timeout 120s)
- **Embedding:** Google `gemini-embedding-001` (768 dimensions) — chỉ dùng cho sync knowledge base
- **Vector Store:** PgVector (`booking_knowledge_base`)

> **LƯU Ý:** Bean `ContentRetriever` (RAG vector search) **đang bị DISABLE** trong `RagConfig.java`
> (gây tràn context với model nhỏ). Chatbot trả lời **dựa hoàn toàn vào Tools query trực tiếp
> PostgreSQL**, không dùng vector similarity search khi chat. Vector DB vẫn được sync nhưng chưa
> được truy vấn trong luồng chat.

## 2. Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Frontend (Node.js)                                │
│                         WebSocket / REST Client                              │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │ HTTP POST /chatbot/ask
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ChatbotController                                    │
│  - Nhận request từ Node.js                                                  │
│  - Lấy 5 message gần nhất từ ChatHistoryService (Redis)                      │
│  - Gọi IBookingAssistantAgent.chat(idUser, message, history)                 │
│  - Lưu USER + MODEL message, bắt rate_limit (OpenAiHttpException)            │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                  IBookingAssistantAgent (@AiService)                         │
│  - LangChain4j implement tự động                                            │
│  - @SystemMessage định nghĩa behavior + hướng dẫn chọn tool                  │
│  - LLM Groq llama-4-scout-17b tự gọi Tools                                   │
│  - History truyền qua @V("history"), key = @MemoryId idUser                  │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
          ┌───────────────────────┴───────────────────────┐
          │                                               │
          ▼                                               ▼
┌──────────────────────────────┐          ┌─────────────────────────────────┐
│ BookingChatTools (32 @Tool)  │          │  ChatHistory (Redis, thủ công)  │
│ - Query PostgreSQL qua Service│         │  - max 15 msg/user, TTL 7 ngày  │
│ - Cache qua RagCacheService   │         └─────────────────────────────────┘
└──────────────┬───────────────┘
               ▼
        ┌──────────────┐
        │ PostgreSQL   │
        └──────────────┘

  (PgVector chỉ dùng cho sync — retriever đang tắt, KHÔNG truy vấn khi chat)
```

## 3. API Endpoints

Base Path: `/chatbot`

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/ask` | Giao tiếp với chatbot |
| POST | `/admin/sync-knowledge` | Admin đồng bộ knowledge base |
| GET | `/history?idUser={uuid}` | Lấy lịch sử chat |
| DELETE | `/memory?idUser={uuid}` | Xóa chat memory |

### POST /chatbot/ask

**Request:**
```json
{
  "idUser": "uuid",
  "userMessage": "Tour Đà Lạt giá bao nhiêu?"
}
```

**Response:**
```json
{
  "message": "Trả lời chatbot thành công!",
  "data": "Tour Đà Lạt có giá từ 1,200,000 VND..."
}
```

## 4. Core Components

### IBookingAssistantAgent.java
```java
@AiService
public interface IBookingAssistantAgent {
    @SystemMessage({ "Bạn là trợ lý AI cho hệ thống Booking.", /* ... */ })
    String chat(@MemoryId String idUser,
                @UserMessage String userMessage,
                @V("history") List<MessageHistoryItem> history);
}
```

### RagConfig.java
```java
@Configuration
public class RagConfig {
    // chatModel()        - Groq OpenAiChatModel (llama-4-scout-17b), @Primary
    // embeddingModel()   - GoogleAiEmbeddingModel gemini-embedding-001, 768 dim
    // embeddingStore()   - PgVectorEmbeddingStore (booking_knowledge_base)
    // contentRetriever() - ĐANG COMMENT (disabled)
    // isValidUuid()      - helper validate UUID
}
```

### BookingChatTools.java — 32 @Tool

| Nhóm | Tools |
|------|-------|
| Tour (4) | `searchTours`, `getTourBasicInfo`, `getTourDetails` (gồm lịch trình + inclusions/exclusions), `getAllTours` |
| Accommodation (4) | `searchAccommodations`, `getAccommodationBasicInfo`, `getAccommodationDetails` (gồm hình ảnh, tiện ích, quy định, đánh giá), `getAllAccommodations` |
| Room (2) | `getRoomsByAccommodation`, `getRoomDetails` |
| Service (3) | `searchServices` (tên/mô tả/loại), `getServiceDetails`, `getServicesByAccommodation` |
| Voucher (3) | `searchVouchers`, `getVouchersByAccommodation`, `getAllActiveVouchers` |
| Transport Ticket (4) | `searchTransportTickets`, `getTransportTicketBasicInfo`, `getTransportTicketDetails`, `getAllTransportTickets` |
| User & Booking (6) | `getUserProfile`, `getLoyaltyHistory`, `getUserTourBookings`, `getUserTransportBookings`, `getBookingTourDetail`, `getBookingTransportDetail` |
| Favorite & Evaluate (3) | `getUserFavoriteAccommodations`, `getUserFavoriteTours`, `getEvaluates` |
| Price Compare (3) | `compareRoomPrices`, `findTopRatedAccommodations`, `findBestValueTours` |

> Tool nhận tên hoặc UUID. Helper `searchXxxIds()` resolve tên → UUID; nếu nhiều kết quả sẽ báo
> "ambiguous" và yêu cầu user cung cấp tên cụ thể hơn. Tool cá nhân (profile, booking, loyalty,
> favorite) lấy user từ `CurrentUserContext` (header `idUser` do gateway inject), không nhận UUID từ LLM.

## 5. Knowledge Base Sync

Admin gọi `POST /chatbot/admin/sync-knowledge` để đồng bộ **9 nguồn dữ liệu**:

| # | Data | Description |
|---|------|-------------|
| 1 | General Regulations | Quy định, chính sách |
| 2 | Tours | Thông tin tour du lịch |
| 3 | Accommodations | Nơi cư trú (đã duyệt) |
| 4 | Rooms | Loại phòng và giá |
| 5 | Services | Dịch vụ khách sạn |
| 6 | Vouchers | Mã giảm giá (đã duyệt) |
| 7 | Transport Tickets | Vé phương tiện (đã duyệt) |
| 8 | Room Types | Danh sách loại phòng |
| 9 | Service Types | Danh sách loại dịch vụ |

**Sync Process:**
1. Fetch data từ PostgreSQL, build corpus (StringBuilder)
2. Split chunks: `DocumentSplitters.recursive(500, 50)` — 500 ký tự, overlap 50
3. Generate embeddings (768 dim) theo batch 20 segment, retry tối đa 3 lần, delay 50s khi rate-limit
4. Store vào PgVector
5. `ragCacheService.evictAllRagCache()` — xóa cache sau sync

## 6. Chat Memory

History được quản lý **thủ công qua Redis** (không dùng `ChatMemoryProvider` của LangChain4j):

```
ChatHistoryService:
  - Tối đa 15 message/user
  - TTL 7 ngày (604800s)
  - Key: chat_history:{idUser}

ChatbotController:
  - Lấy 5 message gần nhất truyền vào agent qua @V("history")
  - @MemoryId idUser làm key memory cho LangChain4j
```

## 7. Supporting Services

### ChatHistoryService.java
- `getHistoryChat(String idUser)` — Lấy lịch sử chat
- `saveChat(String idUser, MessageHistoryItem message, long ttlSeconds)` — Lưu chat
- `deleteHistoryChat(String idUser)` — Xóa lịch sử

### KnowledgeBaseSyncService.java
- `syncDataToVectorDb()` — Đồng bộ 9 nguồn dữ liệu vào Vector DB

### RagCacheService.java
- Cache Redis cho tool results (prefix `rag_cache:`, TTL mặc định 300s)
- `evictAllRagCache()`, `evictAccommodationCache()`, `evictTourCache()`, `evictVoucherCache()`

## 8. Database Schema

### Vector Table (PostgreSQL/PgVector)
```sql
CREATE TABLE booking_knowledge_base (
    embedding_id UUID PRIMARY KEY,
    text TEXT,
    embedding VECTOR(768) NOT NULL,
    metadata JSONB
);

CREATE INDEX booking_knowledge_base_embedding_idx 
ON booking_knowledge_base 
USING hnsw (embedding vector_cosine_ops);
```

## 9. Related Files

| Type | Path |
|------|------|
| Controller | `Controllers/ChatbotController.java` |
| Agent Interface | `Services/Interfaces/IBookingAssistantAgent.java` |
| Config | `Configs/RagConfig.java` |
| Tools | `Helpers/BookingChatTools.java` |
| Services | `Services/ChatHistoryService.java` |
| | `Services/KnowledgeBaseSyncService.java` |
| | `Services/RagCacheService.java` |
| DTOs | `Dto/Request/ChatbotRequest.java` |
| | `Dto/Request/MessageHistoryItem.java` |
| Constants | `Constant/PredefinedRoleChattingAI.java` |

## 10. Environment Variables

| Property (application.properties) | Env var |
|-----------------------------------|---------|
| `gemini.api.key` | `GEMINI_APIKEY` |
| `groq.api.key` | `GROQ_API_KEY` |
| `groq.base.url` | `GROQ_BASE_URL` |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` |

## 11. Error Handling

| Error | Handling |
|-------|----------|
| Rate limit (Groq, chat) | Controller bắt `OpenAiHttpException` chứa "rate_limit" → trả "Hệ thống đang bận, vui lòng thử lại sau 1 phút." |
| Rate limit (embedding sync) | RESOURCE_EXHAUSTED/429 → tự retry 3 lần, delay 50s/lần |
| API key invalid | Log error, throw exception |
| Tool execution error | Catch trong từng @Tool, trả message lỗi thân thiện tiếng Việt |

## 12. Adding New Tools

### Step 1: Add method to BookingChatTools
```java
@Tool("Mô tả tool")
public String newTool(String param) {
    // Implementation - query qua Service, trả JSON string
}
```

### Step 2: (Tùy chọn) Cập nhật @SystemMessage trong IBookingAssistantAgent
Thêm hướng dẫn chọn tool nếu cần định hướng cho LLM.

### Step 3: Sync knowledge base (nếu tool đọc data cần index)
Gọi `POST /chatbot/admin/sync-knowledge`.

## 13. Example Conversation Flow

### User: "Tour Đà Lạt giá bao nhiêu?"

```
1. Controller nhận request
         ↓
2. ChatHistoryService lấy 5 messages gần nhất
         ↓
3. IBookingAssistantAgent.chat() được gọi
         ↓
4. LLM phân tích: "Câu hỏi về tour"
         ↓
5. LLM gọi searchTours("Đà Lạt") → danh sách tour
         ↓
6. LLM gọi getTourDetails("...") → chi tiết tour
         ↓
7. LLM tổng hợp → Trả lời user bằng tiếng Việt
```

### User: "Có voucher giảm giá nào không?"

```
1. LLM phân tích: "Câu hỏi về voucher"
         ↓
2. LLM gọi getAllActiveVouchers() → danh sách voucher đang hoạt động
         ↓
3. LLM format kết quả → Trả lời user
```
