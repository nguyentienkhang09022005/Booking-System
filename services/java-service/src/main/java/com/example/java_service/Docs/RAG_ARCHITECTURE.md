# RAG Chatbot Architecture Guide

## Tổng quan kiến trúc

> **LƯU Ý QUAN TRỌNG:** Hiện tại bean `ContentRetriever` (vector search) **đang bị DISABLE**
> trong `RagConfig.java` (gây tràn context với model nhỏ). Chatbot trả lời **dựa hoàn toàn vào
> Tools query trực tiếp PostgreSQL**, không dùng vector similarity search khi chat.
> Vector DB (`booking_knowledge_base`) vẫn được sync nhưng chưa được truy vấn trong luồng chat.

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
│  - Lưu USER + MODEL message, trả response về Node.js                         │
│  - Bắt OpenAiHttpException (rate_limit) của Groq                             │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                  IBookingAssistantAgent (@AiService)                         │
│  - Interface được LangChain4j implement tự động                              │
│  - @SystemMessage định nghĩa behavior + hướng dẫn chọn tool                  │
│  - LLM: Groq meta-llama/llama-4-scout-17b-16e-instruct (temperature 0.3)     │
│  - History truyền thủ công qua @V("history"), key memory = @MemoryId idUser  │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
          ┌───────────────────────┴───────────────────────┐
          │                                               │
          ▼                                               ▼
┌──────────────────────────────┐          ┌─────────────────────────────────┐
│ BookingChatTools (32 @Tool)  │          │  ChatHistory (Redis, thủ công)  │
│                              │          │                                 │
│ - Query trực tiếp PostgreSQL │          │ - ChatHistoryService            │
│   qua các Service            │          │ - Tối đa 15 message/user        │
│ - Có cache qua RagCacheService│         │ - TTL 7 ngày (604800s)          │
└──────────────┬───────────────┘          └─────────────────────────────────┘
               │
               ▼
        ┌──────────────┐
        │ PostgreSQL   │
        └──────────────┘

  (Vector DB / PgVector chỉ dùng cho sync — KHÔNG truy vấn khi chat, retriever đang tắt)
```

## Stack AI thực tế

| Thành phần | Công nghệ | Cấu hình |
|-----------|-----------|----------|
| LLM (chat) | Groq `meta-llama/llama-4-scout-17b-16e-instruct` | temperature 0.3, timeout 120s |
| Embedding | Google `gemini-embedding-001` | 768 dimensions |
| Vector Store | PgVector | table `booking_knowledge_base`, dim 768 |
| Framework | LangChain4j (`@AiService`, `@Tool`) | |
| Chat History | Redis (thủ công, không qua ChatMemoryProvider) | max 15 msg, TTL 7 ngày |

## Các file chính

| Component | Path | Mô tả |
|-----------|------|-------|
| Controller | `Controllers/ChatbotController.java` | REST controller nhận request |
| Agent Interface | `Services/Interfaces/IBookingAssistantAgent.java` | AI Agent interface (@AiService) |
| Config | `Configs/RagConfig.java` | LangChain4j beans (ChatModel, EmbeddingModel, EmbeddingStore) |
| Tools | `Helpers/BookingChatTools.java` | 32 @Tool methods cho AI gọi |
| Knowledge Sync | `Services/KnowledgeBaseSyncService.java` | Sync PostgreSQL → Vector DB |
| Chat History | `Services/ChatHistoryService.java` | Lưu trữ lịch sử chat (Redis) |
| RAG Cache | `Services/RagCacheService.java` | Cache Redis cho tool results |

## TOOLS TRONG BOOKINGCHATTOOLS (32 tools)

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

> Tool search nhận tên/UUID. Helper `searchXxxIds()` tự resolve tên → UUID; nếu trả nhiều kết quả
> sẽ báo "ambiguous" và yêu cầu user cung cấp tên cụ thể hơn. Các tool cá nhân (profile, booking,
> loyalty, favorite) lấy user từ `CurrentUserContext` (header `idUser` do gateway inject), không nhận UUID từ LLM.

## API Endpoints

Base Path: `/chatbot`

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/chatbot/ask` | Giao tiếp chat |
| POST | `/chatbot/admin/sync-knowledge` | Admin sync knowledge base |
| GET | `/chatbot/history?idUser={uuid}` | Lấy lịch sử chat |
| DELETE | `/chatbot/memory?idUser={uuid}` | Xóa chat memory |

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

## Database Schema - Vector Table

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

## Environment Variables cần thiết

