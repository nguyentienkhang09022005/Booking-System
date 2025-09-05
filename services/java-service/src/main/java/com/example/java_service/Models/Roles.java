package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roles")
public class Roles {

    @Id
    @Column(name = "role_name", length = 20)
    private String roleName;

    @Column(columnDefinition = "TEXT")
    private String description;
}
