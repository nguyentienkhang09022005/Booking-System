package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "facility")
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_facility", nullable = false, updatable = false)
    private UUID idFacility;

    @Column(name = "facility_name", length = 200, nullable = false)
    private String facilityName;

    @Column(name = "is_free", nullable = false)
    private Boolean isFree = true;

    @ManyToOne
    @JoinColumn(name = "id_category",
            referencedColumnName = "id_category",
            foreignKey = @ForeignKey(name = "FK_FACILITY_CATEGORY"))
    private FacilityCategory category;

}
