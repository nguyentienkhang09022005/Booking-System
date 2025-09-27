package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "booking_service")
public class BookingService {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_booking_service", nullable = false, updatable = false)
    private UUID idBookingService;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "price_booking_service", precision = 12, scale = 2)
    private BigDecimal priceBookingService;

    @ManyToOne
    @JoinColumn(
            name = "id_booking",
            referencedColumnName = "id_booking",
            foreignKey = @ForeignKey(name = "FK_BOOKING_SERVICE_BOOKING")
    )
    private Booking booking;

    @ManyToOne
    @JoinColumn(
            name = "id_service",
            referencedColumnName = "id_service",
            foreignKey = @ForeignKey(name = "FK_BOOKING_SERVICE_SERVICE")
    )
    private Service service;
}
