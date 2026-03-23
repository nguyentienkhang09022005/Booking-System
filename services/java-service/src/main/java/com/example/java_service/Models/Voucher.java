package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "voucher")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_voucher", nullable = false, updatable = false)
    private UUID idVoucher;

    @ManyToOne
    @JoinColumn(name = "id_accommodation", referencedColumnName = "id_accommodation", foreignKey = @ForeignKey(name = "FK_VOUCHER_HOTEL"))
    private Accommodation accommodation;

    @Column(name = "name_voucher", length = 100)
    private String nameVoucher;

    @Column(name = "code", length = 50, unique = true)
    private String code;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "min_discount_amount", precision = 12, scale = 2)
    private BigDecimal minDiscountAmount;

    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "sponsor_type", length = 50)
    private String sponsorType;

    @Column(name = "apply_to", length = 20)
    private String applyTo;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "conditions", columnDefinition = "TEXT")
    private String conditions;

    @Column(name = "is_approved", nullable = false)
    private Boolean approved = false;

    @CreationTimestamp
    @Column(name = "create_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createAt;
}