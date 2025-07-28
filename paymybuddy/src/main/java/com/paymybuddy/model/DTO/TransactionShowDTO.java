package com.paymybuddy.model.DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionShowDTO {

    private String receiverName;
    private String description;
    private BigDecimal amount;

    public TransactionShowDTO(String receiverName, String description, BigDecimal amount) {
        this.receiverName = receiverName;
        this.description = description;
        this.amount = amount;
    }
}
