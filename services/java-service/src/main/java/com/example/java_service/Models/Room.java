package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "room")
public class Room
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_room", nullable = false, updatable = false)
    private UUID idRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hotel",
            referencedColumnName = "id_hotel ",
            foreignKey = @ForeignKey(name = "FK_ROOM_HOTEL"))
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_room",
            referencedColumnName = "type_room",
            foreignKey = @ForeignKey(name = "FK_ROOM_TYPE_ROOM"))
    private TypeRoom typeRoom;

    @Column(name = "room_name", length = 50, nullable = false)
    private String roomName;

    @Column(name = "room_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal roomPrice;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
