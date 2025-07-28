package com.paymybuddy.service;

import com.paymybuddy.exception.TransactionBusinessException;
import com.paymybuddy.model.DTO.TransactionRequest;
import com.paymybuddy.model.DTO.TransactionShowDTO;
import com.paymybuddy.model.Transaction;
import com.paymybuddy.model.User;
import com.paymybuddy.repository.TransactionRepository;
import com.paymybuddy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    public List<Transaction> getTransactionByUserSender(User user) {
        return transactionRepository.findBySender(user);
    }

    @Override
    public void saveNewTransaction(TransactionRequest transaction, User userSender) {
        if(transaction.getUserReceiverId() == 0L) {
            throw new TransactionBusinessException("Le destinataire est requis", "transferer", transaction);
        }

        if(transaction.getAmount() == null) {
            throw new TransactionBusinessException("Le montant est requis", "transferer", transaction);
        }

        if(transaction.getUserReceiverId() == userSender.getId()) {
            throw new TransactionBusinessException("Vous ne pouvez pas vous envoyer de l'argent à vous même", "transferer", transaction);
        }

        User userReceiver = userRepository.findById((transaction.getUserReceiverId()))
                .orElseThrow(() -> new TransactionBusinessException("Le destinataire n'existe pas", "transferer", transaction));

        Transaction transactionObj = new Transaction();
        transactionObj.setSender(userSender);
        transactionObj.setReceiver(userReceiver);
        transactionObj.setDescription(transaction.getDescription());
        transactionObj.setAmount(transaction.getAmount());

        transactionRepository.save(transactionObj);
    }

    @Override
    public List<TransactionShowDTO> getTransactionDTOToShow(List<Transaction> transactions) {
        return transactions.stream()
                .map(t -> new TransactionShowDTO(
                        t.getReceiver().getUsername(),
                        t.getDescription(),
                        t.getAmount()
                ))
                .toList();
    }
}
