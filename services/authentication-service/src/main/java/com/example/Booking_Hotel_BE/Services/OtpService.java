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

    // Hàm xác thực OTP cho quên mật khẩu
    public ResponseEntity<String> verifyForgotPasswordOtp(OtpRequest request)
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
            return new ResponseEntity<>("OTP không tồn tại cho email", HttpStatus.EXPECTATION_FAILED);
        }
        return ResponseEntity.ok("OTP đã được xác thực!");
    }

    // Hàm xác thực OTP cho đăng ký
    public ResponseEntity<String> verifyRegisterOtp(OtpRequest request) {
        OTP checkOtp = otpRepository.findByOtp(request.getOtp())
                .orElseThrow(() -> new ApiException(ErrorCode.OTP_NOT_FOUND));

        if (checkOtp.getExpiryTime().isBefore(LocalDateTime.now())) {
            otpRepository.delete(checkOtp);
            return new ResponseEntity<>("OTP đã hết hạn!", HttpStatus.EXPECTATION_FAILED);
        }

        return ResponseEntity.ok("OTP đã được xác thực!");
    }

    // Hàm tạo OTP cho quên mật khẩu
    public void sendForgotPasswordOtp(String email)
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

    public void sendRegisterOtp(String email)
    {
        int otp = otpGenerator();
        MainBody mainBody = MainBody.builder()
                .to(email)
                .subject("OTP for forgot password request")
                .text("Mã OTP của bạn là: " + otp)
                .build();

        OTP saveOtp = OTP.builder()
                .otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(10))// OTP có hiệu lực trong 2 phút
                .build();

        emailService.sendOtpEmail(mainBody);
        otpRepository.save(saveOtp);
    }

    // Hàm thay đổi mật khẩu
    public ResponseEntity<String> changePassword(ChangePasswordRequest request)
    {
        // Verify OTP
        ResponseEntity<String> verifyResult = verifyForgotPasswordOtp(
                new OtpRequest(request.getOtp(), request.getEmail())
        );
        if (!verifyResult.getStatusCode().is2xxSuccessful()) {
            return verifyResult;
        }

        Users user = userRepository.findByEmail(request.getEmail()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        if (!request.getNewPassword().equals(request.getConfirmPassword()))
        {
            return new ResponseEntity<>("Hãy nhập lại mật khẩu của bạn cho khớp", HttpStatus.EXPECTATION_FAILED);
        }

        String newPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordUser(newPassword);
        userRepository.save(user);
        return ResponseEntity.ok("Thay đổi mật khẩu thành công!");
    }
}

