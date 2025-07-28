package com.paymybuddy.service;

import com.paymybuddy.model.DTO.TransactionRequest;
import com.paymybuddy.model.DTO.TransactionShowDTO;
import com.paymybuddy.model.Transaction;
import com.paymybuddy.model.User;

import java.util.List;

public interface TransactionService {

    List<Transaction> getTransactionByUserSender(User user);

    void saveNewTransaction(TransactionRequest transaction, User userSender);

    List<TransactionShowDTO> getTransactionDTOToShow(List<Transaction> transactions);
}
