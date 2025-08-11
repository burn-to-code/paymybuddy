package com.paymybuddy.controllerIT;
import com.paymybuddy.model.AuthProvider;
import com.paymybuddy.model.DTO.ResponseTransactionDTO;
import com.paymybuddy.model.Transaction;
import com.paymybuddy.model.User;
import com.paymybuddy.repository.TransactionRepository;
import com.paymybuddy.repository.UserRepository;
import com.paymybuddy.security.UserDetailsImpl;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test",
        "spring.security.oauth2.client.registration.google.client-secret=test"
})
public class TransactionControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionRepository transactionRepository;

    private User userSender;
    private User userReceiver1;
    private User userReceiver2;

    private Transaction transaction2;
    private Transaction transaction;

    final static String SENDER_EMAIL = "userSender@email.com";
    final static String RECEIVER_EMAIL_1 = "emailReceiver1@test.com";
    final static String RECEIVER_EMAIL_2 = "emailReceiver2@test.com";

    final static String USERNAME_SENDER = "Sender1";
    final static String USERNAME_RECEIVER_1 = "Receiver1";
    final static String USERNAME_RECEIVER_2 = "Receiver2";

    final static String PASSWORD = "Password";

    @BeforeEach
    void setUpUserAndTransaction() {
        createUser();
        createTransaction();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldShowProfilePageWithRequestAttribute() throws Exception {
        ResponseTransactionDTO response1 = new ResponseTransactionDTO(transaction.getReceiver().getUsername(), transaction.getDescription(), transaction.getAmount());
        ResponseTransactionDTO response2 = new ResponseTransactionDTO(transaction2.getReceiver().getUsername(), transaction2.getDescription(), transaction2.getAmount());

        mockMvc.perform(get("/transferer")
                        .with(user(new UserDetailsImpl(userSender))))
                .andExpect(status().isOk())
                .andExpect(view().name("transferer"))
                .andExpect(model().attributeExists("request"))
                .andExpect(model().attribute("request", allOf(
                        hasProperty("userReceiverId", is(0L)),
                        hasProperty("description", nullValue()),
                        hasProperty("amount", nullValue())
                )))
                .andExpect(model().attribute("contacts", containsInAnyOrder(userReceiver1, userReceiver2)))
                .andExpect(model().attribute("transactions", containsInAnyOrder(response1, response2)));
    }

    @Test
    void ShouldSubmitTransaction() throws Exception {

        mockMvc.perform(post("/transferer")
                        .param("userReceiverId", userReceiver1.getId().toString())
                        .param("description", "new Transaction")
                        .param("amount", "20")
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userSender))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transferer"))
                .andExpect(flash().attribute("success",
                        "transaction effectuée avec succès"));


        mockMvc.perform(get("/transferer")
                        .with(user(new UserDetailsImpl(userSender))))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("transactions"))
                .andExpect(model().attribute("transactions", hasItem(
                        allOf(
                                hasProperty("receiverName", is(USERNAME_RECEIVER_1)),
                                hasProperty("description", is("new Transaction")),
                                hasProperty("amount", is(new BigDecimal("20")))
                        )
                )));
    }

    @Test
    void ShouldSubmitTransactionIsOkWithNNumberAtVirgule() throws Exception {

        mockMvc.perform(post("/transferer")
                        .param("userReceiverId", userReceiver1.getId().toString())
                        .param("description", "new Transaction")
                        .param("amount", "20.28")
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userSender))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transferer"))
                .andExpect(flash().attribute("success",
                        "transaction effectuée avec succès"));


        mockMvc.perform(get("/transferer")
                        .with(user(new UserDetailsImpl(userSender))))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("transactions"))
                .andExpect(model().attribute("transactions", hasItem(
                        allOf(
                                hasProperty("receiverName", is(USERNAME_RECEIVER_1)),
                                hasProperty("description", is("new Transaction")),
                                hasProperty("amount", is(new BigDecimal("20.28")))
                        )
                )));
    }

    @Test
    void ShouldSubmitTransactionWithInvalidAmount() throws Exception {
        mockMvc.perform(post("/transferer")
                        .param("userReceiverId", userReceiver1.getId().toString())
                        .param("description", "new Transaction")
                        .param("amount", "invalid")
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userSender))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transferer"))
                .andExpect(flash().attribute("errors", hasItem("Le montant est invalide")));
    }

    @Test
    void ShouldSubmitTransactionWithNullAmount() throws Exception {
        mockMvc.perform(post("/transferer")
                        .param("userReceiverId", userReceiver1.getId().toString())
                        .param("description", "new Transaction")
                        .param("amount", (String) null)
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userSender))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transferer"))
                .andExpect(flash().attribute("errors", hasItem("Le montant est invalide")));
    }

    @Test
    void ShouldSubmitTransactionsWithInvalidId() throws Exception {
        mockMvc.perform(post("/transferer")
                        .param("userReceiverId", "")
                        .param("description", "new Transaction")
                        .param("amount", "20")
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userSender))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transferer"))
                .andExpect(flash().attribute("errors", hasItem("Vous devez ajouter un destinataire")));
    }

    @Test
    void ShouldSubmitTransactionsWithNullId()  throws Exception {
        mockMvc.perform(post("/transferer")
                        .param("userReceiverId", (String) null)
                        .param("description", "new Transaction")
                        .param("amount", "20")
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userSender))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transferer"))
                .andExpect(flash().attribute("errors", hasItem("Vous devez ajouter un destinataire")));
    }

    @Test
    void ShouldRejectTransactionWithZeroAmount() throws Exception {
        mockMvc.perform(post("/transferer")
                        .param("userReceiverId", userReceiver1.getId().toString())
                        .param("description", "Zero amount test")
                        .param("amount", "0")
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userSender))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transferer"))
                .andExpect(flash().attribute("errors", hasItem("Le montant doit être positif")));
    }

    @Test
    void ShouldRejectTransactionWithNegativeAmount() throws Exception {
        mockMvc.perform(post("/transferer")
                        .param("userReceiverId", userReceiver1.getId().toString())
                        .param("description", "Negative amount test")
                        .param("amount", "-10")
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userSender))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transferer"))
                .andExpect(flash().attribute("errors", hasItem("Le montant doit être positif")));
    }

    @Test
    void ShouldRejectTransactionToSelf() throws Exception {
        mockMvc.perform(post("/transferer")
                        .param("userReceiverId", userSender.getId().toString())
                        .param("description", "Self transaction")
                        .param("amount", "10")
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userSender))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transferer"))
                .andExpect(flash().attribute("error", "Vous ne pouvez pas vous envoyer de l'argent à vous même"));
    }

    @Test
    void ShouldRejectTransactionBecauseAmountIsSuperiorAtAccount() throws Exception {

        BigDecimal account = new BigDecimal(10);
        BigDecimal amount = new BigDecimal(100);

        userSender.setAccount(account);
        mockMvc.perform(post("/transferer")
                        .param("userReceiverId", userReceiver1.getId().toString())
                        .param("description", "Self transaction")
                        .param("amount", amount.toString())
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userSender))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transferer"))
                .andExpect(flash().attribute("error", "Solde insuffisant : " + account + " € disponible, mais " + amount + " € demandé."));
    }

    @Test
    void ShouldRejectTransactionBecauseUserReceiverDoesntExist() throws Exception {

        mockMvc.perform(post("/transferer")
                        .param("userReceiverId", "10")
                        .param("description", "Self transaction")
                        .param("amount", "20")
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userSender))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transferer"))
                .andExpect(flash().attribute("error", "Le destinataire n'existe pas"));
    }

    @Test
    void ShouldRejectTransactionBecauseAmountIsNotCorrectlyCreated() throws Exception {

        mockMvc.perform(post("/transferer")
                        .param("userReceiverId", "10")
                        .param("description", "Self transaction")
                        .param("amount", "20.024")
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userSender))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transferer"))
                .andExpect(flash().attribute("errors", hasItem("Le montant dois contenir au maximum 2 décimales")));
    }



    //UTILS
    private void createUser() {
        userRepository.deleteAll();

        userReceiver1 = new User();
        userReceiver1.setEmail(RECEIVER_EMAIL_1);
        userReceiver1.setUsername(USERNAME_RECEIVER_1);
        userReceiver1.setAccount(new BigDecimal(100));
        userReceiver1.setPassword(passwordEncoder.encode(PASSWORD));
        userReceiver1.setProvider(AuthProvider.LOCAL);

        userReceiver2 = new User();
        userReceiver2.setEmail(RECEIVER_EMAIL_2);
        userReceiver2.setUsername(USERNAME_RECEIVER_2);
        userReceiver2.setAccount(new BigDecimal(100));
        userReceiver2.setPassword(passwordEncoder.encode(PASSWORD));
        userReceiver2.setProvider(AuthProvider.LOCAL);

        userSender = new User();
        userSender.setEmail(SENDER_EMAIL);
        userSender.setUsername(USERNAME_SENDER);
        userSender.setAccount(new BigDecimal(100));
        userSender.setPassword(passwordEncoder.encode(PASSWORD));
        userSender.setProvider(AuthProvider.LOCAL);

        userSender.setConnections(List.of(userReceiver1, userReceiver2));

        userRepository.save(userSender);
        userRepository.save(userReceiver1);
        userRepository.save(userReceiver2);
    }

    private void createTransaction() {
        transactionRepository.deleteAll();

        transaction = new Transaction();
        transaction.setAmount(new BigDecimal(20));
        transaction.setSender(userSender);
        transaction.setDescription("Test description");
        transaction.setReceiver(userReceiver1);

        transaction2 = new Transaction();
        transaction2.setAmount(new BigDecimal(50));
        transaction2.setSender(userSender);
        transaction2.setDescription("Test description 2");
        transaction2.setReceiver(userReceiver2);

        transactionRepository.save(transaction);
        transactionRepository.save(transaction2);
    }

}
