package com.paymybuddy.controllerIT;

import com.paymybuddy.model.AuthProvider;
import com.paymybuddy.model.User;
import com.paymybuddy.repository.UserRepository;
import com.paymybuddy.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test",
        "spring.security.oauth2.client.registration.google.client-secret=test"
})
public class UpdateProfilControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User userConnected;
    private User userAlreadyExist;
    private User userOAuth;

    final static String EMAIL_CONNECTED = "userConnected@email.com";
    final static String EMAIL_USER_ALREADY_EXIST = "emailAlreadyExist@test.com";
    final static String EMAIL_OAUTH = "emailOAuth@gmail.com";

    final static String USERNAME_CONNECTED = "Connected";
    final static String USERNAME_ALREADY_EXIST = "AlreadyExist";
    final static String USERNAME_OAUTH = "OAuthUser";

    final static String PASSWORD = "Password";

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();

        userAlreadyExist = new User();
        userAlreadyExist.setEmail(EMAIL_USER_ALREADY_EXIST);
        userAlreadyExist.setUsername(USERNAME_ALREADY_EXIST);
        userAlreadyExist.setAccount(new BigDecimal(100));
        userAlreadyExist.setPassword(passwordEncoder.encode(PASSWORD));
        userAlreadyExist.setProvider(AuthProvider.LOCAL);

        userOAuth = new User();
        userOAuth.setEmail(EMAIL_OAUTH);
        userOAuth.setUsername(USERNAME_OAUTH);
        userOAuth.setAccount(new BigDecimal(100));
        userOAuth.setPassword(null);
        userOAuth.setProvider(AuthProvider.GOOGLE);
        userOAuth.setConnections(new ArrayList<>(List.of()));

        userConnected = new User();
        userConnected.setEmail(EMAIL_CONNECTED);
        userConnected.setUsername(USERNAME_CONNECTED);
        userConnected.setAccount(new BigDecimal(100));
        userConnected.setPassword(passwordEncoder.encode(PASSWORD));
        userConnected.setProvider(AuthProvider.LOCAL);

        userAlreadyExist.setConnections(new ArrayList<>(List.of()));
        userConnected.setConnections(new ArrayList<>(List.of(userAlreadyExist)));

        userRepository.save(userConnected);
        userRepository.save(userAlreadyExist);
        userRepository.save(userOAuth);
    }

    @Test
    void printPageUpdateProfilWithCorrectlyObjectAndWebPage () throws Exception {
        mockMvc.perform(get("/profil")
                        .with(user(new UserDetailsImpl(userConnected))))
                .andExpect(status().isOk())
                .andExpect(view().name("profil"))
                .andExpect(model().attributeExists("request"))
                .andExpect(model().attribute("request", allOf(
                        hasProperty("username", nullValue()),
                        hasProperty("email", nullValue()),
                        hasProperty("password", nullValue())
                )));
    }

    @Test
    @WithAnonymousUser
    public void shouldRedirectToLoginPageWhenNotConnected() throws Exception {
        mockMvc.perform(get("/profil"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void shouldUpdateProfilWhenEverythingIsOk() throws Exception {
        String newUsername = "newUsername";
        String newMail = "newAdresse@test.com";
        String newPassword = "newPassword";
        mockMvc.perform(post("/profil/update")
                .with(csrf())
                .with(user(new UserDetailsImpl(userConnected)))
                .param("username", newUsername)
                .param("email", newMail)
                .param("password", newPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profil"))
                .andExpect(flash().attribute("success", "les modifications ont bien été enregistrés"));

        assertEquals(newUsername, userConnected.getUsername());
        assertEquals(newMail, userConnected.getEmail());
        assertTrue(passwordEncoder.matches(newPassword, userConnected.getPassword()));
    }

    @Test
    void shouldUpdateProfilWhenJustOneParameterHasChange() throws Exception {
        String newUsername = "newUsername";
        String mail = userConnected.getEmail();

        mockMvc.perform(post("/profil/update")
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userConnected)))
                        .param("username", newUsername)
                        .param("email", "")
                        .param("password", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profil"))
                .andExpect(flash().attribute("success", "les modifications ont bien été enregistrés"));

        assertEquals(newUsername, userConnected.getUsername());
        assertEquals(mail, userConnected.getEmail());
    }

    @Test
    void shouldUpdateProfilButAnythingAtChange() throws Exception {
        String username = userConnected.getUsername();
        String mail = userConnected.getEmail();
        String password = userConnected.getPassword();

        mockMvc.perform(post("/profil/update")
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userConnected)))
                        .param("username", "")
                        .param("email", "")
                        .param("password", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profil"))
                .andExpect(flash().attribute("error", "Aucune données à mettre à jour. Veuillez en choisir au moins une."));

        User updatedUser = userRepository.findById(userConnected.getId()).orElseThrow();

        assertEquals(username, updatedUser.getUsername());
        assertEquals(mail, updatedUser.getEmail());
        assertEquals(password, updatedUser.getPassword());
    }

    @Test
    void shouldThrowExceptionWhenParametersAreOnlySpaces() throws Exception {
        String mail = userConnected.getEmail();

        String emailWithOnlySpaces = "     ";

        mockMvc.perform(post("/profil/update")
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userConnected)))
                        .param("username", "")
                        .param("email", emailWithOnlySpaces)
                        .param("password", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profil"))
                .andExpect(flash().attribute("error", "L'email n'est pas valide, la mis à jour n'est pas possible : " + emailWithOnlySpaces + " Veuillez écrire un mail au bon format."));

        User updatedUser = userRepository.findById(userConnected.getId()).orElseThrow();

        assertEquals(mail, updatedUser.getEmail());
    }

    @Test
    void shouldThrowExceptionWhenEmailIsNotAGoodFormat() throws Exception {
        String mail = userConnected.getEmail();

        String emailNotGoodFormat = "testIsNotMail";

        mockMvc.perform(post("/profil/update")
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userConnected)))
                        .param("username", "")
                        .param("email", emailNotGoodFormat)
                        .param("password", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profil"))
                .andExpect(flash().attribute("error", "L'email n'est pas valide, la mis à jour n'est pas possible : " + emailNotGoodFormat + " Veuillez écrire un mail au bon format."));

        User updatedUser = userRepository.findById(userConnected.getId()).orElseThrow();

        assertEquals(mail, updatedUser.getEmail());
    }

    @Test
    void shouldThrowExceptionWhenEmailIsAlreadyExist() throws Exception {
        String mailAlreadyExist = userAlreadyExist.getEmail();

        String emailConnected = userConnected.getEmail();

        mockMvc.perform(post("/profil/update")
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userConnected)))
                        .param("username", "")
                        .param("email", mailAlreadyExist)
                        .param("password", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profil"))
                .andExpect(flash().attribute("error", "L'email existe déjà : " + mailAlreadyExist + " Veuillez en choisir une autre."));

        User updatedUser = userRepository.findById(userConnected.getId()).orElseThrow();

        assertEquals(emailConnected, updatedUser.getEmail());
    }

    @Test
    void shouldThrowExceptionWhenUsernameIsAlreadyExist() throws Exception {
        String usernameAlreadyExist = userAlreadyExist.getUsername();

        String usernameConnected = userConnected.getUsername();

        mockMvc.perform(post("/profil/update")
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userConnected)))
                        .param("username", usernameAlreadyExist)
                        .param("email", "")
                        .param("password", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profil"))
                .andExpect(flash().attribute("error", "Le nom d'utilisateur existe déjà : " + usernameAlreadyExist + " Veuillez en choisir un autre."));

        User updatedUser = userRepository.findById(userConnected.getId()).orElseThrow();

        assertEquals(usernameConnected, updatedUser.getUsername());
    }

    @Test
    void shouldThrowExceptionWhenOAuthUserTryToChangeMail() throws Exception {
        String mail = userOAuth.getEmail();

        String newMail = "newmail@gmail.com";

        mockMvc.perform(post("/profil/update")
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userOAuth)))
                        .param("username", "")
                        .param("email", newMail)
                        .param("password", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profil"))
                .andExpect(flash().attribute("error", "Modification de l'email impossible pour un compte Google ou Facebook."));

        User updatedUser = userRepository.findById(userOAuth.getId()).orElseThrow();

        assertEquals(mail, updatedUser.getEmail());
    }


    @Test
    void shouldThrowExceptionWhenOAuthUserTryToChangePassword() throws Exception {
        String password = userOAuth.getPassword();

        String newPassword = "newPassword";

        mockMvc.perform(post("/profil/update")
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userOAuth)))
                        .param("username", "")
                        .param("email", "")
                        .param("password", newPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profil"))
                .andExpect(flash().attribute("error", "Modification du mot de passe impossible pour un compte Google ou Facebook."));

        User updatedUser = userRepository.findById(userOAuth.getId()).orElseThrow();

        assertEquals(password, updatedUser.getPassword());
    }

    @Test
    void shouldUpdateProfilWhenOAuthUserTryToChangeUsername() throws Exception {
        String newUsername = "newUsername";

        mockMvc.perform(post("/profil/update")
                        .with(csrf())
                        .with(user(new UserDetailsImpl(userOAuth)))
                        .param("username", newUsername)
                        .param("email", "")
                        .param("password", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profil"))
                .andExpect(flash().attribute("success", "les modifications ont bien été enregistrés"));

        User updatedUser = userRepository.findById(userOAuth.getId()).orElseThrow();

        assertEquals(newUsername, updatedUser.getUsername());
    }








}
