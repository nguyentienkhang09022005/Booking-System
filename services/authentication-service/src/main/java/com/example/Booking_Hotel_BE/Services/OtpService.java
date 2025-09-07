package com.example.Booking_Hotel_BE.Services;

import com.example.Booking_Hotel_BE.Dto.MainBody;
import com.example.Booking_Hotel_BE.Dto.Request.ChangePasswordRequest;
import com.example.Booking_Hotel_BE.Dto.Request.OtpRequest;
import com.example.Booking_Hotel_BE.Exception.ApiException;
import com.example.Booking_Hotel_BE.Exception.ErrorCode;
import com.example.Booking_Hotel_BE.Models.OTP;
import com.example.Booking_Hotel_BE.Models.Users;
import com.example.Booking_Hotel_BE.Repositories.OtpRepository;
import com.example.Booking_Hotel_BE.Repositories.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpService
{
    UserRepository userRepository;
    OtpRepository otpRepository;
    EmailService emailService;
    PasswordEncoder passwordEncoder;

    // Hàm tạo OTP
    public Integer otpGenerator()
    {
        Random random = new Random();
        return random.nextInt(100000,1000000);
    }

    // Hàm xác thực OTP
    public ResponseEntity<String> verifyOtp(OtpRequest request)
    {
        var user = userRepository.findByEmail(request.getEmail()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );
        OTP checkOtp = otpRepository.findByOtpAndUser(request.getOtp(), user).orElseThrow(
                () -> new ApiException(ErrorCode.OTP_NOT_EXIST_FOR_EMAIL)
        );

        if (checkOtp.getExpiryTime().isBefore(LocalDateTime.now()))
        {
            otpRepository.deleteById(checkOtp.getOtp());
            return new ResponseEntity<>("OTP not exist for email", HttpStatus.EXPECTATION_FAILED);
        }
        return ResponseEntity.ok("OTP verified!");
    }

    // Hàm xác thực email để tạo OTP
    public void verifyEmail(String email)
    {
        var user = userRepository.findByEmail(email).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );
        int otp = otpGenerator();
        MainBody mainBody = MainBody.builder()
                .to(email)
                .subject("OTP for forgot password request")
                .text("Mã OTP của bạn là: " + otp)
                .build();

        OTP saveOtp = OTP.builder()
                .otp(otp)
                .user(user)
                .expiryTime(LocalDateTime.now().plusMinutes(10))// OTP có hiệu lực trong 2 phút
                .build();

        emailService.sendOtpEmail(mainBody);
        otpRepository.save(saveOtp);
    }

    // Hàm thay đổi mật khẩu
    public ResponseEntity<String> changePassword (ChangePasswordRequest request)
    {
        Users user = userRepository.findByEmail(request.getEmail()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        if (!request.getNewPassword().equals(request.getConfirmPassword()))
        {
            return new ResponseEntity<>("Enter the password again", HttpStatus.EXPECTATION_FAILED);
        }

        String newPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordUser(newPassword);
        userRepository.save(user);
        return ResponseEntity.ok("Password changed!");
    }
}

