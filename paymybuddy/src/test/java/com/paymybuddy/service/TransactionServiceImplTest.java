package com.paymybuddy.service;

import com.paymybuddy.exception.TransactionBusinessException;
import com.paymybuddy.model.DTO.ResponseTransactionDTO;
import com.paymybuddy.model.DTO.TransactionRequest;
import com.paymybuddy.model.Transaction;
import com.paymybuddy.model.User;
import com.paymybuddy.repository.TransactionRepository;
import com.paymybuddy.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceImplTest {

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;


    // ==== Groupe saveNewTransaction ====
    @Tag("saveNewTransaction")
    @Test
    void saveNewTransaction_ShouldThrow_WhenUserReceiverIdIsNull() {
        // Given
        User sender = createUser(1L, "sender@example.com", "Sender");
        TransactionRequest request = createTransactionRequest(null, BigDecimal.valueOf(50.0), "Test");

        // When & Then
        TransactionBusinessException ex = assertThrows(TransactionBusinessException.class,
                () -> transactionService.saveNewTransaction(request, sender));
        assertEquals("Le destinataire est requis", ex.getMessage());

        verify(transactionRepository, never()).save(any());
    }

    @Tag("saveNewTransaction")
    @Test
    void saveNewTransaction_ShouldThrow_WhenAmountIsNull() {
        // Given
        User sender = createUser(1L, "sender@example.com", "Sender");
        sender.setAccount(BigDecimal.valueOf(100.0));
        TransactionRequest request = createTransactionRequest(2L, null, "Test");

        // When & Then
        TransactionBusinessException ex = assertThrows(TransactionBusinessException.class,
                () -> transactionService.saveNewTransaction(request, sender));
        assertEquals("Le montant ou le solde est nul", ex.getMessage());

        verify(transactionRepository, never()).save(any());
    }

    @Tag("saveNewTransaction")
    @Test
    void saveNewTransaction_ShouldThrow_WhenSenderIsReceiver() {
        // Given
        User sender = createUser(1L, "sender@example.com", "Sender");
        TransactionRequest request = createTransactionRequest(1L, BigDecimal.valueOf(50.0), "Test");

        // When & Then
        TransactionBusinessException ex = assertThrows(TransactionBusinessException.class,
                () -> transactionService.saveNewTransaction(request, sender));
        assertEquals("Vous ne pouvez pas vous envoyer de l'argent à vous même", ex.getMessage());

        verify(transactionRepository, never()).save(any());
    }

    @Tag("saveNewTransaction")
    @Test
    void saveNewTransaction_ShouldThrow_WhenReceiverNotFound() {
        // Given
        User sender = createUser(1L, "sender@example.com", "Sender");
        sender.setAccount(BigDecimal.valueOf(100.0));
        TransactionRequest request = createTransactionRequest(2L, BigDecimal.valueOf(50.0), "Test");

        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        // When & Then
        TransactionBusinessException ex = assertThrows(TransactionBusinessException.class,
                () -> transactionService.saveNewTransaction(request, sender));
        assertEquals("Le destinataire n'existe pas", ex.getMessage());

        verify(transactionRepository, never()).save(any());
    }

    @Tag("saveNewTransaction")
    @Test
    void saveNewTransaction_ShouldSave_WhenValidRequest() {
        // Given
        User sender = createUser(1L, "sender@example.com", "Sender");
        sender.setAccount(BigDecimal.valueOf(100.0));
        User receiver = createUser(2L, "receiver@example.com", "Receiver");
        receiver.setAccount(BigDecimal.valueOf(100.0));
        TransactionRequest request = createTransactionRequest(2L, BigDecimal.valueOf(50.0), "Paiement");

        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        // When
        transactionService.saveNewTransaction(request, sender);

        // Then
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(captor.capture());
        Transaction savedTransaction = captor.getValue();

        assertEquals("Paiement", savedTransaction.getDescription());
        assertEquals(BigDecimal.valueOf(50.0), savedTransaction.getAmount());
        assertEquals(sender, savedTransaction.getSender());
        assertEquals(receiver, savedTransaction.getReceiver());


        verify(userRepository).updateAccount(sender.getId(), BigDecimal.valueOf(50.0));   // 100 - 50
        verify(userRepository).updateAccount(receiver.getId(), BigDecimal.valueOf(150.0)); // 100 + 50
    }

    @Test
    void testSaveNewTransaction_shouldThrowException_whenAmountOrAccountIsNullOrZero() {
        TransactionRequest request = createTransactionRequest(1L, null, "Sender");

        User sender = createUser(3L, "sender@example.com", "Sender" );
        sender.setAccount(null);

        TransactionBusinessException exception = assertThrows(TransactionBusinessException.class, () ->
                transactionService.saveNewTransaction(request, sender)
        );

        assertEquals("Le montant ou le solde est nul", exception.getMessage());
    }

    @Test
    void testSaveNewTransaction_shouldThrowException_whenInsufficientFunds() {
        TransactionRequest request = createTransactionRequest(1L, BigDecimal.valueOf(200), "Sender");

        User sender = createUser(3L, "sender@example.com", "Sender" );
        sender.setAccount(BigDecimal.valueOf(100));

        User receiver = new User();
        receiver.setEmail("receiver@example.com");

        TransactionBusinessException exception = assertThrows(TransactionBusinessException.class, () ->
                transactionService.saveNewTransaction(request, sender)
        );

        assertEquals("Solde insuffisant : " + sender.getAccount() + " € disponible, mais " + request.getAmount() + " € demandé.", exception.getMessage());
        assertTrue(exception.getMessage().contains("100"));
        assertTrue(exception.getMessage().contains("200"));
    }

    // ==== Groupe getTransactionByUserSenderId ====
    @Tag("getTransactionByUserSenderId")
    @Test
    void getTransactionByUserSenderId_ShouldReturnTransactions_WhenUserHasTransactions() {
        // Given
        Long userId = 1L;
        User sender = createUser(userId, "sender@example.com", "Sender");
        Transaction tx1 = new Transaction("desc1", BigDecimal.valueOf(10), sender, createUser(2L, "rec1@example.com", "Receiver1"));
        Transaction tx2 = new Transaction("desc2", BigDecimal.valueOf(20), sender, createUser(3L, "rec2@example.com", "Receiver2"));
        List<Transaction> transactions = List.of(tx1, tx2);

        when(transactionRepository.findBySender_Id(userId)).thenReturn(transactions);

        // When
        List<Transaction> result = transactionService.getTransactionByUserSenderId(userId);

        // Then
        assertEquals(2, result.size());
        assertEquals("desc1", result.get(0).getDescription());
        assertEquals("desc2", result.get(1).getDescription());
        verify(transactionRepository, times(1)).findBySender_Id(userId);
    }

    @Tag("getTransactionByUserSenderId")
    @Test
    void getTransactionByUserSenderId_ShouldReturnEmptyList_WhenUserHasNoTransactions() {
        // Given
        Long userId = 1L;

        when(transactionRepository.findBySender_Id(userId)).thenReturn(List.of());

        // When
        List<Transaction> result = transactionService.getTransactionByUserSenderId(userId);

        // Then
        assertEquals(0, result.size());
        verify(transactionRepository, times(1)).findBySender_Id(userId);
    }


    // ==== Groupe getTransactionByUserSenderId ====
    @Tag("getTransactionByUserSenderId")
    @Test
    void getTransactionDTOToShow_ShouldConvertTransactionsToDTOs() {
        // Given
        User receiver1 = createUser(2L, "receiver1@example.com", "Receiver1");
        User receiver2 = createUser(3L, "receiver2@example.com", "Receiver2");

        Transaction tx1 = new Transaction("desc1", BigDecimal.valueOf(10), createUser(1L, "sender@example.com", "Sender"), receiver1);
        Transaction tx2 = new Transaction("desc2", BigDecimal.valueOf(20), createUser(1L, "sender@example.com", "Sender"), receiver2);

        List<Transaction> transactions = List.of(tx1, tx2);

        // When
        List<ResponseTransactionDTO> dtos = transactionService.getTransactionDTOToShow(transactions);

        // Then
        assertEquals(2, dtos.size());

        assertEquals("Receiver1", dtos.getFirst().getReceiverName());
        assertEquals("desc1", dtos.get(0).getDescription());
        assertEquals(BigDecimal.valueOf(10), dtos.get(0).getAmount());

        assertEquals("Receiver2", dtos.get(1).getReceiverName());
        assertEquals("desc2", dtos.get(1).getDescription());
        assertEquals(BigDecimal.valueOf(20), dtos.get(1).getAmount());
    }

    @Tag("getTransactionByUserSenderId")
    @Test
    void getTransactionDTOToShow_ShouldReturnEmptyList_WhenNoTransactions() {
        // Given
        List<Transaction> transactions = List.of();

        // When
        List<ResponseTransactionDTO> dtos = transactionService.getTransactionDTOToShow(transactions);

        // Then
        assertEquals(0, dtos.size());
    }


    // ==== Utils ====
    private User createUser(Long id, String email, String username) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUsername(username);
        return user;
    }

    private TransactionRequest createTransactionRequest(Long receiverId, BigDecimal amount, String description) {
        TransactionRequest request = new TransactionRequest();
        request.setUserReceiverId(receiverId);
        request.setAmount(amount);
        request.setDescription(description);
        return request;
    }
}
