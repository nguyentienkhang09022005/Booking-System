package com.example.Booking_Hotel_BE.Dto;

import lombok.Builder;

@Builder
public record MainBody(String to, String subject, String text)
{
}
