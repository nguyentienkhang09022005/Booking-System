package com.example.Booking_Hotel_BE.Controllers;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/health")
public class HeathController
{
    @GetMapping
    public String healthCheck() {
        return "OK";
    }
}
