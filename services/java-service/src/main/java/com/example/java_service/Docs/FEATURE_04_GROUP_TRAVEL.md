# Group Travel - Du Lịch Nhóm & Chia Tiền

## 1. Mục đích
Cho phép chủ booking mời bạn bè/thành viên nhóm cùng tham gia chuyến đi và hỗ trợ chia tiền (split payment) một cách công bằng và minh bạch.

## 2. Mô tả nghiệp vụ

### Luồng hoạt động
1. Chủ booking tạo "Group Trip" từ 1 booking có sẵn
2. Mời thành viên qua email/phone
3. Thành viên join vào group
4. Chủ group phân chia chi phí (auto hoặc manual)
5. Thành viên thanh toán phần của mình

### Roles trong Group
- `ORGANIZER` - Chủ booking, người tạo group
- `MEMBER` - Thành viên được mời

### Split Types
- `EQUAL` - Chia đều cho tất cả
- `BY_PERSON` - Mỗi người tự chọn phần
- `BY_ROOM` - Chia theo phòng
- `CUSTOM` - Tùy chỉnh

### Trạng thái thanh toán
- `PENDING` - Chờ thanh toán
- `PARTIAL` - Thanh toán một phần
- `PAID` - Đã thanh toán đủ
- `EXEMPTED` - Được miễn (organizer exempt)

### Notifications
- Khi được mời: email/notification
- Khi có thay đổi phân chia
- Khi có member thanh toán
- Reminder thanh toán chưa hoàn tất

## 3. Database Design

### PostgreSQL Table: `group_trip`

| Column | Type | Description |
|--------|------|-------------|
| id_group | UUID | PK |
| id_organizer | UUID | FK → users (chủ group) |
| ref_type | VARCHAR(20) | BOOKING/TOUR/TRANSPORT |
| ref_id | UUID | Booking/Tour/Transport reference |
| title | VARCHAR(200) | Tên nhóm chuyến đi |
| description | TEXT | Mô tả |
| total_amount | DECIMAL(15,2) | Tổng chi phí |
| currency | VARCHAR(3) | VND/USD |
| split_type | VARCHAR(20) | EQUAL/BY_PERSON/BY_ROOM/CUSTOM |
| status | VARCHAR(20) | ACTIVE/COMPLETED/CANCELLED |
| expires_at | TIMESTAMP | Hạn chót thanh toán |
| created_at | TIMESTAMP | Ngày tạo |
| updated_at | TIMESTAMP | Ngày cập nhật |

### PostgreSQL Table: `group_member`

| Column | Type | Description |
|--------|------|-------------|
| id_member | UUID | PK |
| id_group | UUID | FK → group_trip |
| id_user | UUID | FK → users (null nếu chưa join) |
| email | VARCHAR(100) | Email mời (null nếu đã join) |
| phone | VARCHAR(20) | Phone mời |
| name | VARCHAR(100) | Tên hiển thị khi mời |
| role | VARCHAR(20) | ORGANIZER/MEMBER |
| invited_at | TIMESTAMP | Ngày mời |
| joined_at | TIMESTAMP | Ngày tham gia |
| status | VARCHAR(20) | INVITED/JOINED/DECLINED |

### PostgreSQL Table: `split_share`

| Column | Type | Description |
|--------|------|-------------|
| id_split | UUID | PK |
| id_group | UUID | FK → group_trip |
| id_member | UUID | FK → group_member |
| amount | DECIMAL(15,2) | Số tiền phải trả |
| amount_paid | DECIMAL(15,2) | Số tiền đã trả |
| status | VARCHAR(20) | PENDING/PARTIAL/PAID/EXEMPTED |
| payment_due_date | TIMESTAMP | Hạn thanh toán |
| notes | TEXT | Ghi chú (VD: "Phòng 1") |
| created_at | TIMESTAMP | Ngày tạo |
| updated_at | TIMESTAMP | Ngày cập nhật |

### PostgreSQL Table: `payment_share`

| Column | Type | Description |
|--------|------|-------------|
| id_payment_share | UUID | PK |
| id_split | UUID | FK → split_share |
| id_payment | UUID | FK → payment |
| amount | DECIMAL(15,2) | Số tiền thanh toán |
| paid_at | TIMESTAMP | Ngày thanh toán |

## 4. API Endpoints

### Group Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/group-trip` | Tạo group trip từ booking |
| GET | `/group-trip/{id}` | Chi tiết group |
| PATCH | `/group-trip/{id}` | Cập nhật group |
| DELETE | `/group-trip/{id}` | Xóa group |
| GET | `/group-trip/my-groups` | Group tôi tham gia |
| POST | `/group-trip/{id}/invite` | Mời thành viên |
| POST | `/group-trip/join/{token}` | Join group qua token |
| DELETE | `/group-trip/{id}/member/{memberId}` | Xóa thành viên |

### Split & Payment
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/group-trip/{id}/split` | Tạo phân chia |
| PATCH | `/group-trip/{id}/split` | Cập nhật phân chia |
| GET | `/group-trip/{id}/splits` | Xem phân chia |
| POST | `/group-trip/{id}/split/{splitId}/pay` | Thanh toán phần của mình |
| GET | `/group-trip/{id}/payment-summary` | Tổng kết thanh toán |

### Calculations
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/group-trip/{id}/calculate-equal` | Tính chia đều |
| POST | `/group-trip/{id}/recalculate` | Tính lại |

## 5. Flow: Thanh toán Split Payment
1. Member click "Pay my share"
2. Tạo Payment với `split_share` reference
3. Redirect to payment gateway (Go service)
4. Callback update `payment_share` + `split_share`
5. Nếu all paid → trigger booking confirmation

## 6. Quan hệ với Models hiện tại
- `group_trip.ref_id` → `booking.idBooking` / `booking_tour.idBookingTour` / `booking_transport.idBookingTransport`
- `group_trip.id_organizer` → `users.idUser`
- `split_share.amount` derived from `booking.total_price`
- `payment_share` → `payment.idPayment`

## 7. Security
- Chỉ organizer mới có quyền: tạo group, mời, phân chia, xóa
- Member chỉ xem và thanh toán phần mình
- Invitation token có expiry

## 8. Event tích hợp
- Thành viên mới join → notification cho organizer
- Member thanh toán xong → notification cho organizer

## 9. Database
- **PostgreSQL**: Tất cả tables (`group_trip`, `group_member`, `split_share`, `payment_share`)
- Lý do: Financial calculations cần ACID, split payment logic phức tạp với multiple FK relationships