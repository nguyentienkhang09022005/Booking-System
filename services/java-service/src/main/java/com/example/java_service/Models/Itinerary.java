package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "itinerary")
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_itinerary", nullable = false, updatable = false)
    private UUID idItinerary;

    @ManyToOne
    @JoinColumn(name = "id_user", referencedColumnName = "id_user", foreignKey = @ForeignKey(name = "FK_ITINERARY_USER"))
    private Users users;

    @Column(name = "destination", length = 100)
    private String destination;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "budget", precision = 15, scale = 2)
    private BigDecimal budget;

    @Column(name = "status", length = 20)
    private String status = "DRAFT";

    @Column(name = "total_estimated_cost", precision = 15, scale = 2)
    private BigDecimal totalEstimatedCost;

    @Column(name = "currency", length = 3)
    private String currency = "VND";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}