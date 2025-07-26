package com.paymybuddy.service;

import com.paymybuddy.exception.TransactionBusinessException;
import com.paymybuddy.model.DTO.TransactionRequest;
import com.paymybuddy.model.Transaction;
import com.paymybuddy.model.User;
import com.paymybuddy.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    public List<Transaction> getTransactionByUserSender(User user) {
        return transactionRepository.findBySender(user);
    }

    @Override
    public void saveNewTransaction(TransactionRequest transaction, User userSender) {
        if(transaction.getUserReceiver() == null) {
            throw new TransactionBusinessException("Le destinataire est requis", "transaction", transaction);
        }

        if(transaction.getAmount() == null) {
            throw new TransactionBusinessException("Le montant est requis", "transaction", transaction);
        }

        if(transaction.getUserReceiver().getEmail().equalsIgnoreCase(userSender.getEmail())) {
            throw new TransactionBusinessException("Vous ne pouvez pas vous envoyer de l'argent à vous même", "transaction", transaction);
        }

        Transaction transactionObj = new Transaction();
        transactionObj.setSender(userSender);
        transactionObj.setReceiver(transaction.getUserReceiver());
        transactionObj.setDescription(transaction.getDescription());
        transactionObj.setAmount(transaction.getAmount());

        transactionRepository.save(transactionObj);
    }
}
