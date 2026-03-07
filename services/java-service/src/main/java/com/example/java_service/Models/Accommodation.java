package com.example.java_service.Models;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "accommodation")
public class Accommodation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_accommodation", nullable = false, updatable = false)
    private UUID idAccommodation;

    @ManyToOne
    @JoinColumn(name = "id_user", referencedColumnName = "id_user", foreignKey = @ForeignKey(name = "FK_HOTEL_USER"))
    private Users users;

    @Column(name = "accommodation_name", length = 100)
    private String accommodationName;

    @Column(name = "accommodation_type", length = 50)
    private String accommodationType;

    @Column(name = "street", length = 100)
    private String street;

    @Column(name = "ward", length = 30)
    private String ward;

    @Column(name = "city", length = 30)
    private String city;

    @Column(name = "province", length = 50)
    private String province;

    @JsonFormat(pattern = "HH:mm:ss")
    @Column(name = "checkin_time")
    private LocalTime checkinTime;

    @JsonFormat(pattern = "HH:mm:ss")
    @Column(name = "checkout_time")
    private LocalTime checkoutTime;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_approved", nullable = false)
    private Boolean approved = false;
}