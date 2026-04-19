# System Parameter Module Specification

## 1. Overview

Module System Parameter quản lý các tham số cấu hình hệ thống. Đây là nơi lưu trữ các giá trị có thể thay đổi runtime mà không cần deploy lại ứng dụng.

## 2. Entity

### SystemParameter.java

```java
@Entity
@Table(name = "system_parameter")
public class SystemParameter {
    @Id
    @Column(name = "param_key", length = 100)
    String paramKey;              // Primary Key
    
    @Column(name = "param_value", columnDefinition = "TEXT")
    String paramValue;            // Giá trị tham số
    
    @Column(name = "description", columnDefinition = "TEXT")
    String description;           // Mô tả tham số
    
    @Column(name = "updated_at")
    LocalDateTime updatedAt;       // Thời gian cập nhật cuối
}
```

## 3. Design Pattern

Sử dụng **Singleton Pattern** để truy cập system parameters:

```java
@Component
public class SystemParameterService {
    
    // Cache parameters in memory
    private Map<String, String> parameterCache = new ConcurrentHashMap<>();
    
    public String getParamValue(String key) {
        if (parameterCache.isEmpty()) {
            loadAllParameters();
        }
        return parameterCache.get(key);
    }
}
```

## 4. API Endpoints

Base Path: `/system-parameter`

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/list-system-parameter` | Lấy danh sách tất cả tham số |
| PATCH | `/update?paramKey={key}` | Cập nhật tham số |

## 5. Service Layer

### SystemParameterService.java

| Method | Mô tả |
|--------|--------|
| `getAllSystemParameters()` | Lấy tất cả tham số |
| `updateSystemParameter(String paramKey, SystemParameterUpdateRequest request)` | Cập nhật tham số |
| `getParamValue(String key)` | Lấy giá trị tham số (hỗ trợ cache) |

## 6. Repository

### SystemParameterRepository.java
```java
JpaRepository<SystemParameter, String>  // Key là paramKey
```

## 7. DTOs

### Request DTOs
- `SystemParameterUpdateRequest`
  - `paramValue` - Giá trị mới

### Response DTOs
- `SystemParameterResponse`
  - `paramKey` - Key của tham số
  - `paramValue` - Giá trị hiện tại
  - `description` - Mô tả
  - `updatedAt` - Thời gian cập nhật

## 8. Common System Parameters

### Loyalty Points
| Key | Default Value | Description |
|-----|---------------|-------------|
| `VND_PER_LOYALTY_POINT` | `1000` | 1000 VND = 1 điểm loyalty |

### Payment
| Key | Default Value | Description |
|-----|---------------|-------------|
| `PAYMENT_TIMEOUT_MINUTES` | `15` | Thời gian timeout thanh toán |
| `PAYMENT_RETRY_COUNT` | `3` | Số lần retry thanh toán |

### Booking
| Key | Default Value | Description |
|-----|---------------|-------------|
| `MAX_BOOKING_ADVANCE_DAYS` | `30` | Số ngày book trước tối đa |
| `MIN_BOOKING_ADVANCE_HOURS` | `2` | Số giờ book trước tối thiểu |

### AI Scheduler
| Key | Default Value | Description |
|-----|---------------|-------------|
| `AI_MAX_TOUR_SUGGESTIONS` | `3` | Số tour gợi ý tối đa |
| `AI_MAX_SERVICE_PER_ACCOMMODATION` | `3` | Số dịch vụ/tour tối đa |
| `AI_FUZZY_MATCH_THRESHOLD` | `0.7` | Ngưỡng fuzzy match |

## 9. Usage in Code

```java
@Service
public class BookingService {
    
    @Autowired
    SystemParameterService systemParameterService;
    
    public void processBooking() {
        String vndPerPoint = systemParameterService.getParamValue("VND_PER_LOYALTY_POINT");
        int points = totalPrice / Integer.parseInt(vndPerPoint);
    }
}
```

## 10. File Locations

| Type | Path |
|------|------|
| Entity | `Models/SystemParameter.java` |
| Repository | `Repositories/Jpa/SystemParameterRepository.java` |
| Service | `Services/SystemParameterService.java` |
| Controller | `Controllers/SystemParameterController.java` |
| Mapper | `Mappers/SystemParameterMapper.java` |

## 11. Validation Rules

| Rule | Description |
|------|-------------|
| Key exists | paramKey phải tồn tại trong database |
| Non-empty value | paramValue không được empty/null |
| Key immutable | paramKey không thể thay đổi sau khi tạo |

## 12. Caching Strategy

```
1. On application startup: Load all parameters into ConcurrentHashMap
2. On getParamValue(): Read from cache first
3. On update(): Update DB, then invalidate cache
4. Cache auto-reload: Every 1 hour or on cache miss
```
