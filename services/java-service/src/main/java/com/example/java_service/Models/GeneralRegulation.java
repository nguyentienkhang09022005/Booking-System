package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "general_regulation")
public class GeneralRegulation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_regulation", nullable = false, updatable = false)
    private UUID idRegulation;

    @Column(name = "regulation_name", length = 100, nullable = false)
    private String regulationName;

    @Column(name = "regulation_content", columnDefinition = "TEXT", nullable = false)
    private String regulationContent;

    @ManyToOne
    @JoinColumn(name = "id_hotel",
            referencedColumnName = "id_hotel",
            foreignKey = @ForeignKey(name = "FK_REGULATION_HOTEL"))
    private Hotel hotel;
}
