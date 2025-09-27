package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelFacilityId implements Serializable
{
    @Column(name = "id_hotel")
    private UUID idHotel;

    @Column(name = "id_facility")
    private UUID idFacility;
}
