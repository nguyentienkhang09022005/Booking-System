package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "itinerary_item")
public class ItineraryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_item", nullable = false, updatable = false)
    private UUID idItem;

    @ManyToOne
    @JoinColumn(name = "id_itinerary", referencedColumnName = "id_itinerary", foreignKey = @ForeignKey(name = "FK_ITEM_ITINERARY"))
    private Itinerary itinerary;

    @Column(name = "item_type", length = 30)
    private String itemType;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "ref_id")
    private UUID refId;

    @Column(name = "suggested_name", length = 200)
    private String suggestedName;

    @Column(name = "suggested_location", length = 200)
    private String suggestedLocation;

    @Column(name = "suggested_price", precision = 15, scale = 2)
    private BigDecimal suggestedPrice;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "booking_status", length = 20)
    private String bookingStatus = "PENDING";

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "payment_status", length = 20)
    private String paymentStatus = "UNPAID";

    @Column(name = "display_order")
    private Integer displayOrder;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}