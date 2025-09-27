package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "hotel_facility")
public class HotelFacility implements Serializable
{
    @EmbeddedId
    private HotelFacilityId idHotelFacility;

    @ManyToOne
    @MapsId("idHotel")
    @JoinColumn(name = "id_hotel",
            insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_HOTEL_FACILITY_1"))
    private Hotel hotel;

    @ManyToOne
    @MapsId("idFacility")
    @JoinColumn(name = "id_facility",
            insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_HOTEL_FACILITY_2"))
    private Facility facility;
}
