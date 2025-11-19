package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "expense")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_expense", nullable = false, updatable = false)
    private UUID idExpense;

    @Column(name = "expense_title", length = 150)
    private String expenseTitle;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "department", length = 50)
    private String department;

    @CreationTimestamp
    @Column(name = "create_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createAt;

    @ManyToOne
    @JoinColumn(name = "id_user",
            referencedColumnName = "id_user",
            foreignKey = @ForeignKey(name = "FK_EXPENSE_USER"))
    private Users user;

    @ManyToOne
    @JoinColumn(name = "id_hotel",
            referencedColumnName = "id_hotel",
            foreignKey = @ForeignKey(name = "FK_EXPENSE_HOTEL"))
    private Hotel hotel;
}
