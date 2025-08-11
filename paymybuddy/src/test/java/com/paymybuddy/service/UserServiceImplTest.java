package com.paymybuddy.service;

import com.paymybuddy.exception.EmailConflictException;
import com.paymybuddy.exception.EmailNotFoundException;
import com.paymybuddy.exception.UserNotFoundException;
import com.paymybuddy.exception.UsernameConflictException;
import com.paymybuddy.model.DTO.RegisterRequest;
import com.paymybuddy.model.DTO.UpdateUserRequest;
import com.paymybuddy.model.User;
import com.paymybuddy.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

        verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void getCurrentUserById_WhenEmailExists() {
        String email = "existing@email.com";
        Long id = 1L;
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        when(userRepository.findByIdWithConnections(id)).thenReturn(Optional.of(user));

        User currentUser = userService.getCurrentUserById(id);

        assertNotNull(currentUser);
        assertEquals(1L, currentUser.getId());
        assertEquals(email, currentUser.getEmail());
        verify(userRepository).findByIdWithConnections(id);
    }

    @Test
    public void getCurrentUserById_WhenIdNotFound() {
        // GIVEN
        Long id = 1L;
        when(userRepository.findByIdWithConnections(id)).thenReturn(Optional.empty());

        //WHEN
        UserNotFoundException ex = assertThrows(UserNotFoundException.class, () -> userService.getCurrentUserById(id));

        // THEN
        assertEquals("Utilisateur introuvable avec l'id : 1", ex.getMessage());

        verify(userRepository).findByIdWithConnections(id);
    }

    @Test
    public void addUserConnexion_WhenEmailUserToConnect_IsNull() {
        Long id = 1L;
        User user = new User();
        user.setId(id);
        user.setEmail("test@example.com");
        user.setPassword("password");

        EmailNotFoundException ex = assertThrows(EmailNotFoundException.class, () -> userService.addUserConnexion(user, null));

        assertEquals("L'email est requis", ex.getMessage());

        verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void addUserConnexion_WhenEmailUserToConnect_IsEqualsToCurrentUser() {
        String email = "<EMAIL>";
        Long id = 1L;
        User user = new User();
        user.setId(id);
        user.setEmail("<EMAIL>");
        user.setPassword("password");

        EmailConflictException ex = assertThrows(EmailConflictException.class, () -> userService.addUserConnexion(user, email));

        assertEquals("Vous ne pouvez pas vous ajouter vous même comme amis", ex.getMessage());
        verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void addUserConnexion_WhenEmailUserToConnect_DoesNotExist() {
        String email = "test@example.com";
        Long id = 1L;
        User user = new User();
        user.setId(id);
        user.setEmail("<EMAIL>");
        user.setPassword("password");

        when(userRepository.findByEmailWithConnections(email)).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class, () -> userService.addUserConnexion(user, email));

        assertEquals("L'utilisateur avec l'email " + email + " n'existe pas, veuillez vérifier.", ex.getMessage());
        verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void addUserConnexion_WhenCurrentUser_DoesntExistInDatabase() {
        String email = "example@test.com";
        Long id = 1L;

        User user = new User();
        user.setId(id);
        user.setEmail("test@example.com");
        user.setPassword("password");

        User userToConnect = new User();
        userToConnect.setId(3L);
        userToConnect.setEmail(email);
        userToConnect.setPassword("password");

        when(userRepository.findByEmailWithConnections(email)).thenReturn(Optional.of(userToConnect));
        when(userRepository.findByIdWithConnections(user.getId())).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class, () -> userService.addUserConnexion(user, email));

        assertEquals("L'utilisateur avec l'id " + user.getId() + " n'existe pas", ex.getMessage());
        verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void addUserConnexion_WhenUserToConnect_AlreadyExistInConnexionOfCurrentUser() {
        String email = "example@test.com";
        String username = "Patrick";
        Long id = 1L;

        User userToConnect = new User();
        userToConnect.setUsername(username);
        userToConnect.setId(3L);
        userToConnect.setEmail(email);
        userToConnect.setPassword("password");

        User user = new User();
        user.setId(id);
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setConnections(List.of(userToConnect));

        when(userRepository.findByEmailWithConnections(email)).thenReturn(Optional.of(userToConnect));
        when(userRepository.findByIdWithConnections(user.getId())).thenReturn(Optional.of(user));

        EmailConflictException ex = assertThrows(EmailConflictException.class, () -> userService.addUserConnexion(user, email));

        assertEquals("Cette personne fait déjà partie de vos contacts : " + email + " (" + username + ")", ex.getMessage());
    }

    @Test
    public void addUserConnexion_WhenEverythingIsOk() {
        String email = "example@test.com";
        User userConnected = new User();
        userConnected.setId(1L);
        userConnected.setEmail("test@example.com");
        userConnected.setConnections(new ArrayList<>());

        User userToConnect = new User();
        userToConnect.setId(3L);
        userToConnect.setEmail(email);
        userToConnect.setConnections(new ArrayList<>());

        when(userRepository.findByEmailWithConnections(email)).thenReturn(Optional.of(userToConnect));
        when(userRepository.findByIdWithConnections(userConnected.getId())).thenReturn(Optional.of(userConnected));

        userService.addUserConnexion(userConnected, email);

        assertTrue(userConnected.getConnections().contains(userToConnect));
        assertFalse(userToConnect.getConnections().contains(userConnected));

        verify(userRepository, times(1)).save(userConnected);
        verify(userRepository, times(0)).save(userToConnect);
    }

    @Test
    public void updateUser_WhenUserToSave_IsNull() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("newUsername");
        request.setEmail("newEmail");
        request.setPassword("<PASSWORD>");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(request, null));

        assertEquals("L'utilisateur ne peut être null", ex.getMessage());
        verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void updateUser_WhenTheRequest_IsNull() {
        User user = new User();
        user.setId(1L);
        user.setEmail("email@example.com");
        user.setUsername("username");
        user.setPassword("password");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(null, user));

        assertEquals("La requête ne peut être null", ex.getMessage());
        verify(userRepository, Mockito.never()).save(Mockito.any());
    }


    @Test
    public void updateUser_WhenUserToSave_IsNotExistInDatabase() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("newUsername");
        request.setEmail("test@example.com");
        request.setPassword("<PASSWORD>");

        User user = new User();
        user.setId(1L);
        user.setEmail("email@example.com");
        user.setUsername("username");
        user.setPassword("password");

        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class, () -> userService.updateUser(request, user));

        assertEquals("L'utilisateur avec l'id " + user.getId() + " n'existe pas", ex.getMessage());
        verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void updateUser_WhenTheRequest_ContainsAllDataEmpty() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("");
        request.setEmail("");
        request.setPassword("");

        User user = new User();
        user.setId(1L);
        user.setEmail("email@example.com");
        user.setUsername("username");
        user.setPassword("password");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(request, user));

        assertEquals("Aucune données à mettre à jour. Veuillez en choisir au moins une.", ex.getMessage());
        verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void updateUser_WhenTheRequest_ContainsEmailDataButNotEmailValidFormat() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("");
        request.setEmail("NotAnEmail");
        request.setPassword("");

        User user = new User();
        user.setId(1L);
        user.setEmail("email@example.com");
        user.setUsername("username");
        user.setPassword("password");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(request, user));

        assertEquals("L'email n'est pas valide, la mis à jour n'est pas possible : " + request.getEmail() + " Veuillez écrire un mail au bon format.", ex.getMessage());
        verify(userRepository, Mockito.never()).save(Mockito.any());
    }


    @Test
    public void updateUser_WhenTheRequest_ContainsUsernameDataButUsernameContainsOnlySpaces() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("     ");
        request.setEmail("");
        request.setPassword("");

        User user = new User();
        user.setId(1L);
        user.setEmail("email@example.com");
        user.setUsername("username");
        user.setPassword("password");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(request, user));

        assertEquals("Vous ne pouvez pas choisir un nom d'utilisateur vide. Veuillez en choisir un autre.", ex.getMessage());
        verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void updateUser_WhenTheRequest_ContainsPasswordDataButPasswordContainsOnlySpaces() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("");
        request.setEmail("");
        request.setPassword("     ");

        User user = new User();
        user.setId(1L);
        user.setEmail("email@example.com");
        user.setUsername("username");
        user.setPassword("password");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(request, user));

        assertEquals("Vous ne pouvez pas choisir un mot de passe vide. Veuillez en choisir un autre.", ex.getMessage());
        verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void updateUser_WhenTheNewMailAlreadyExists() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("newUsername");
        request.setEmail("emaiAlreadyExist@example.com");
        request.setPassword("<PASSWORD>");

        User user = new User();
        user.setId(1L);
        user.setEmail("email@example.com");
        user.setUsername("username");
        user.setPassword("password");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(new User()));

        EmailConflictException ex = assertThrows(EmailConflictException.class, () -> userService.updateUser(request, user));

        assertEquals("L'email existe déjà : " + request.getEmail() + " Veuillez en choisir une autre.", ex.getMessage());
        verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void updateUser_WhenTheNewUsernameAlreadyExists() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("UsernameAlreadyExist");
        request.setEmail("email@example.com");
        request.setPassword("<PASSWORD>");

        User user = new User();
        user.setId(1L);
        user.setEmail("email@example.com");
        user.setUsername("username");
        user.setPassword("password");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.of(new User()));

        UsernameConflictException ex = assertThrows(UsernameConflictException.class, () -> userService.updateUser(request, user));

        assertEquals("Le nom d'utilisateur existe déjà : " + request.getUsername() + " Veuillez en choisir un autre.", ex.getMessage());
        verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void updateUser_WhenTheEmailIsExactlySameBeforeUpdate_verifyWithSaveIfNeverCalled() {
        String email = "emailSame@example.com";
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("");
        request.setEmail(email);
        request.setPassword("");

        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setUsername("username");
        user.setPassword("password");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userService.updateUser(request, user);

        verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void updateUser_WhenTheUsernameIsExactlySameBeforeUpdate_verifyIfSaveIsNeverCalled() {
        String username = "usernameSame";
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername(username);
        request.setEmail("");
        request.setPassword("");

        User user = new User();
        user.setId(1L);
        user.setEmail("example@test.com");
        user.setUsername(username);
        user.setPassword("password");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userService.updateUser(request, user);

        verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void updateUser_WhenThePasswordIsExactlySameBeforeUpdate_verifyIfSaveIsNeverCalled() {
        String password = "password";
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("");
        request.setEmail("");
        request.setPassword(password);

        User user = new User();
        user.setId(1L);
        user.setEmail("example@test.com");
        user.setUsername("username");
        user.setPassword(password);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, password)).thenReturn(true);

        userService.updateUser(request, user);

        verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void updateUser_EverythingIsOk() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("new@example.com");
        request.setUsername("newUsername");
        request.setPassword("newPassword123");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("old@example.com");
        existingUser.setUsername("oldUsername");
        existingUser.setPassword("encodedOldPassword");

        when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(passwordEncoder.matches("newPassword123", "encodedOldPassword")).thenReturn(false);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");

        userService.updateUser(request, existingUser);

        assertEquals("new@example.com", existingUser.getEmail());
        assertEquals("newUsername", existingUser.getUsername());
        assertEquals("encodedNewPassword", existingUser.getPassword());

        verify(userRepository, times(1)).save(existingUser);
    }

    @Test
    void depositOnAccount_WhenEverythingIsOk() {
        Long id = 1L;
        User user = new User();
        user.setId(id);
        user.setEmail("<EMAIL>");
        user.setPassword("password");
        user.setAccount(new BigDecimal(0));

        userService.depositOnAccount(new BigDecimal("100"), user);

        assertEquals(new BigDecimal("100"), user.getAccount());
        verify(userRepository, times(1)).updateAccount(user.getId(), new BigDecimal("100"));
    }

    @Test
    void depositOnAccount_WhenAmountIsZeroOrNegative_ShouldThrow() {
        User user = new User();
        user.setId(1L);
        user.setAccount(BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> userService.depositOnAccount(BigDecimal.ZERO, user));

        assertThrows(IllegalArgumentException.class, () -> userService.depositOnAccount(new BigDecimal("-10"), user));
    }
}
