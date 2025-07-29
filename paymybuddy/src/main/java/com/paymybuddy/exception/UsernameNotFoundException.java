package com.paymybuddy.exception;

import lombok.Getter;

@Getter
public class UsernameNotFoundException extends RuntimeException{

    public UsernameNotFoundException(String message) {
        super(message);
    }
}
