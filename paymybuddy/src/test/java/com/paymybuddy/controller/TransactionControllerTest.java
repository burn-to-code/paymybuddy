package com.paymybuddy.controller;

import com.paymybuddy.model.DTO.TransactionRequest;
import com.paymybuddy.model.Transaction;
import com.paymybuddy.model.User;
import com.paymybuddy.service.SecurityUtils;
import com.paymybuddy.service.TransactionService;
import com.paymybuddy.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        TransactionController controller = new TransactionController(transactionService, userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(new InternalResourceViewResolver("/WEB-INF/views/", ".jsp"))
                .build();
    }

    @Test
    void showTransactionPage_shouldShowPageWithModel() throws Exception {
        User mockUser = new User();
        mockUser.setUsername("user1");

        List<User> mockConnections = List.of(new User());
        List<Transaction> mockTransactions = List.of(new Transaction());

        try (MockedStatic<SecurityUtils> mockedSecurity = Mockito.mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getConnectedUserId).thenReturn(1L);

            when(userService.getListOfConnectionOfCurrentUserById(1L)).thenReturn(mockConnections);
            when(transactionService.getTransactionByUserSenderId(1L)).thenReturn(mockTransactions);
            when(transactionService.getTransactionDTOToShow(mockTransactions)).thenReturn(List.of());

            mockMvc.perform(get("/transferer"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("transferer"))
                    .andExpect(model().attributeExists("request"))
                    .andExpect(model().attributeExists("contacts"))
                    .andExpect(model().attributeExists("transactions"));
        }
    }

    @Test
    void showTransactionPage_shouldHandleServiceException() throws Exception {
        try (MockedStatic<SecurityUtils> mockedSecurity = Mockito.mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getConnectedUserId).thenReturn(1L);

            when(userService.getListOfConnectionOfCurrentUserById(1L)).thenThrow(new RuntimeException("fail"));

            mockMvc.perform(get("/transferer"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/transferer"))
                    .andExpect(flash().attributeExists("error"));
        }
    }


    @Test
    void processTransaction_shouldRedirectWithErrors_whenValidationFails() throws Exception {
        User mockUser = new User();
        try (MockedStatic<SecurityUtils> mockedSecurity = Mockito.mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getConnectedUser).thenReturn(mockUser);

            mockMvc.perform(post("/transferer")
                            .param("userReceiverId", "") // suppose ce champ est obligatoire
                            .param("amount", ""))          // suppose montant obligatoire
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/transferer"))
                    .andExpect(flash().attributeExists("errors"));
        }
    }

    @Test
    void processTransaction_shouldRedirectWithErrorFlash_whenServiceThrows() throws Exception {
        User mockUser = new User();
        TransactionRequest req = new TransactionRequest();
        req.setUserReceiverId(1L);
        req.setAmount(new BigDecimal("10"));

        try (MockedStatic<SecurityUtils> mockedSecurity = Mockito.mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getConnectedUser).thenReturn(mockUser);

            doThrow(new RuntimeException("fail")).when(transactionService).saveNewTransaction(any(), eq(mockUser));

            mockMvc.perform(post("/transferer")
                            .param("userReceiverId", "1")
                            .param("amount", "10"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/transferer"))
                    .andExpect(flash().attributeExists("error"));
        }
    }

    @Test
    void processTransaction_shouldRedirectWithSuccessFlash_whenSuccess() throws Exception {
        User mockUser = new User();
        try (MockedStatic<SecurityUtils> mockedSecurity = Mockito.mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getConnectedUser).thenReturn(mockUser);

            doNothing().when(transactionService).saveNewTransaction(any(), eq(mockUser));

            mockMvc.perform(post("/transferer")
                            .param("userReceiverId", "1")
                            .param("amount", "10"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/transferer"))
                    .andExpect(flash().attribute("success", "transaction effectuée avec succès"));
        }
    }
}

