package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "booking_room")
public class BookingRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_booking_room", nullable = false, updatable = false)
    private UUID idBookingRoom;

    @Column(name = "price_booking_room", precision = 12, scale = 2)
    private BigDecimal priceBookingRoom;

    @Column(name = "quantity")
    private Integer quantity;

    @CreationTimestamp
    @Column(name = "create_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createAt;

    @ManyToOne
    @JoinColumn(
            name = "id_booking",
            referencedColumnName = "id_booking",
            foreignKey = @ForeignKey(name = "FK_BOOKING_ROOM_BOOKING")
    )
    private Booking booking;

    @ManyToOne
    @JoinColumn(
            name = "id_room",
            referencedColumnName = "id_room",
            foreignKey = @ForeignKey(name = "FK_BOOKING_ROOM_ROOM")
    )
    private Room room;
}
