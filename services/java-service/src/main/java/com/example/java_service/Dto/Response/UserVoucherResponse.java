package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserVoucherResponse
{
    Boolean isUsed;
    LocalDateTime usedAt;
    LocalDateTime savedAt;
    VoucherViewResponse voucherViewResponse;
}
