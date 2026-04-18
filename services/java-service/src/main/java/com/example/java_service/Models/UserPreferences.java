package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_preference", nullable = false, updatable = false)
    private UUID idPreference;

    @OneToOne
    @JoinColumn(name = "id_user", referencedColumnName = "id_user", foreignKey = @ForeignKey(name = "FK_PREFERENCES_USER"))
    private Users users;

    @Column(name = "travel_style", length = 50)
    private String travelStyle;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "interests", columnDefinition = "jsonb")
    private List<String> interests;

    @Column(name = "budget_level", length = 20)
    private String budgetLevel;

    @Column(name = "dietary_restrictions", length = 100)
    private String dietaryRestrictions;

    @Column(name = "mobility_limitations", length = 100)
    private String mobilityLimitations;

    @Column(name = "preferred_accommodation", length = 50)
    private String preferredAccommodation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_activities", columnDefinition = "jsonb")
    private List<String> preferredActivities;

    @Column(name = "climate_preference", length = 30)
    private String climatePreference;

    @Column(name = "traveling_with", length = 50)
    private String travelingWith;

    @Column(name = "has_children")
    private Boolean hasChildren = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "children_ages", columnDefinition = "jsonb")
    private List<Integer> childrenAges;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @Column(name = "is_completed")
    private Boolean isCompleted = false;
}