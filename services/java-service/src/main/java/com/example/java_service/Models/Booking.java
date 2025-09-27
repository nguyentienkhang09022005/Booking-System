package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
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

    @Column(name = "booking_date", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp bookingDate;

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
}
