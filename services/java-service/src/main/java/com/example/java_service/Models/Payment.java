package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_payment", nullable = false, updatable = false)
    private UUID idPayment;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "method", length = 20)
    private String method;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "id_app_trans", length = 100)
    private String idAppTrans;

    @Column(name = "id_zp_trans", length = 100)
    private String idZpTrans;

    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @CreationTimestamp
    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createAt;

    @ManyToOne
    @JoinColumn(
            name = "id_user",
            referencedColumnName = "id_user",
            foreignKey = @ForeignKey(name = "FK_PAYMENT_USER")
    )
    private Users users;

    @ManyToOne
    @JoinColumn(
            name = "id_booking",
            referencedColumnName = "id_booking",
            foreignKey = @ForeignKey(name = "FK_PAYMENT_BOOKING")
    )
    private Booking booking;

    @ManyToOne
    @JoinColumn(
            name = "id_booking_tour",
            referencedColumnName = "id_booking_tour",
            foreignKey = @ForeignKey(name = "FK_PAYMENT_BOOKING_TOUR")
    )
    private BookingTour bookingTour;

    @ManyToOne
    @JoinColumn(
            name = "id_booking_transport",
            referencedColumnName = "id_booking_transport",
            foreignKey = @ForeignKey(name = "FK_PAYMENT_BOOKING_TRANSPORT")
    )
    private BookingTransport bookingTransport;
}