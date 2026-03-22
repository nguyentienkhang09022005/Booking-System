package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "loyalty_transaction")
public class LoyaltyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_loyalty_trans", nullable = false, updatable = false)
    private UUID idLoyaltyTrans;

    @Column(name = "points")
    private Integer points;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "id_user", referencedColumnName = "id_user", foreignKey = @ForeignKey(name = "FK_LOYALTY_USER"))
    private Users users;

    @ManyToOne
    @JoinColumn(name = "id_booking", referencedColumnName = "id_booking", foreignKey = @ForeignKey(name = "FK_LOYALTY_BOOKING"))
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "id_booking_tour", referencedColumnName = "id_booking_tour", foreignKey = @ForeignKey(name = "FK_LOYALTY_TOUR"))
    private BookingTour bookingTour;

    @ManyToOne
    @JoinColumn(name = "id_booking_transport", referencedColumnName = "id_booking_transport", foreignKey = @ForeignKey(name = "FK_LOYALTY_TRANSPORT"))
    private BookingTransport bookingTransport;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}