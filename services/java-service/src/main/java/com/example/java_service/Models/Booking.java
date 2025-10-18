package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_booking", nullable = false, updatable = false)
    private UUID idBooking;

    @CreationTimestamp
    @Column(name = "booking_date", nullable = false, updatable = false)
    private LocalDateTime bookingDate;

    @Column(name = "checkin_date")
    private Date checkinDate;

    @Column(name = "checkout_date")
    private Date checkoutDate;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "total_price", precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @ManyToOne
    @JoinColumn(
            name = "id_user",
            referencedColumnName = "id_user",
            foreignKey = @ForeignKey(name = "FK_BOOKING_USER")
    )
    private Users users;

    @ManyToOne
    @JoinColumn(
            name = "id_hotel",
            referencedColumnName = "id_hotel",
            foreignKey = @ForeignKey(name = "FK_BOOKING_HOTEL")
    )
    private Hotel hotel;
}
