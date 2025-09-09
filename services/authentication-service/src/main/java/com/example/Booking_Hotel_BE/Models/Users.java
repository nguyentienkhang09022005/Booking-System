package com.example.Booking_Hotel_BE.Models;

import jakarta.persistence.*;
import lombok.*;
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
public class Users {

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

    @Column(name = "create_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createAt = LocalDateTime.now();
}
