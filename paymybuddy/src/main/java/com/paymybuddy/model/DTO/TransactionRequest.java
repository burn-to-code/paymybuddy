package com.paymybuddy.model.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {
    @NotNull(message = "Vous devez ajouter un destinataire")
    private Long userReceiverId;

    private String description;

    @NotNull(message = "Le montant est invalide")
    @DecimalMin(value = "0.01", message = "Le montant doit être positif")
    @Digits(integer = 10, fraction = 2, message = "Le montant dois contenir au maximum 2 décimales")
    private BigDecimal amount;
}
