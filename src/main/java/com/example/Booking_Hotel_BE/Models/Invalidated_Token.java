package com.example.Booking_Hotel_BE.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invalidated_token")
public class Invalidated_Token {

    @Id
    @Column(name = "id_token", length = 20)
    private UUID idToken;

    @Column(name = "expiry_time")
    private Date expiryTime;
}
