package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tour_schedule")
public class TourSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_schedule", nullable = false, updatable = false)
    private UUID idSchedule;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @Column(name = "arrival_location", length = 100)
    private String arrivalLocation;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "id_tour", referencedColumnName = "id_tour", foreignKey = @ForeignKey(name = "FK_SCHEDULE_TOUR"))
    private Tour tour;
}