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
@Table(name = "tour")
public class Tour {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_tour", nullable = false, updatable = false)
    private UUID idTour;

    @ManyToOne
    @JoinColumn(name = "id_user", referencedColumnName = "id_user", foreignKey = @ForeignKey(name = "FK_TOUR_USER"))
    private Users users; // Công ty cung cấp tour

    @Column(name = "tour_name", length = 150)
    private String tourName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "price_per_person", precision = 12, scale = 2)
    private BigDecimal pricePerPerson;

    @Column(name = "status", length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}