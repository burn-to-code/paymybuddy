package com.paymybuddy.exception;

import com.paymybuddy.model.DTO.TransactionShowDTO;
import com.paymybuddy.model.User;
import lombok.Getter;

import java.util.List;

@Getter
public class TransactionBusinessException extends RuntimeException implements ExceptionWithUrlName{
    private final String urlName;
    private final Object formData;
    private final List<User> UsersConnections;
    private final List<TransactionShowDTO> transactionList;

    public TransactionBusinessException(String message, String urlName, Object formData,  List<TransactionShowDTO> transactionList, List<User> UsersConnections) {
        super(message);
        this.urlName = urlName;
        this.formData = formData;
        this.transactionList = transactionList;
        this.UsersConnections = UsersConnections;
    }
}
