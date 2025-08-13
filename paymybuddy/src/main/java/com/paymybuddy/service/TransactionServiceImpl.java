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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Service de gestion des transactions entre utilisateurs.
 * Implémente les opérations pour récupérer, créer et transformer les transactions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    /**
     * Récupère toutes les transactions effectuées par un utilisateur donné.
     *
     * @param userId l'identifiant de l'utilisateur expéditeur
     * @return la liste des transactions où l'utilisateur est l'expéditeur
     */
    @Override
    public List<Transaction> getTransactionByUserSenderId(Long userId) {
        log.info("Récupération des transactions de l'utilisateur avec l'id {}", userId);
        return transactionRepository.findBySender_Id(userId);
    }

    /**
     * Sauvegarde une nouvelle transaction entre un utilisateur expéditeur et un utilisateur destinataire.
     * Vérifie que :
     * - le destinataire existe et n'est pas le même que l'expéditeur,
     * - le montant est valide et ne dépasse pas le solde de l'expéditeur.
     *
     * @param transaction l'objet TransactionRequest contenant le destinataire, le montant et la description
     * @param userSender  l'utilisateur expéditeur de la transaction
     * @throws TransactionBusinessException si le destinataire est invalide, si le montant est incorrect
     *                                      ou si le solde est insuffisant
     */
    @Override
    @Transactional
    public void saveNewTransaction(TransactionRequest transaction, User userSender) {
        log.info("Tentative de sauvegarde d'une nouvelle transaction. UserSender: {}, Transaction: {}", userSender, transaction);

        if(transaction.getUserReceiverId() == null) {
            throw new TransactionBusinessException("Le destinataire est requis");
        } else if (transaction.getUserReceiverId().equals(userSender.getId())) {
            throw new TransactionBusinessException("Vous ne pouvez pas vous envoyer de l'argent à vous même");
        }

        BigDecimal amount = getBigDecimalAndVerifyIfTransactionIsOk(transaction, userSender);

        log.info("Montant valide");

        log.info("Tentative de récupération de l'utilisateur receveur avec l'id {}", transaction.getUserReceiverId());
        User userReceiver = userRepository.findById((transaction.getUserReceiverId()))
                .orElseThrow(() -> new TransactionBusinessException("Le destinataire n'existe pas"));

        transferMoney(userSender, userReceiver, amount);

        Transaction transactionObj = new Transaction(transaction.getDescription(),
                transaction.getAmount(),
                userSender,
                userReceiver);

        transactionRepository.save(transactionObj);

        log.info("Transaction sauvegardée avec succès entre {} et {} pour un montant de {}",
                userSender.getEmail(), userReceiver.getEmail(), amount);
    }

    /**
     * Transforme une liste de Transaction en une liste de DTO pour affichage.
     *
     * @param transactions la liste de transactions à transformer
     * @return la liste de ResponseTransactionDTO contenant : nom du destinataire, description et montant
     */
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

    // Utilitaire pour transaction d'argent

    private static BigDecimal getBigDecimalAndVerifyIfTransactionIsOk(TransactionRequest transaction, User userSender) {
        BigDecimal amount = transaction.getAmount();
        BigDecimal account = userSender.getAccount();

        if (amount == null) {
            throw new TransactionBusinessException("Le montant est obligatoire");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionBusinessException("Le montant ne doit pas être inférieur à 0");
        }
        if (amount.compareTo(account) > 0) {
            throw new TransactionBusinessException("Solde insuffisant : " + account + " € disponible, mais " + amount + " € demandé.");
        }

        amount = amount.setScale(2, RoundingMode.HALF_UP);

        return amount;
    }

    private void transferMoney(User sender, User receiver, BigDecimal amount) {
        BigDecimal newSenderAccount = sender.getAccount().subtract(amount);
        BigDecimal newReceiverAccount = receiver.getAccount().add(amount);

        sender.setAccount(newSenderAccount);
        receiver.setAccount(newReceiverAccount);

        userRepository.updateAccount(sender.getId(), newSenderAccount);
        userRepository.updateAccount(receiver.getId(), newReceiverAccount);

        log.info("Comptes mis à jour. Nouveau solde de l'expéditeur : {}", sender.getAccount());
        log.info("Nouveau solde du destinataire : {}", receiver.getAccount());
    }
}
