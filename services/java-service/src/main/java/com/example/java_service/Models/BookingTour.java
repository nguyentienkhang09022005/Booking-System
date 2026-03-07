package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "booking_tour")
public class BookingTour {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_booking_tour", nullable = false, updatable = false)
    private UUID idBookingTour;

    @Column(name = "quantity_people")
    private Integer quantityPeople;

    @Column(name = "price_booking_tour", precision = 12, scale = 2)
    private BigDecimal priceBookingTour;

    @ManyToOne
    @JoinColumn(name = "id_booking", referencedColumnName = "id_booking", foreignKey = @ForeignKey(name = "FK_B_TOUR_BOOKING"))
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "id_tour", referencedColumnName = "id_tour", foreignKey = @ForeignKey(name = "FK_B_TOUR_TOUR"))
    private Tour tour;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}