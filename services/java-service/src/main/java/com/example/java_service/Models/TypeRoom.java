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
@Table(name = "type_room")
public class TypeRoom
{
    @Id
    @Column(name = "type_room", nullable = false, updatable = false)
    private String typeRoom;

    @Column(name = "description",columnDefinition = "TEXT")
    private String description;
}
