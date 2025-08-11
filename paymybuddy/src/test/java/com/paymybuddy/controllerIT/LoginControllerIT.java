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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


import static org.hamcrest.Matchers.containsString;
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
public class LoginControllerIT {

    private static final String ERROR_INVALID_CREDENTIALS = "Email ou mot de passe incorrect";

    private final String goodEmail = "good@email.com";
    private final String goodPassword = "goodPassword";

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
        user.setPassword(passwordEncoder.encode(goodPassword));
        user.setProvider(AuthProvider.LOCAL);
        userRepository.save(user);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }


    @Test
    @WithAnonymousUser
    void shouldReturnLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().contentType(MediaType.TEXT_HTML_VALUE + ";charset=UTF-8"));
    }

    @Test
    @WithAnonymousUser
    void shouldRedirectToLoginWhenAccessingProtectedPage() throws Exception {
        mockMvc.perform(get("/transferer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithAnonymousUser
    void shouldFailLoginWithInvalidCredentials() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", "mauvaix@email.com")
                        .param("password", "fauxMotDePasse")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        mockMvc.perform(get("/login")
                        .param("error", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(containsString(ERROR_INVALID_CREDENTIALS)));
    }

    @Test
    @WithAnonymousUser
    void shouldFailLoginWithEmptyCredentials() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", "")
                        .param("password", "")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @WithAnonymousUser
    void shouldFailLoginWithInvalidEmailFormat() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", "invalidEmail")
                        .param("password", "password123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }



    @Test
    @WithAnonymousUser
    void shouldLoginSuccessfullyWithLocalUser() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", goodEmail)
                        .param("password", goodPassword)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transferer"));
    }

    @Test
    @WithAnonymousUser
    void shouldRejectLoginIfProviderIsNotLocal() throws Exception {
        User oauthUser = new User();
        oauthUser.setUsername("OAuthUser");
        oauthUser.setEmail("oauth@user.com");
        oauthUser.setPassword(passwordEncoder.encode("irrelevant"));
        oauthUser.setProvider(AuthProvider.GOOGLE);
        userRepository.save(oauthUser);

        mockMvc.perform(post("/login")
                        .param("email", "oauth@user.com")
                        .param("password", "irrelevant")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        mockMvc.perform(get("/login")
                        .param("error", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(containsString(ERROR_INVALID_CREDENTIALS)));
    }

    @Test
    @WithAnonymousUser
    void shouldShowLoginPageWithErrorMessage() throws Exception {
        mockMvc.perform(get("/login")
                        .param("error", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(containsString(ERROR_INVALID_CREDENTIALS)));
    }

    @Test
    @WithAnonymousUser
    void shouldRejectLoginWithoutCsrf() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", goodEmail)
                        .param("password", goodPassword))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRedirectAuthenticatedUserFromLoginPage() throws Exception {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@example.com");

        UserDetailsImpl userDetails = new UserDetailsImpl(mockUser);

        TestingAuthenticationToken auth = new TestingAuthenticationToken(userDetails, null, "ROLE_USER");
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transferer"));
    }


    @Test
    @WithAnonymousUser
    void shouldAllowAccessToStaticResources() throws Exception {
        mockMvc.perform(get("/css/login.css"))
                .andExpect(status().isOk());
    }
}
