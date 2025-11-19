package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "incident_report")
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_incident", nullable = false, updatable = false)
    private UUID idIncident;

    @Column(name = "incident_name", length = 100)
    private String incidentName;

    @Column(name = "incident_description", columnDefinition = "TEXT")
    private String incidentDescription;

    @Column(name = "severity", length = 10)
    private String severity;

    @Column(name = "status", length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "create_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createAt;

    @ManyToOne
    @JoinColumn(
            name = "id_user",
            referencedColumnName = "id_user",
            foreignKey = @ForeignKey(name = "FK_INCIDENT_USER")
    )
    private Users user;

    @ManyToOne
    @JoinColumn(
            name = "id_booking",
            referencedColumnName = "id_booking",
            foreignKey = @ForeignKey(name = "FK_INCIDENT_BOOKING")
    )
    private Booking booking;
}