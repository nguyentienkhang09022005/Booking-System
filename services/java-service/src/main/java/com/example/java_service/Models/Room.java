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

    @ManyToOne
    @JoinColumn(name = "id_hotel",
            referencedColumnName = "id_hotel ",
            foreignKey = @ForeignKey(name = "FK_ROOM_HOTEL"))
    private Hotel hotel;

    @ManyToOne
    @JoinColumn(name = "type_room",
            referencedColumnName = "type_room",
            foreignKey = @ForeignKey(name = "FK_ROOM_TYPE_ROOM"))
    private TypeRoom typeRoom;

    @Column(name = "room_name", length = 50, nullable = false)
    private String roomName;

    @Column(name = "room_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal roomPrice;

    @Column(name = "size", length = 50, nullable = false)
    private String size;

    @Column(name = "view", length = 100, nullable = false)
    private String view;

    @Column(name = "regulations", columnDefinition = "TEXT")
    private String regulations;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
