package com.example.java_service.Dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResponse
{
    @JsonProperty("id_payment")
    UUID idPayment;

    @JsonProperty("amount")
    BigDecimal amount;

    @JsonProperty("method")
    String method;

    @JsonProperty("status")
    String status;

    @JsonProperty("order_url")
    String orderUrl;

    // VNDStablecoin fields — null for ZaloPay payments
    @JsonProperty("typed_data")
    Map<String, Object> typedData;

    @JsonProperty("order_id")
    String orderId;
}
