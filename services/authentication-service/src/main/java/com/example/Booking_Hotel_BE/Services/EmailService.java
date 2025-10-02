package com.example.Booking_Hotel_BE.Services;

import com.example.Booking_Hotel_BE.Dto.MainBody;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class EmailService {

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email}")
    private String fromEmail;

    public void sendOtpEmail(MainBody mainBody) {
        Email from = new Email(fromEmail);
        Email to = new Email(mainBody.to());
        Content content = new Content("text/plain", mainBody.text());
        Mail mail = new Mail(from, mainBody.subject(), to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            log.info("SendGrid response status code: {}", response.getStatusCode());
        } catch (IOException ex) {
            log.error("Error sending email with SendGrid", ex);
        }
    }
}