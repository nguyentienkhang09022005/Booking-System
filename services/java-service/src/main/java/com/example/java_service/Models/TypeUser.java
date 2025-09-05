package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "type_user")
public class TypeUser {

    @Id
    @Column(name = "type_user", length = 30)
    private String typeUser;

    @Column(columnDefinition = "TEXT")
    private String description;
}
