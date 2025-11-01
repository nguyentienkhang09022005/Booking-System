package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_voucher")
@IdClass(UserVoucherId.class)
public class UserVoucher
{
    @Id
    @ManyToOne
    @JoinColumn(name = "id_user",
            referencedColumnName = "id_user",
            foreignKey = @ForeignKey(name = "FK_USER_VOUCHER_USER"))
    private Users user;

    @Id
    @ManyToOne
    @JoinColumn(name = "id_voucher",
            referencedColumnName = "id_voucher",
            foreignKey = @ForeignKey(name = "FK_USER_VOUCHER_VOUCHER"))
    private Voucher voucher;

    @Column(name = "is_used")
    private Boolean isUsed = false;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(name = "saved_at")
    private LocalDateTime savedAt;
}
