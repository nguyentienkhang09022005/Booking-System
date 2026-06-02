# 08 — Chatbot AI thế hệ mới (RAG + Tools)

> **Nâng cấp lớn so với Đồ án 1.** Đồ án 1 dùng chat AI cơ bản (Gemini gọi qua Node.js, tư vấn + kiểm duyệt review). Đồ án 2 xây **chatbot RAG** ngay trong java-service với LangChain4j: truy vấn dữ liệu thật qua bộ **tools**, có **bộ nhớ hội thoại**, nền tảng vector cho tri thức.

## 1. Kiến trúc (thực tế trong code)
| Thành phần | Công nghệ |
|------------|-----------|
| LLM hội thoại | **Groq `meta-llama/llama-4-scout-17b-16e-instruct`** (`@Primary ChatLanguageModel`) |
| Embedding | **Gemini `gemini-embedding-001`** (768 chiều) |
| Vector store | **PgVector** (bảng `booking_knowledge_base`) |
| Agent | LangChain4j `@AiService` (`IBookingAssistantAgent`) với `@SystemMessage` |
| Bộ nhớ | **Redis** quản lý thủ công (tối đa 15 msg, TTL 7 ngày, truyền 5 msg gần nhất qua `@V("history")`) |

> Lưu ý: `ContentRetriever` (vector search trong hội thoại) hiện **đang tắt** — chatbot trả lời chủ yếu bằng cách gọi **tools** truy vấn Postgres trực tiếp. PgVector + đồng bộ tri thức vẫn được duy trì để mở rộng.

## 2. Bộ công cụ (tools) — `Helpers/BookingChatTools.java` (32 `@Tool`)
Nhóm tool truy vấn nghiệp vụ cho LLM gọi đúng nhu cầu:
- **Tour** (4), **Accommodation** (4), **Room** (2), **Service** (3), **Voucher** (3), **Transport** (4).
- **Cá nhân** (lấy user từ `CurrentUserContext`, gateway inject `idUser`): hồ sơ, lịch sử điểm thưởng, booking tour/vé, booking detail.
- **Yêu thích & Đánh giá** (3), **So sánh giá / gợi ý** (3).
- Mỗi tool có mô tả "Dùng khi…", ưu tiên `*BasicInfo` (nhẹ) trước `*Details` (nặng), giới hạn số bản ghi để tránh tràn context.

## 3. API — base path `/chatbot`
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/ask` | Hỏi chatbot (`userMessage`); user lấy từ header `idUser` (gateway), fallback body cho dev |
| DELETE | `/memory?idUser=` | Xóa bộ nhớ hội thoại của user |
| POST | `/admin/sync-knowledge` | Đồng bộ tri thức vào PgVector (admin) |

## 4. Đồng bộ tri thức (RAG corpus)
`KnowledgeBaseSyncService` gom dữ liệu từ nhiều nguồn (khách sạn, phòng, dịch vụ, tour, vé, voucher, loại dịch vụ, đánh giá...), sinh embedding Gemini và lưu vào PgVector để truy hồi ngữ nghĩa khi cần.

## 5. Vị trí mã nguồn
`Controllers/ChatbotController.java`; `Configs/RagConfig.java`; `Services/Interfaces/IBookingAssistantAgent.java`; `Helpers/BookingChatTools.java`, `CurrentUserContext.java`; `Services/KnowledgeBaseSyncService.java`, `ChatHistoryService.java`, `RagCacheService.java`. Tài liệu kỹ thuật: `Docs/RAG_ARCHITECTURE.md`, `Docs/SPEC_CHATBOT_AI.md`.

> Bảo mật: thông tin cá nhân (hồ sơ, booking, điểm thưởng) lấy theo `idUser` đã xác thực bởi gateway, không nhận UUID tùy ý từ LLM.
