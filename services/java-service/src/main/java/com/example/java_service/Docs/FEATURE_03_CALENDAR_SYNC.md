# Calendar Sync - Đồng Bộ Lịch Trình

## 1. Mục đích
Cho phép khách hàng đồng bộ lịch trình chuyến đi (booking, tour, transport) sang Google Calendar và Apple Calendar (iCal) để quản lý lịch trình tập trung.

## 2. Mô tả nghiệp vụ

### Supported Calendar Systems
- **Google Calendar** - qua Google Calendar API (OAuth2)
- **Apple Calendar (iCal)** - qua file .ics download

### Loại sự kiện đồng bộ
- `BOOKING` - Đặt phòng khách sạn
- `TOUR` - Tour du lịch
- `TRANSPORT` - Di chuyển (vé máy bay, xe, etc.)
- `ITINERARY` - Lịch trình từ itinerary

### Thông tin sự kiện
- Title: Tên accommodation/tour/transport
- Date/Time: checkin→checkout hoặc departure→arrival
- Location: Địa chỉ đầy đủ
- Description: Mã booking, thông tin liên hệ
- Color coding: Theo loại sự kiện

### Đồng bộ 2 chiều
- **Pull**: Import từ calendar bên thứ 3 (future)
- **Push**: Export booking mới sang calendar

## 3. Database Design

### PostgreSQL Table: `calendar_sync`

| Column | Type | Description |
|--------|------|-------------|
| id_sync | UUID | PK |
| id_user | UUID | FK → users |
| provider | VARCHAR(20) | GOOGLE/APPLE |
| access_token | TEXT | Encrypted OAuth token (Google) |
| refresh_token | TEXT | Encrypted refresh token |
| token_expires_at | TIMESTAMP | Token expiration |
| calendar_id | VARCHAR(100) | Target calendar ID |
| is_active | BOOLEAN | Sync active flag |
| created_at | TIMESTAMP | Ngày tạo kết nối |
| last_sync_at | TIMESTAMP | Last sync timestamp |

### PostgreSQL Table: `calendar_event_mapping`

| Column | Type | Description |
|--------|------|-------------|
| id_mapping | UUID | PK |
| id_sync | UUID | FK → calendar_sync |
| ref_type | VARCHAR(20) | BOOKING/TOUR/TRANSPORT/ITINERARY |
| ref_id | UUID | ID của booking/tour/etc |
| external_event_id | VARCHAR(100) | Event ID bên calendar provider |
| last_synced_at | TIMESTAMP | Last synced |
| status | VARCHAR(20) | ACTIVE/DELETED/ERROR |

## 4. API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/calendar/connect/google` | Kết nối Google Calendar |
| DELETE | `/calendar/disconnect` | Ngắt kết nối |
| GET | `/calendar/connections` | Danh sách kết nối |
| GET | `/calendar/events` | Danh sách sự kiện đã đồng bộ |
| POST | `/calendar/sync` | Đồng bộ manual |
| POST | `/calendar/export/{refType}/{refId}` | Export 1 booking/tour sang calendar |
| GET | `/calendar/download/{bookingId}` | Download .ics file |
| PATCH | `/calendar/event/{mappingId}` | Cập nhật event đã sync |
| DELETE | `/calendar/event/{mappingId}` | Xóa event khỏi calendar |
| GET | `/calendar/booking/{bookingId}/sync-status` | Kiểm tra sync status |

## 5. Google OAuth Flow
1. User click "Connect Google Calendar"
2. Redirect to Google OAuth consent screen
3. User approve → receive authorization code
4. Exchange code for access/refresh tokens
5. Store encrypted tokens in `calendar_sync`
6. Create/update sync records

## 6. iCal (.ics) Export
- Generate .ics file on-demand
- Include VEVENT with:
  - DTSTART/DTEND
  - SUMMARY (booking info)
  - DESCRIPTION (booking details)
  - LOCATION (address)
  - UID (unique identifier)
- User downloads and imports vào Apple Calendar

## 7. Quan hệ với Models hiện tại
- `calendar_sync.id_user` → `users.idUser`
- Events mapped từ: `booking`, `booking_tour`, `booking_transport`, `itinerary`

## 8. Dependencies
- Google Calendar API (OAuth2 client library)
- iCal4j library cho .ics generation

## 9. Database
- **PostgreSQL**: Tất cả tables (`calendar_sync`, `calendar_event_mapping`)
- Lý do: OAuth tokens cần encrypted storage, ACID cho sensitive calendar data