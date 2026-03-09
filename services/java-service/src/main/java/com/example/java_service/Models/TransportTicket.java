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
@Table(name = "transport_ticket")
public class TransportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_ticket", nullable = false, updatable = false)
    private UUID idTicket;

    @ManyToOne
    @JoinColumn(name = "id_user", referencedColumnName = "id_user", foreignKey = @ForeignKey(name = "FK_TRANSPORT_TICKET_USER"))
    private Users users; // Công ty cung cấp vé di chuyển

    @Column(name = "ticket_type", length = 50)
    private String ticketType;

    @Column(name = "ticket_class", length = 50)
    private String ticketClass;

    @Column(name = "provider_name", length = 100)
    private String providerName;

    @Column(name = "departure_location", length = 100)
    private String departureLocation;

    @Column(name = "arrival_location", length = 100)
    private String arrivalLocation;

    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "is_approved", nullable = false)
    private Boolean approved = false;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}