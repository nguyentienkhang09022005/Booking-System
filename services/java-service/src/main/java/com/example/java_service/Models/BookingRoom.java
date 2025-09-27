package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
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

    @Column(name = "checkin_date")
    private Date checkinDate;

    @Column(name = "checkout_date")
    private Date checkoutDate;

    @Column(name = "price_booking_room", precision = 12, scale = 2)
    private BigDecimal priceBookingRoom;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "create_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createAt;

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
