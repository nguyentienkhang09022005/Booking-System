package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "service")
public class Service
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_service", nullable = false, updatable = false)
    private UUID idService;

    @ManyToOne
    @JoinColumn(name = "id_hotel",
            referencedColumnName = "id_hotel",
            foreignKey = @ForeignKey(name = "FK_SERVICE_HOTEL"))
    private Hotel hotel;

    @ManyToOne
    @JoinColumn(name = "type_service",
            referencedColumnName = "type_service",
            foreignKey = @ForeignKey(name = "FK_SERVICE_TYPE_SERVICE"))
    private TypeService typeService;

    @Column(name = "service_name", length = 100, nullable = false)
    private String serviceName;

    @Column(name = "service_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal servicePrice;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
