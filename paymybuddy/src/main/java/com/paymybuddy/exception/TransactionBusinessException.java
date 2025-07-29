package com.paymybuddy.exception;
import lombok.Getter;

@Getter
public class TransactionBusinessException extends RuntimeException {

    public TransactionBusinessException(String message) {
        super(message);
    }
}
