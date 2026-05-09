package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.PaymentSuccessNotification;
import com.example.java_service.Services.CompanyBalanceService;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentInternalController {

    CompanyBalanceService companyBalanceService;

    @PostMapping("/payment-success")
    public ResponseEntity<Void> handlePaymentSuccess(@RequestBody PaymentSuccessNotification notification) {
        companyBalanceService.addEarnings(notification);
        return ResponseEntity.ok().build();
    }
}