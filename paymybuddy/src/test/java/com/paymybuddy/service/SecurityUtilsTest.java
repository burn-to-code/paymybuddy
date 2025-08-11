package com.paymybuddy.service;

import com.paymybuddy.model.User;
import com.paymybuddy.security.CustomOidcUser;
import com.paymybuddy.security.UserDetailsImpl;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    private MockedStatic<SecurityContextHolder> securityContextHolderMock;

    @BeforeEach
    void setup() {
        securityContextHolderMock = Mockito.mockStatic(SecurityContextHolder.class);
    }

    @AfterEach
    void teardown() {
        securityContextHolderMock.close();
    }

    private void mockAuthentication(Object principal) {
        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication.getPrincipal()).thenReturn(principal);

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
    }

    @Test
    void findConnectedUser_withUserPrincipal_returnsUser() {
        User user = new User();
        user.setUsername("user1");

        mockAuthentication(user);

        Optional<User> result = SecurityUtils.findConnectedUser();
        assertTrue(result.isPresent());
        assertEquals("user1", result.get().getUsername());
    }

    @Test
    void findConnectedUser_withUserDetailsImpl_returnsUser() {
        User user = new User();
        user.setUsername("user2");

        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        mockAuthentication(userDetails);

        Optional<User> result = SecurityUtils.findConnectedUser();
        assertTrue(result.isPresent());
        assertEquals("user2", result.get().getUsername());
    }

    @Test
    void findConnectedUser_withCustomOidcUser_returnsUser() {
        User user = new User();
        user.setUsername("user3");

        OidcUser oidcUserMock = Mockito.mock(OidcUser.class);

        CustomOidcUser oidcUser = new CustomOidcUser(oidcUserMock, user);

        mockAuthentication(oidcUser);

        Optional<User> result = SecurityUtils.findConnectedUser();
        assertTrue(result.isPresent());
        assertEquals("user3", result.get().getUsername());
    }

    @Test
    void findConnectedUser_withOtherPrincipal_returnsEmpty() {
        mockAuthentication("anonymousUser");

        Optional<User> result = SecurityUtils.findConnectedUser();
        assertTrue(result.isEmpty());
    }

    @Test
    void findConnectedUser_withNullAuthentication_returnsEmpty() {
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(null);

        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        Optional<User> result = SecurityUtils.findConnectedUser();
        assertTrue(result.isEmpty());
    }

    @Test
    void findConnectedUser_withNullContext_returnsEmpty() {
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(null);

        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        Optional<User> result = SecurityUtils.findConnectedUser();
        assertTrue(result.isEmpty());
    }

    @Test
    void getConnectedUser_returnsUser_whenPresent() {
        User user = new User();
        user.setUsername("user4");

        mockAuthentication(user);

        User result = SecurityUtils.getConnectedUser();
        assertEquals("user4", result.getUsername());
    }

    @Test
    void getConnectedUser_throwsException_whenNotPresent() {
        mockAuthentication("anonymousUser");

        IllegalStateException ex = assertThrows(IllegalStateException.class, SecurityUtils::getConnectedUser);
        assertEquals("No user connected", ex.getMessage());
    }

    @Test
    void getConnectedUserId_returnsId_whenPresent() {
        User user = new User();
        user.setId(42L);

        mockAuthentication(user);

        Long id = SecurityUtils.getConnectedUserId();
        assertEquals(42L, id);
    }

    @Test
    void getConnectedUserId_throwsException_whenNotPresent() {
        mockAuthentication("anonymousUser");

        IllegalStateException ex = assertThrows(IllegalStateException.class, SecurityUtils::getConnectedUserId);
        assertEquals("No user connected", ex.getMessage());
    }

    @Test
    void isConnected_returnsTrue_whenUserPresent() {
        User user = new User();
        mockAuthentication(user);

        assertTrue(SecurityUtils.isConnected());
    }

    @Test
    void isConnected_returnsFalse_whenNoUser() {
        mockAuthentication("anonymousUser");

        assertFalse(SecurityUtils.isConnected());
    }
}
