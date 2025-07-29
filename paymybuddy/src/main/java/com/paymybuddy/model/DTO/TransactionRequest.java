package com.paymybuddy.model.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {
    @NotNull(message = "Vous devez ajouter un destinataire")
    private Long userReceiverId;

    private String description;

    @NotNull(message = "Une valeur est requise")
    @DecimalMin(value = "0.01", message = "Le montant doit être positif")
    private BigDecimal amount;
}
