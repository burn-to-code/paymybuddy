package com.paymybuddy.controllerIT;

import com.paymybuddy.model.AuthProvider;
import com.paymybuddy.model.User;
import com.paymybuddy.repository.UserRepository;
import com.paymybuddy.security.UserDetailsImpl;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
public class RegisterControllerIT {

    private final String goodEmail = "good@email.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        User user = new User();
        user.setEmail(goodEmail);
        user.setUsername("TestUser");
        String goodPassword = "goodPassword";
        user.setPassword(passwordEncoder.encode(goodPassword));
        user.setProvider(AuthProvider.LOCAL);
        userRepository.save(user);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldShowProfilePageWithRequestAttribute() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("request"))
                .andExpect(model().attribute("request", allOf(
                        hasProperty("email", nullValue()),
                        hasProperty("password", nullValue()),
                        hasProperty("userName", nullValue())
                )));
    }

    @Test
    public void shouldRegisterUserWhenEverythingIsOkay() throws Exception {
        mockMvc.perform(post("/register")
                .param("email", "newEmail@gmail.com")
                .param("password", "newPassword")
                .param("userName", "newUsername")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("success",
                        "Votre inscription à été enregistré ! Veuillez vous connecté pour accéder à votre compte."));
    }

    @ParameterizedTest
    @CsvSource({
           "invalidEmail, password123, newUsername, L'email est invalide",
            "'', password123, newUsername, L'email est requis",
            "newEmail@gmail.com, '', NewUsername, Le mot de passe est requis",
            "newEmail@gmail.com, 'password123', '', L'userName est requis"
    })
    public void shouldRedirectToRegisterWithFlashErrorWhenInputIsInvalid(String email, String password, String userName, String messageError) throws Exception {
        mockMvc.perform(post("/register")
                .param("email", email)
                .param("password", password)
                .param("userName", userName)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("errors", Matchers.hasItem(messageError)));
    }

    @Test
    public void shouldSaveRegisterUserButEmailAlreadyExist() throws Exception {
        mockMvc.perform(post("/register")
                .param("email", goodEmail)
                .param("password", "newPassword")
                .param("userName", "newUsername")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("error", "Email déjà utilisé"));
    }

    @Test
    public void shouldSaveRegisterUserButUsernameAlreadyExist() throws Exception {
        mockMvc.perform(post("/register")
                        .param("email", "newMail@gmail.com")
                        .param("password", "newPassword")
                        .param("userName", "TestUser")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("error", "UserName déjà utilisé"));
    }

    @Test
    void shouldRedirectAuthenticatedUserFromRegisterPage() throws Exception {
        User user = userRepository.findByEmail(goodEmail).orElseThrow();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken(new UserDetailsImpl(user), null));
        SecurityContextHolder.setContext(context);

        mockMvc.perform(get("/register"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transferer"));
    }

    @Test
    void shouldPersistUserCorrectly() throws Exception {
        mockMvc.perform(post("/register")
                        .param("email", "check@db.com")
                        .param("password", "123456")
                        .param("userName", "DBTestUser")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        User saved = userRepository.findByEmail("check@db.com").orElse(null);
        assertNotNull(saved);
        assertEquals("DBTestUser", saved.getUsername());
        assertTrue(passwordEncoder.matches("123456", saved.getPassword()));
    }

}
