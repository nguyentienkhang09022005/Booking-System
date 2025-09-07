package com.example.Booking_Hotel_BE.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "otp")
public class OTP {

    @Id
    @Column(name = "otp")
    private int otp;

    @ManyToOne
    @JoinColumn(name = "id_user",
            referencedColumnName = "id_user",
            foreignKey = @ForeignKey(name = "FK_OTP_USER")
    )
    private Users user;

    @Column(name = "expiry_time")
    private LocalDateTime expiryTime;

}
