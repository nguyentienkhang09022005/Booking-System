package com.example.Booking_Hotel_BE.Controllers;

import com.example.Booking_Hotel_BE.Config.CustomOAuth2User;
import com.example.Booking_Hotel_BE.Dto.Response.AuthenResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/google")
public class GoogleAuthenController
{
    @GetMapping("/login")
    public ResponseEntity<AuthenResponse> googleLogin(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    )
    {
        if (customOAuth2User == null) {
            return ResponseEntity.badRequest().build();
        }

        AuthenResponse authResponse = customOAuth2User.getAuthResponse();
        return ResponseEntity.ok(authResponse);
    }
}
