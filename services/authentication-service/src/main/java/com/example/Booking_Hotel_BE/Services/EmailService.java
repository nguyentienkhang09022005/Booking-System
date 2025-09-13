package com.example.Booking_Hotel_BE.Services;

import com.example.Booking_Hotel_BE.Dto.MainBody;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailService
{
    JavaMailSender mailSender;

    public void sendOtpEmail(MainBody mainBody) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(mainBody.to());
        message.setFrom("nguyentienkhang09022005@gmail.com");
        message.setSubject(mainBody.subject());
        message.setText(mainBody.text());
        mailSender.send(message);
    }
}

