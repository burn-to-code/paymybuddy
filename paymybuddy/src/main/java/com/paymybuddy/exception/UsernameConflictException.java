package com.paymybuddy.exception;

import lombok.Getter;

@Getter
public class UsernameConflictException extends RuntimeException{

    public UsernameConflictException(String message) {
        super(message);
    }
}
