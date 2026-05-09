package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_balance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyBalance {

    @Id
    @Column(name = "id_user")
    private UUID idUser;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id_user", foreignKey = @ForeignKey(name = "fk_company_balance_user"))
    private Users users;

    @Column(name = "total_earned", precision = 16, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalEarned = BigDecimal.ZERO;

    @Column(name = "total_withdrawn", precision = 16, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalWithdrawn = BigDecimal.ZERO;

    @Column(name = "locked_amount", precision = 16, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal lockedAmount = BigDecimal.ZERO;

    public BigDecimal getAvailableBalance() {
        return totalEarned.subtract(totalWithdrawn).subtract(lockedAmount);
    }
}