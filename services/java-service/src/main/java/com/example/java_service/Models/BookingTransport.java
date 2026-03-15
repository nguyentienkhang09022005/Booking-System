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
@Table(name = "booking_transport")
public class BookingTransport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_booking_transport", nullable = false, updatable = false)
    private UUID idBookingTransport;

    @Column(name = "quantity_tickets")
    private Integer quantityTickets;

    @Column(name = "price_booking_transport", precision = 12, scale = 2)
    private BigDecimal priceBookingTransport;

    @Column(name = "seat_numbers", length = 100)
    private String seatNumbers;

    @Column(name = "status", length = 20)
    private String status;

    @ManyToOne
    @JoinColumn(name = "id_ticket", referencedColumnName = "id_ticket", foreignKey = @ForeignKey(name = "FK_B_TRANSPORT_TICKET"))
    private TransportTicket transportTicket;

    @ManyToOne
    @JoinColumn(name = "id_user", referencedColumnName = "id_user", foreignKey = @ForeignKey(name = "FK_B_USER"))
    private Users users;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}