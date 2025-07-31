package com.paymybuddy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "transaction_logs")
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne // ou @ManyToOne selon ta relation
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "log_message")
    private String logMessage;

    @Column(name = "log_date", nullable = false)
    private LocalDateTime dateFacturation;

    @NotNull
    @DecimalMin(value = "0.01", message = "La commission doit être supérieure à 0")
    @Column(name = "commission", nullable = false, precision = 10, scale = 2)
    private BigDecimal commission;

    // getters et setters

    public void setCommission(BigDecimal commission) {
        if (commission == null || commission.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La commission doit être supérieure à zéro");
        }
        // Arrondi à 2 décimales, mode HALF_UP (classique en finance)
        this.commission = commission.setScale(2, RoundingMode.HALF_UP);
    }
}
