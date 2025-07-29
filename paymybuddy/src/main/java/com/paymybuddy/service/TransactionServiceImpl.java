package com.paymybuddy.service;

import com.paymybuddy.exception.TransactionBusinessException;
import com.paymybuddy.model.DTO.TransactionRequest;
import com.paymybuddy.model.DTO.ResponseTransactionDTO;
import com.paymybuddy.model.Transaction;
import com.paymybuddy.model.User;
import com.paymybuddy.repository.TransactionRepository;
import com.paymybuddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Override
    public List<Transaction> getTransactionByUserSenderId(Long userId) {
        log.info("Récupération des transactions de l'utilisateur avec l'id {}", userId);
        return transactionRepository.findBySender_Id(userId);
    }

    @Override
    public void saveNewTransaction(TransactionRequest transaction, User userSender) {
        log.info("Tentative de sauvegarde d'une nouvelle transaction. UserSender: {}, Transaction: {}", userSender, transaction);

        if(transaction.getUserReceiverId() == null) {
            throw new TransactionBusinessException("Le destinataire est requis");
        } else if (transaction.getAmount() == null) {
            throw new TransactionBusinessException("Le montant est requis");
        } else if (transaction.getUserReceiverId().equals(userSender.getId())) {
            throw new TransactionBusinessException("Vous ne pouvez pas vous envoyer de l'argent à vous même");
        }

        log.info("Transaction valide");

        log.info("Tentative de récupération de l'utilisateur receveur avec l'id {}", transaction.getUserReceiverId());
        User userReceiver = userRepository.findById((transaction.getUserReceiverId()))
                .orElseThrow(() -> new TransactionBusinessException("Le destinataire n'existe pas"));

        Transaction transactionObj = new Transaction(transaction.getDescription(),
                transaction.getAmount(),
                userSender,
                userReceiver);

        transactionRepository.save(transactionObj);
    }

    @Override
    public List<ResponseTransactionDTO> getTransactionDTOToShow(List<Transaction> transactions) {
        return transactions.stream()
                .map(t -> new ResponseTransactionDTO(
                        t.getReceiver().getUsername(),
                        t.getDescription(),
                        t.getAmount()
                ))
                .toList();
    }
}
