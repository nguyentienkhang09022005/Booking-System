package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingFilterResponse<T> {
    List<T> bookings;
    List<VoucherStatisticResponse> voucherStatistics;
}