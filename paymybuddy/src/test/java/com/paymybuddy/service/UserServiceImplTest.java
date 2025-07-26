package com.paymybuddy.service;

import com.paymybuddy.exception.EmailConflictException;
import com.paymybuddy.exception.UsernameConflictException;
import com.paymybuddy.model.DTO.RegisterRequest;
import com.paymybuddy.model.User;
import com.paymybuddy.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private RegisterRequest request;

    @BeforeEach
    public void setUp() {
        request = new RegisterRequest();
        request.setUserName("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
    }

    @Test
    public void testRegisterUser_WhenSaveUserSucceeds() {
        // Given
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(request.getUserName())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");

        // When
        userService.registerUser(request);

        // Then
        verify(userRepository).save(userCaptor.capture());

        User user = userCaptor.getValue();

        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("encodedPassword", user.getPassword());
    }

    @Test
    public void testRegisterUser_ShouldThrowEmailConflictException() {
        // Given
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(new User()));

        // When
        EmailConflictException ex = assertThrows(
                EmailConflictException.class,
                () -> userService.registerUser(request)
        );

        assertEquals("Email déjà utilisé", ex.getMessage());
        assertEquals("register", ex.getUrlName());
        assertEquals(request, ex.getFormData());

        verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void testRegisterUser_ShouldThrowUsernameConflictException() {
        // Given
        when(userRepository.findByUsername(request.getUserName())).thenReturn(Optional.of(new User()));

        //When
        UsernameConflictException ex = assertThrows(
                UsernameConflictException.class,
                () -> userService.registerUser(request)
        );

        // Then
        assertEquals("UserName déjà utilisé", ex.getMessage());
        assertEquals("register", ex.getUrlName());
        assertEquals(request, ex.getFormData());

        verify(userRepository, Mockito.never()).save(Mockito.any());
    }

}
