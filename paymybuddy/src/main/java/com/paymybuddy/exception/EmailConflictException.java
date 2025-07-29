package com.paymybuddy.exception;

import lombok.Getter;

@Getter
public class EmailConflictException extends RuntimeException{

    public EmailConflictException(String message) {
        super(message);
    }
}
