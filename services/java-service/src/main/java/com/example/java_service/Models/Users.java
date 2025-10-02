package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class Users
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_user", nullable = false, updatable = false)
    private UUID idUser;

    @ManyToOne
    @JoinColumn(name = "role_name",
            referencedColumnName = "role_name",
            foreignKey = @ForeignKey(name = "FK_USER_ROLE"))
    private Roles role;

    @ManyToOne
    @JoinColumn(name = "type_user",
            referencedColumnName = "type_user",
            foreignKey = @ForeignKey(name = "FK_USER_TYPE_USER"))
    private TypeUser typeUser;

    @Column(name = "first_name", length = 20)
    private String firstName;

    @Column(name = "last_name", length = 20)
    private String lastName;

    @Column(unique = true, length = 12)
    private String phone;

    @Column(length = 5)
    private String gender; // "Nam" hoặc "Nữ"

    private LocalDate dob;

    @Column(name = "address_user",columnDefinition = "TEXT")
    private String address;

    @Column(unique = true)
    private String email;

    @Column(name = "password_user")
    private String passwordUser;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createAt;

    @PrePersist
    public void prePersist() {
        if (this.totalAmount == null) {
            this.totalAmount = BigDecimal.ZERO;
        }
    }
}
