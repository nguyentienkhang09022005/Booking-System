package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "meeting_point", length = 200)
    private String meetingPoint;

    @Column(name = "drop_off_point", length = 200)
    private String dropOffPoint;

    @Column(name = "cancellation_policy", columnDefinition = "TEXT")
    private String cancellationPolicy;

    @Column(name = "languages", length = 100)
    private String languages;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "inclusions", columnDefinition = "jsonb")
    private List<String> inclusions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "exclusions", columnDefinition = "jsonb")
    private List<String> exclusions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_info", columnDefinition = "jsonb")
    private List<String> additionalInfo;

    @Column(name = "adult_price", precision = 12, scale = 2)
    private BigDecimal adultPrice;

    @Column(name = "child_price", precision = 12, scale = 2)
    private BigDecimal childPrice;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "is_approved", nullable = false)
    private Boolean approved = false;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TourSchedule> tourSchedules;
}