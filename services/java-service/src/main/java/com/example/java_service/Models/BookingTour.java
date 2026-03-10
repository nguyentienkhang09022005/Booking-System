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

    @Column(name = "quantity_adult", columnDefinition = "INT DEFAULT 1")
    @Builder.Default
    private Integer quantityAdult = 1;

    @Column(name = "quantity_child", columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer quantityChild = 0;

    @Column(name = "price_booking_tour", precision = 12, scale = 2)
    private BigDecimal priceBookingTour;

    @Column(name = "pickup_location", length = 200)
    private String pickupLocation;

    @Column(name = "language_selected", length = 50)
    private String languageSelected;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    @ManyToOne
    @JoinColumn(name = "id_tour", referencedColumnName = "id_tour", foreignKey = @ForeignKey(name = "FK_B_TOUR_TOUR"))
    private Tour tour;

    @ManyToOne
    @JoinColumn(name = "id_user", referencedColumnName = "id_user", foreignKey = @ForeignKey(name = "FK_B_TOUR_USER"))
    private Users users;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}