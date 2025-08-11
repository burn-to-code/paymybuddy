package com.paymybuddy.controllerIT;

import com.paymybuddy.model.AuthProvider;
import com.paymybuddy.model.User;
import com.paymybuddy.repository.UserRepository;
import com.paymybuddy.security.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test",
        "spring.security.oauth2.client.registration.google.client-secret=test"
})
@Transactional
public class AddRelationControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User userConnected;
    private User userFriend;
    private User userNoFriend;

    final static String SENDER_EMAIL = "userSender@email.com";
    final static String RECEIVER_EMAIL_1 = "emailReceiver1@test.com";
    final static String RECEIVER_EMAIL_2 = "emailReceiver2@test.com";

    final static String USERNAME_SENDER = "Sender1";
    final static String USERNAME_RECEIVER_1 = "Receiver1";
    final static String USERNAME_RECEIVER_2 = "Receiver2";

    final static String PASSWORD = "Password";

    @BeforeEach
    public void setup() {
            userRepository.deleteAll();

            userFriend = new User();
            userFriend.setEmail(RECEIVER_EMAIL_1);
            userFriend.setUsername(USERNAME_RECEIVER_1);
            userFriend.setAccount(new BigDecimal(100));
            userFriend.setPassword(passwordEncoder.encode(PASSWORD));
            userFriend.setProvider(AuthProvider.LOCAL);


            userNoFriend = new User();
            userNoFriend.setEmail(RECEIVER_EMAIL_2);
            userNoFriend.setUsername(USERNAME_RECEIVER_2);
            userNoFriend.setAccount(new BigDecimal(100));
            userNoFriend.setPassword(passwordEncoder.encode(PASSWORD));
            userNoFriend.setProvider(AuthProvider.LOCAL);
            userNoFriend.setConnections(new ArrayList<>(List.of()));

            userConnected = new User();
            userConnected.setEmail(SENDER_EMAIL);
            userConnected.setUsername(USERNAME_SENDER);
            userConnected.setAccount(new BigDecimal(100));
            userConnected.setPassword(passwordEncoder.encode(PASSWORD));
            userConnected.setProvider(AuthProvider.LOCAL);

            userFriend.setConnections(new ArrayList<>(List.of()));
            userConnected.setConnections(new ArrayList<>(List.of(userFriend)));

            userRepository.save(userConnected);
            userRepository.save(userFriend);
            userRepository.save(userNoFriend);

    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldGetPageAddRelationWithCorrectlyObjectAndWebPage() throws Exception {
        MvcResult result = mockMvc.perform(get("/ajouter-relation")
                .with(user(new UserDetailsImpl(userConnected))))
                .andExpect(status().isOk())
                .andExpect(view().name("ajouter-relation"))
                .andReturn();

        String html = result.getResponse().getContentAsString();

        assertThat(html).contains("<form");
        assertThat(html).contains("action=\"/ajouter-relation\"");
        assertThat(html).contains("type=\"email\"");
        assertThat(html).contains("placeholder=\"Saisir une adresse mail\"");
        assertThat(html).contains("<button type=\"submit\">Ajouter</button>");
    }

    @Test
    @WithAnonymousUser
    public void shouldRedirectToLoginPageWhenNotConnected() throws Exception {
        mockMvc.perform(get("/ajouter-relation"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void shouldAddRelationSuccessfully() throws Exception {
        mockMvc.perform(post("/ajouter-relation")
                .with(user(new UserDetailsImpl(userConnected)))
                .with(csrf())
                .param("email", userNoFriend.getEmail()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ajouter-relation"))
                .andExpect(flash().attribute("success", "Utilisateur ajouté avec succès !"));

        assertEquals(2, userConnected.getConnections().size());
        assertEquals(0, userNoFriend.getConnections().size());
    }

    @Test
    void shouldReturnErrorWhenEmailIsEmpty() throws Exception {
        mockMvc.perform(post("/ajouter-relation")
                        .with(user(new UserDetailsImpl(userConnected)))
                        .with(csrf())
                        .param("email", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ajouter-relation"))
                .andExpect(flash().attribute("error", "L'email est requis"));

        assertEquals(1, userConnected.getConnections().size());
    }

    @Test
    void shouldNotAllowAddingSelf() throws Exception {

        mockMvc.perform(post("/ajouter-relation")
                        .with(user(new UserDetailsImpl(userConnected)))
                        .with(csrf())
                        .param("email", userConnected.getEmail()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ajouter-relation"))
                .andExpect(flash().attribute("error", "Vous ne pouvez pas vous ajouter vous même comme amis"));
    }

    @Test
    void shouldShowErrorWhenUserNotFound() throws Exception {
        String unknownEmail = "inconnu@example.com";

        mockMvc.perform(post("/ajouter-relation")
                        .with(user(new UserDetailsImpl(userConnected)))
                        .with(csrf())
                        .param("email", unknownEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ajouter-relation"))
                .andExpect(flash().attribute("error", "L'utilisateur avec l'email " + unknownEmail + " n'existe pas, veuillez vérifier."));
    }

    @Test
    void shouldShowErrorWhenUserAlreadyExists() throws Exception {
        String unknownEmail = "inconnu@example.com";

        mockMvc.perform(post("/ajouter-relation")
                        .with(user(new UserDetailsImpl(userConnected)))
                        .with(csrf())
                        .param("email", unknownEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ajouter-relation"))
                .andExpect(flash().attribute("error", "L'utilisateur avec l'email " + unknownEmail + " n'existe pas, veuillez vérifier."));
    }

    @Test
    void shouldShowErrorWhenUserIsAlreadyFriend() throws Exception {
        String friendEmail = userFriend.getEmail();

        mockMvc.perform(post("/ajouter-relation")
                        .with(user(new UserDetailsImpl(userConnected)))
                        .with(csrf())
                        .param("email", friendEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ajouter-relation"))
                .andExpect(flash().attribute("error", "Cette personne fait déjà partie de vos contacts : " + friendEmail + " (" + userFriend.getUsername() + ")"));
    }
}
