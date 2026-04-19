# RAG Chatbot Architecture Guide

## Tổng quan kiến trúc

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

## Các file chính

| Component | Path | Mô tả |
|-----------|------|-------|
| Controller | `Controllers/ChatbotController.java` | REST controller nhận request |
| Agent Interface | `Services/Interfaces/IBookingAssistantAgent.java` | AI Agent interface |
| Config | `Configs/RagConfig.java` | LangChain4j beans configuration |
| Tools | `Helpers/BookingChatTools.java` | 8 @Tool methods cho AI gọi |
| Knowledge Sync | `Services/KnowledgeBaseSyncService.java` | Sync PostgreSQL → Vector DB |
| Chat History | `Services/ChatHistoryService.java` | Lưu trữ lịch sử chat |

## TOOLS TRONG BOOKINGCHATTOOLS

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

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/v1/chatbot/ask` | Giao tiếp chat |
| POST | `/api/v1/chatbot/admin/sync-knowledge` | Admin sync knowledge base |
| DELETE | `/api/v1/chatbot/memory/{sessionId}` | Xóa chat memory |

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

```properties
GEMINI_APIKEY=your-gemini-api-key
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/database
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password
```

## Flow xử lý câu hỏi

```
1. User gửi câu hỏi
         ↓
2. Controller nhận request, gọi assistantAgent.chat()
         ↓
3. AI phân tích: "Đây là câu hỏi về..."
         ↓
4. AI gọi tool(s) phù hợp → Nhận kết quả từ DB
         ↓
5. AI tổng hợp → Trả lời user bằng tiếng Việt
```

## Sync Knowledge Base (Admin)

Admin gọi `POST /api/v1/chatbot/admin/sync-knowledge` để đồng bộ:

1. **General Regulations** - Quy định, chính sách
2. **Tours** - Thông tin tour du lịch
3. **Accommodations** - Thông tin nơi cư trú
4. **Services** - Dịch vụ khách sạn
5. **Vouchers** - Mã giảm giá

## Troubleshooting

| Lỗi | Nguyên nhân | Xử lý |
|-----|-------------|--------|
| `embedding dimension mismatch` | Dimension không khớp | Kiểm tra `dimension(768)` trong RagConfig |
| `Table not found` | Vector table chưa tạo | Chạy script `sql/rag_vector_table.sql` |
| `API key invalid` | GEMINI_APIKEY sai | Kiểm tra environment variable |
| Không tìm thấy kết quả RAG | Data chưa sync | Gọi sync-knowledge endpoint |
| AI không gọi đúng tool | @Tool description không rõ | Cập nhật SystemMessage |

## Tài liệu chi tiết

Xem [SPEC_CHATBOT_AI.md](./SPEC_CHATBOT_AI.md) để biết thêm chi tiết về:
- Chi tiết từng component
- Cách thêm tool mới
- Cấu hình ChatMemory
- Example conversation flows
- Error handling
