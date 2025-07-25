package com.paymybuddy.exception;

import lombok.Getter;

@Getter
public class EmailConflictException extends RuntimeException{

    private final String urlName;
    private final Object formData;

    public EmailConflictException(String message, String urlName, Object formData) {
        super(message);
        this.urlName = urlName;
        this.formData = formData;
    }
}
