package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "facility_category")
public class FacilityCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_category", nullable = false, updatable = false)
    private UUID idCategory;

    @Column(name = "category_name", length = 100, nullable = false)
    private String categoryName;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Facility> facilities;
}