| Property (application.properties) | Env var |
|-----------------------------------|---------|
| `gemini.api.key` | `GEMINI_APIKEY` |
| `groq.api.key` | `GROQ_API_KEY` |
| `groq.base.url` | `GROQ_BASE_URL` |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` |

## Flow xử lý câu hỏi

```
1. User gửi câu hỏi (POST /chatbot/ask)
         ↓
2. Controller lấy 5 message gần nhất từ Redis, gọi assistantAgent.chat()
         ↓
3. LLM (Groq) phân tích câu hỏi dựa trên @SystemMessage
         ↓
4. LLM tự chọn & gọi tool(s) phù hợp trong BookingChatTools → query PostgreSQL
         ↓
5. LLM tổng hợp kết quả → Trả lời user bằng tiếng Việt (markdown)
         ↓
6. Controller lưu USER + MODEL message vào Redis
```

## Sync Knowledge Base (Admin)

Admin gọi `POST /chatbot/admin/sync-knowledge` để đồng bộ **9 nguồn dữ liệu**:

1. General Regulations — Quy định, chính sách
2. Tours — Thông tin tour du lịch
3. Accommodations — Nơi cư trú (đã duyệt)
4. Rooms — Loại phòng và giá
5. Services — Dịch vụ khách sạn
6. Vouchers — Mã giảm giá (đã duyệt)
7. Transport Tickets — Vé phương tiện (đã duyệt)
8. Room Types — Danh sách loại phòng
9. Service Types — Danh sách loại dịch vụ

**Sync Process:**
1. Fetch data từ PostgreSQL, build corpus (StringBuilder)
2. Split chunks bằng `DocumentSplitters.recursive(500, 50)` — chunk 500 ký tự, overlap 50
3. Embed theo batch 20 segment (`embedAndStoreInBatches`), retry tối đa 3 lần, delay 50s khi rate-limit (RESOURCE_EXHAUSTED / 429)
4. Lưu vào PgVector
5. `ragCacheService.evictAllRagCache()` — xóa cache sau khi sync

## Chat Memory

History được quản lý **thủ công qua Redis** (không dùng `ChatMemoryProvider` của LangChain4j):

- `ChatHistoryService`: lưu tối đa **15 message**/user, TTL **7 ngày** (604800s)
- `ChatbotController`: lấy **5 message gần nhất** truyền vào agent qua `@V("history")`
- `@MemoryId idUser` dùng làm key memory cho LangChain4j

## Supporting Services

### ChatHistoryService.java
- `getHistoryChat(String idUser)` — Lấy lịch sử chat
- `saveChat(String idUser, MessageHistoryItem message, long ttlSeconds)` — Lưu chat
- `deleteHistoryChat(String idUser)` — Xóa lịch sử

### KnowledgeBaseSyncService.java
- `syncDataToVectorDb()` — Đồng bộ 9 nguồn dữ liệu vào Vector DB

### RagCacheService.java
- Cache Redis cho tool results (prefix `rag_cache:`, TTL mặc định 300s)
- `evictAllRagCache()`, `evictAccommodationCache()`, `evictTourCache()`, `evictVoucherCache()`

## Core Components (code thực tế)

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
    // chatModel()      - Groq OpenAiChatModel (llama-4-scout-17b), @Primary
    // embeddingModel() - GoogleAiEmbeddingModel gemini-embedding-001, 768 dim
    // embeddingStore() - PgVectorEmbeddingStore (booking_knowledge_base)
    // contentRetriever() - ĐANG COMMENT (disabled)
    // isValidUuid()    - helper validate UUID
}
```

## Troubleshooting

| Lỗi | Nguyên nhân | Xử lý |
|-----|-------------|--------|
| `embedding dimension mismatch` | Dimension không khớp | Kiểm tra `VECTOR_DIMENSION=768` trong RagConfig |
| `Table not found` | Vector table chưa tạo | Tạo table `booking_knowledge_base` |
| `API key invalid` | Sai key | Kiểm tra `GEMINI_APIKEY` / `GROQ_API_KEY` |
| Rate limit (Groq) | Vượt quota | Controller trả "Hệ thống đang bận, vui lòng thử lại sau 1 phút" |
| Rate limit (embedding sync) | RESOURCE_EXHAUSTED/429 | Tự retry 3 lần, delay 50s/lần |
| AI không gọi đúng tool | @Tool description hoặc @SystemMessage chưa rõ | Cập nhật description / SystemMessage |

## Tài liệu chi tiết

Xem [SPEC_CHATBOT_AI.md](./SPEC_CHATBOT_AI.md) để biết thêm chi tiết.
