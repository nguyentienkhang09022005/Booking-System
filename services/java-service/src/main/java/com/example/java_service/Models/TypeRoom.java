package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "room")
public class TypeRoom
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_type_room", nullable = false, updatable = false)
    private UUID idTypeRoom;

    @Column(name = "description",columnDefinition = "TEXT")
    private String description;
}
