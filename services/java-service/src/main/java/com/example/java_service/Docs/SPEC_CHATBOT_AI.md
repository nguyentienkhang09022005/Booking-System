# Chatbot AI Module Specification

## 1. Overview

Chatbot AI là trợ lý ảo sử dụng RAG (Retrieval Augmented Generation) để trả lời câu hỏi về dịch vụ du lịch trong hệ thống BookingNext. Chatbot sử dụng LangChain4j với Gemini 2.0 Flash làm LLM.

## 2. Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Frontend (Node.js)                                │
│                         WebSocket / REST Client                              │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │ HTTP POST /api/v1/chatbot/ask
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ChatbotController                                    │
│  - Nhận request từ Node.js                                                  │
│  - Gọi IBookingAssistantAgent.chat()                                         │
│  - Trả response về cho Node.js                                              │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      IBookingAssistantAgent (@AiService)                     │
│  - Interface được LangChain4j implement tự động                              │
│  - Sử dụng SystemMessage để định nghĩa behavior AI                         │
│  - Tự động inject Tools & ChatMemory                                        │
│  - RAG Retriever được cấu hình để tìm kiếm Vector DB                       │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          │                       │                       │
          ▼                       ▼                       ▼
┌──────────────────┐  ┌────────────────────┐  ┌─────────────────────┐
│ BookingChatTools │  │  Vector DB (RAG)   │  │   ChatMemory        │
│                  │  │                    │  │                     │
│ 8 Tools:         │  │ - PgVector store   │  │ - Per-session       │
│ - searchTours   │  │ - Embedding search │  │   20 messages       │
│ - getTourDetails │  │ - 768 dim vectors  │  │ - MemoryWindow      │
│ - searchAccom... │  └────────────────────┘  └─────────────────────┘
│ - getAccom...    │
│ - checkRoom...   │
│ - searchVoucher  │
│ - searchTrans... │
│ - searchService  │
└──────────────────┘
```

## 3. API Endpoints

Base Path: `/chatbot`

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/ask` | Giao tiếp với chatbot |
| POST | `/admin/sync-knowledge` | Admin đồng bộ knowledge base |
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
    String chat(String userId, String userMessage, List<MessageHistoryItem> history);
}
```

### RagConfig.java
```java
@Configuration
public class RagConfig {
    // ChatLanguageModel - Gemini 2.0 Flash
    // EmbeddingModel - text-embedding-004 (768 dimensions)
    // EmbeddingStore - PgVector
    // ContentRetriever - Vector similarity search
    // ChatMemoryProvider - 20 messages per session
}
```

### BookingChatTools.java

| Tool | Mô tả | Khi nào AI gọi |
|------|-------|----------------|
| `searchTours` | Tìm tour theo tên/địa điểm | Hỏi về tour du lịch |
| `getTourDetails` | Lấy chi tiết tour (giá, lịch trình) | Cần thông tin chi tiết tour |
| `searchAccommodations` | Tìm nơi cư trú theo tên/thành phố | Hỏi về khách sạn |
| `getAccommodationDetails` | Lấy chi tiết nơi cư trú | Cần thông tin chi tiết khách sạn |
| `checkRoomAvailability` | Kiểm tra phòng trống, giá | Hỏi về phòng cụ thể |
| `searchVouchers` | Tìm voucher theo tên/mã | Hỏi về khuyến mãi |
| `searchTransportTickets` | Tìm vé di chuyển | Hỏi về vé phương tiện |
| `searchServices` | Tìm dịch vụ | Hỏi về dịch vụ |

## 5. Knowledge Base Sync

Admin gọi `POST /api/v1/chatbot/admin/sync-knowledge` để đồng bộ:

| Data | Description |
|------|-------------|
| General Regulations | Quy định, chính sách |
| Tours | Thông tin tour du lịch |
| Accommodations | Thông tin nơi cư trú |
| Services | Dịch vụ khách sạn |
| Vouchers | Mã giảm giá |

**Sync Process:**
1. Fetch data từ PostgreSQL
2. Combine into corpus
3. Split into chunks (500 chars, 50 overlap)
4. Generate embeddings (768 dimensions)
5. Store in PgVector

## 6. Chat Memory

```java
// Memory configuration
ChatMemoryProvider:
  - 20 messages per session
  - MemoryWindow.chatMemory(20)
  - Keyed by sessionId (userId)
```

## 7. Supporting Services

### ChatHistoryService.java
- `getHistoryChat(String idUser)` - Lấy lịch sử chat
- `saveChat(String idUser, MessageHistoryItem message, Long durationSeconds)` - Lưu chat

### KnowledgeBaseSyncService.java
- `syncDataToVectorDb()` - Đồng bộ knowledge base

### RagCacheService.java
- Cache cho RAG queries

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

```properties
GEMINI_APIKEY=your-gemini-api-key
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/database
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password
```

## 11. Error Handling

| Error | Handling |
|-------|----------|
| Rate limit exceeded | Return message: "Hệ thống đang bận, vui lòng thử lại sau 1 phút" |
| API key invalid | Log error, throw exception |
| Vector DB not found | Return error, suggest running sync-knowledge |

## 12. Adding New Tools

### Step 1: Add method to BookingChatTools

```java
@Tool("Mô tả tool")
public String newTool(String param) {
    // Implementation
}
```

### Step 2: Update SystemMessage in IBookingAssistantAgent

Add tool usage instructions in the system prompt.

### Step 3: Sync knowledge base

Call `POST /chatbot/admin/sync-knowledge` to reload.

## 13. Example Conversation Flow

### User: "Tour Đà Lạt giá bao nhiêu?"

```
1. Controller nhận request
         ↓
2. ChatHistoryService lấy 5 messages gần nhất
         ↓
3. IBookingAssistantAgent.chat() được gọi
         ↓
4. AI phân tích: "Câu hỏi về tour"
         ↓
5. AI gọi searchTours("Đà Lạt") → Tool trả về danh sách tour
         ↓
6. AI gọi getTourDetails("uuid-tour") → Chi tiết tour
         ↓
7. AI tổng hợp → Trả lời user bằng tiếng Việt
```

### User: "Có voucher giảm giá nào không?"

```
1. AI phân tích: "Câu hỏi về voucher"
         ↓
2. AI gọi searchVouchers("") → Danh sách voucher đang active
         ↓
3. AI format kết quả → Trả lời user
```
