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

### 1. ChatbotController
**Đường dẫn:** `Controllers/ChatbotController.java`
**Chức năng:** REST controller nhận request từ Node.js frontend
**Endpoints:**
- `POST /api/v1/chatbot/ask` - Giao tiếp chat
- `POST /api/v1/chatbot/admin/sync-knowledge` - Admin sync toàn bộ knowledge base
- `DELETE /api/v1/chatbot/memory/{sessionId}` - Xóa chat memory

### 2. IBookingAssistantAgent
**Đường dẫn:** `Services/Interfaces/IBookingAssistantAgent.java`
**Chức năng:** Interface cho AI Agent, được LangChain4j implement tự động
**SystemMessage:** Định nghĩa behavior và rules cho AI - hướng dẫn AI cách xử lý từng loại câu hỏi

### 3. RagConfig
**Đường dẫn:** `Configs/RagConfig.java`
**Chức năng:** Cấu hình tất cả LangChain4j beans
**Beans được tạo:**
- `ChatLanguageModel` - Gemini 2.0 Flash (suy nghĩ và trả lời)
- `EmbeddingModel` - Google text-embedding-004 (chuyển text → vector số)
- `EmbeddingStore` - PgVector connection đến PostgreSQL
- `ContentRetriever` - Tìm kiếm vector gần nhất với câu hỏi
- `ChatMemoryProvider` - Session memory (20 messages/session)

### 4. BookingChatTools
**Đường dẫn:** `Helpers/BookingChatTools.java`
**Chức năng:** Chứa các @Tool methods cho AI agent gọi - là cầu nối giữa AI và Database
**Mỗi method public với @Tool annotation = 1 Tool cho AI**

### 5. KnowledgeBaseSyncService
**Đường dẫn:** `Services/KnowledgeBaseSyncService.java`
**Chức năng:** Sync dữ liệu từ PostgreSQL sang Vector DB (RAG)
**Đồng bộ:** General Regulations, Tours, Accommodations, Services, Vouchers

### 6. ChatMemoryManager
**Đường dẫn:** `Helpers/ChatMemoryManager.java`
**Chức năng:** Quản lý chat memory (clear, count messages)

---

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

---

## Hướng dẫn thêm Tool mới

### Ví dụ: Thêm Tool kiểm tra trạng thái booking

#### Bước 1: Thêm method vào BookingChatTools

```java
@Tool("Kiểm tra trạng thái đặt phòng của khách hàng khi có booking ID hoặc email")
public String checkBookingStatus(String bookingIdentifier)
{
    try {
        BookingResponse booking = bookingService.findBooking(bookingIdentifier);
        return objectMapper.writeValueAsString(booking);
    } catch (Exception e) {
        log.error("Tool Error - checkBookingStatus: {}", e.getMessage());
        return "Không tìm thấy thông tin đặt phòng.";
    }
}
```

#### Bước 2: Thêm BookingService vào constructor

BookingChatTools đã có `@RequiredArgsConstructor` nên chỉ cần thêm field:

```java
@Component
public class BookingChatTools
{
    RoomService roomService;
    TourService tourService;
    AccommodationService accommodationService;
    VoucherService voucherService;
    TransportTicketService transportTicketService;
    ServiceService serviceService;
    BookingService bookingService;  // ← Thêm dòng này
    ObjectMapper objectMapper;
    // ...
}
```

#### Bước 3: Cập nhật SystemMessage trong IBookingAssistantAgent (nếu cần)

Thêm vào phần "--- BOOKING ---":
```
"Khi khách hỏi về trạng thái đặt phòng, lịch sử đặt:"
"1. Gọi checkBookingStatus với booking ID hoặc email."
"2. Trả lời với thông tin: ngày đặt, ngày nhận/trả phòng, trạng thái."
```

---

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

---

## Environment Variables cần thiết

```properties
GEMINI_APIKEY=your-gemini-api-key
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/database
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password
```

---

## Flow xử lý câu hỏi

### Ví dụ 1: "Tour Đà Lạt giá bao nhiêu?"

```
1. User gửi câu hỏi
         ↓
2. Controller nhận request, gọi assistantAgent.chat()
         ↓
3. AI phân tích: "Đây là câu hỏi về tour"
         ↓
4. AI gọi searchTours("Đà Lạt") → Nhận danh sách tour
         ↓
5. AI chọn tour phù hợp, gọi getTourDetails("uuid-tour")
         ↓
6. AI nhận chi tiết tour (giá, lịch trình)
         ↓
7. AI tổng hợp → Trả lời user bằng tiếng Việt
```

### Ví dụ 2: "Có voucher giảm giá nào không?"

```
1. User gửi câu hỏi
         ↓
2. AI phân tích: "Đây là câu hỏi về voucher"
         ↓
3. AI gọi searchVouchers("") → Nhận danh sách voucher đang hoạt động
         ↓
4. AI format kết quả → Trả lời user với danh sách voucher
```

### Ví dụ 3: "Khách sạn ở Hà Nội có gì?"

```
1. User gửi câu hỏi
         ↓
2. AI phân tích: "Đây là câu hỏi về nơi cư trú"
         ↓
3. AI gọi searchAccommodations("Hà Nội") → Nhận danh sách khách sạn
         ↓
4. AI format kết quả → Trả lời user với top khách sạn ở Hà Nội
```

---

## Sync Knowledge Base (Admin)

Admin gọi `POST /api/v1/chatbot/admin/sync-knowledge` để đồng bộ:

1. **General Regulations** - Quy định, chính sách khách sạn
2. **Tours** - Thông tin tour du lịch
3. **Accommodations** - Thông tin nơi cư trú
4. **Services** - Dịch vụ khách sạn
5. **Vouchers** - Mã giảm giá

Dữ liệu được:
- Ghép thành corpus lớn
- Chia nhỏ thành chunks (500 ký tự, overlap 50)
- Embed thành vectors (768 chiều)
- Lưu vào Vector DB

---

## Troubleshooting

### Lỗi "embedding dimension mismatch"
- Kiểm tra `dimension(768)` trong RagConfig khớp với model embedding

### Lỗi "Table not found"
- Chạy script `sql/rag_vector_table.sql`
- Kiểm tra `spring.datasource.url` đúng database

### Lỗi "API key invalid"
- Kiểm tra `GEMINI_APIKEY` environment variable

### Không tìm thấy kết quả RAG
- Gọi `POST /api/v1/chatbot/admin/sync-knowledge` để sync data

### AI không gọi đúng tool
- Kiểm tra @Tool description có rõ ràng không
- Kiểm tra SystemMessage trong IBookingAssistantAgent
