package com.paymybuddy.security;

import com.paymybuddy.model.AuthProvider;
import com.paymybuddy.model.User;
import com.paymybuddy.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserDetailsServiceImplTest {

    private UserRepository userRepository;
    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userDetailsService = new UserDetailsServiceImpl(userRepository);
    }

    @Test
    void loadUserByUsername_UserExistsAndLocalAuth_ReturnsUserDetails() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("hashedPassword");
        user.setProvider(AuthProvider.LOCAL);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        assertNotNull(userDetails);
        assertEquals(user.getEmail(), userDetails.getUsername());
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    void loadUserByUsername_UserDoesNotExist_ThrowsUsernameNotFoundException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                userDetailsService.loadUserByUsername("unknown@example.com"));

        verify(userRepository, times(1)).findByEmail("unknown@example.com");
    }

    @Test
    void loadUserByUsername_UserHasNullPassword_ThrowsBadCredentialsException() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword(null);
        user.setProvider(AuthProvider.LOCAL);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThrows(BadCredentialsException.class, () ->
                userDetailsService.loadUserByUsername("test@example.com"));
    }

    @Test
    void loadUserByUsername_UserIsOAuthProvider_ThrowsBadCredentialsException() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("somePassword");
        user.setProvider(AuthProvider.GOOGLE); // pas LOCAL

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThrows(BadCredentialsException.class, () ->
                userDetailsService.loadUserByUsername("test@example.com"));
    }
}
