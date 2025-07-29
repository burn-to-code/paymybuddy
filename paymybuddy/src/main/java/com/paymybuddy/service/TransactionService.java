package com.paymybuddy.service;

import com.paymybuddy.model.DTO.TransactionRequest;
import com.paymybuddy.model.DTO.ResponseTransactionDTO;
import com.paymybuddy.model.Transaction;
import com.paymybuddy.model.User;

import java.util.List;

public interface TransactionService {

    List<Transaction> getTransactionByUserSenderId(Long userId);

    void saveNewTransaction(TransactionRequest transaction, User userSender);

    List<ResponseTransactionDTO> getTransactionDTOToShow(List<Transaction> transactions);
}
