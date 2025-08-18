package com.example.Booking_Hotel_BE.Models;

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
@Table(name = "users")
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_user")
    private String idUser;

    @Column(name = "name_user")
    private String nameuser;

    private String phone;
    private String email;

    private String account;

    @Column(name = "password_user")
    private String passworduser;
}
