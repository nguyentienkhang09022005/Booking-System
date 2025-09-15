package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "type_service")
public class TypeService
{
    @Id
    @Column(name = "type_service", nullable = false, updatable = false)
    private String typeService;

    @Column(name = "description",columnDefinition = "TEXT")
    private String description;
}
