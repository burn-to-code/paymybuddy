package com.paymybuddy.service;

import com.paymybuddy.exception.EmailConflictException;
import com.paymybuddy.exception.EmailNotFoundException;
import com.paymybuddy.exception.UserNotFoundException;
import com.paymybuddy.exception.UsernameConflictException;
import com.paymybuddy.model.DTO.RegisterRequest;
import com.paymybuddy.model.DTO.UpdateUserRequest;
import com.paymybuddy.model.User;
import com.paymybuddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");


    @Override
    public void registerUser(RegisterRequest request) {
        log.info("Création utilisateur avec email {}", request.getEmail());

        if(userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("L'utilisateur avec l'email {}, existe déjà.", request.getEmail());
            throw new EmailConflictException("Email déjà utilisé");
        }
        if(userRepository.findByUsername(request.getUserName()).isPresent()) {
            log.warn("L'utilisateur avec le nom d'utilisateur {}, existe déjà", request.getUserName());
            throw new UsernameConflictException("UserName déjà utilisé");
        }

        User user = new User();

        user.setUsername(request.getUserName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);
    }

    @Override
    @Transactional
    public User getCurrentUserById(Long userId) {
        log.info("Récupération d'un utilisateur avec ses contacts avec l'id {}", userId);
        return userRepository.findByIdWithConnections(userId)
                 .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable avec l'id : " + userId));
    }

    @Override
    @Transactional
    public void addUserConnexion(User userConnected, String emailOfUserToConnect) {
        if(emailOfUserToConnect == null) {
            throw new EmailNotFoundException("L'email est requis");
        }

        if(userConnected.getEmail().equals(emailOfUserToConnect)) {
            throw new IllegalArgumentException("Vous ne pouvez pas vous ajouter vous même comme amis");
        }

        log.info("Tentative de récupération de l'utilisateur avec l'email {} pour ajouter une connexion avec l'utilisateur {}", emailOfUserToConnect, userConnected.getEmail());
        User userToConnect = userRepository.findByEmailWithConnections(emailOfUserToConnect)
                .orElseThrow(() -> new EmailNotFoundException("L'utilisateur avec l'email " + emailOfUserToConnect + " n'existe pas"));

        User userConnectedEntity = userRepository.findByIdWithConnections(userConnected.getId())
                        .orElseThrow(() -> new UserNotFoundException("L'utilisateur avec l'id " + userConnected.getId() + " n'existe pas"));

        if(userConnectedEntity.getConnections().contains(userToConnect)) {
            throw new EmailConflictException("Cette personne fait déjà partie de vos contacts : " + emailOfUserToConnect + " (" + userToConnect.getUsername() + ")");
        }

        log.info("Ajout d'une connexion entre {} et {}", userConnected.getEmail(), emailOfUserToConnect);
        userConnectedEntity.getConnections().add(userToConnect);
        userToConnect.getConnections().add(userConnectedEntity);

        userRepository.save(userConnectedEntity);
        userRepository.save(userToConnect);
    }

    @Override
    @Transactional
    public void updateUser(UpdateUserRequest request, User user) {
        log.info("Début de tentative de mise à jour de l'utilisateur");
        Assert.notNull(request, "request must not be null");

        String email = request.getEmail();
        String username = request.getUsername();
        String password = request.getPassword();

        User userConnected = userRepository.findById(user.getId())
                .orElseThrow(() -> new UserNotFoundException("L'utilisateur avec l'id " + user.getId() + " n'existe pas"));

        validateRequest(request);
        checkConflict(request, userConnected);

        if(!email.equals(userConnected.getEmail()) && !email.isBlank()){
            log.debug("Mise à jour email: {} → {}", userConnected.getEmail(), email);
            userConnected.setEmail(request.getEmail());
        }
        if(!username.equals(userConnected.getUsername()) && !username.isBlank()){
            log.debug("Mise à jour username: {} → {}", userConnected.getUsername(), username);
            userConnected.setUsername(request.getUsername());
        }
        if(!passwordEncoder.matches(password, userConnected.getPassword()) && !password.isBlank()){
            log.debug("Mise à jour mot de passe");
            userConnected.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(userConnected);
        log.info("Mise à jour de l'utilisateur {} réussie", userConnected.getId());
    }

    private void validateRequest(UpdateUserRequest request) {
        String email = request.getEmail();
        String username = request.getUsername();
        String password = request.getPassword();

        if(email.isEmpty() && username.isEmpty() && password.isEmpty()){
            throw new IllegalArgumentException("Aucune données à mettre à jour. Veuillez en choisir au moins une.");
        }

        if(!email.isEmpty() && !EMAIL_REGEX.matcher(email).matches()){
            throw new IllegalArgumentException("L'email n'est pas valide, la mis à jour n'est pas possible : " + request.getEmail() + " Veuillez en choisir un autre.");
        }

        // Vérifier username n'est pas remplie d'espace
        if(!username.isEmpty() && username.trim().isEmpty()){
            throw new IllegalArgumentException("Vous ne pouvez pas choisir un nom d'utilisateur vide. Veuillez en choisir un autre.");
        }

        // Vérifier que le password n'est pas simplement remplie d'espace
        if(!password.isEmpty() && password.trim().isEmpty()){
            throw new IllegalArgumentException("Vous ne pouvez pas choisir un mot de passe vide. Veuillez en choisir un autre.");
        }
    }

    private void checkConflict(UpdateUserRequest request, User userConnected) {
        String email = request.getEmail();
        String username = request.getUsername();

        // Vérifie que le mail est bien différent de celle d'origine, puis regarde si elle existe en bdd
        if(!email.equals(userConnected.getEmail()) && userRepository.findByEmail(email).isPresent() ){
            throw new EmailConflictException("L'email existe déjà : " + request.getUsername() + " Veuillez en choisir une autre.");
        }

        // Vérifie que la meme chose mais pour l'userName
        if(!username.equals(userConnected.getUsername()) && userRepository.findByUsername(username).isPresent()){
            throw new UsernameConflictException("Le nom d'utilisateur existe déjà : " + request.getUsername() + " Veuillez en choisir un autre.");
        }
    }
}
