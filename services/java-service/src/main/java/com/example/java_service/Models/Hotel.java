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
@Table(name = "hotel")
public class Hotel
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_hotel", nullable = false, updatable = false)
    private UUID idHotel;

    @ManyToOne
    @JoinColumn(name = "id_user",
            referencedColumnName = "id_user ",
            foreignKey = @ForeignKey(name = "FK_HOTEL_USER"))
    private Users users;

    @Column(name = "hotel_name", length = 100)
    private String hotelName;

    @Column(name = "street", length = 100)
    private String street;

    @Column(name = "ward", length = 30)
    private String ward;

    @Column(name = "city", length = 30)
    private String city;

    @Column(name = "description",columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_approved", nullable = false)
    private Boolean approved = false;
}
