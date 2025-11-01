package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "applied_voucher")
public class AppliedVoucher
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_applied", nullable = false, updatable = false)
    private UUID idApplied;

    @ManyToOne
    @JoinColumn(name = "id_voucher",
            referencedColumnName = "id_voucher",
            foreignKey = @ForeignKey(name = "FK_APPLIED_VOUCHER"))
    private Voucher voucher;

    @ManyToOne
    @JoinColumn(name = "id_user",
            referencedColumnName = "id_user",
            foreignKey = @ForeignKey(name = "FK_APPLIED_USER"))
    private Users user;

    @ManyToOne
    @JoinColumn(name = "id_booking",
            referencedColumnName = "id_booking",
            foreignKey = @ForeignKey(name = "FK_APPLIED_BOOKING"))
    private Booking booking;

    @CreationTimestamp
    @Column(name = "applied_at")
    private LocalDateTime appliedAt;
}
