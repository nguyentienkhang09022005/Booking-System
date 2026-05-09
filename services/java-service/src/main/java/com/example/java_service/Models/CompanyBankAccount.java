package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_bank_account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyBankAccount {

    @Id
    @Column(name = "id_user")
    private UUID idUser;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id_user", foreignKey = @ForeignKey(name = "fk_bank_account_user"))
    private Users users;

    @Column(name = "bank_name", length = 100, nullable = false)
    private String bankName;

    @Column(name = "account_number", length = 50, nullable = false)
    private String accountNumber;

    @Column(name = "account_holder_name", length = 100, nullable = false)
    private String accountHolderName;

    @Column(name = "branch", length = 100)
    private String branch;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}