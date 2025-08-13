package com.paymybuddy.service;

import com.paymybuddy.exception.EmailConflictException;
import com.paymybuddy.exception.EmailNotFoundException;
import com.paymybuddy.exception.UserNotFoundException;
import com.paymybuddy.exception.UsernameConflictException;
import com.paymybuddy.model.AuthProvider;
import com.paymybuddy.model.DTO.RegisterRequest;
import com.paymybuddy.model.DTO.UpdateUserRequest;
import com.paymybuddy.model.User;
import com.paymybuddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Service de gestion des utilisateurs.
 * Implémente les opérations liées à l'enregistrement, la mise à jour,
 * la gestion des connexions et des comptes utilisateurs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");


    /**
     * Enregistre un nouvel utilisateur.
     *
     * @param request DTO contenant les informations nécessaires : username, email et password
     *                L'username et l'email doivent être uniques.
     * @throws UsernameConflictException si le nom d'utilisateur est déjà utilisé
     * @throws EmailConflictException    si l'email existe déjà pour un compte local
     */
    @Override
    public void registerUser(RegisterRequest request) {
        log.info("Création utilisateur avec email {}", request.getEmail());
        Assert.notNull(request.getEmail(), "L'email est requis");
        Assert.notNull(request.getUserName(), "Le nom d'utilisateur est requis");
        Assert.notNull(request.getPassword(), "Le mot de passe est requis");

        if(userRepository.findByUsername(request.getUserName()).isPresent()) {
            log.warn("L'utilisateur avec le nom d'utilisateur {}, existe déjà", request.getUserName());
            throw new UsernameConflictException("UserName déjà utilisé");
        }

        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        if(userOpt.isPresent()) {
            User existingUser = userOpt.get();
            verifyProvider(existingUser, request);
            return;
        }

        User user = new User();
        user.setUsername(request.getUserName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setProvider(AuthProvider.LOCAL);

        userRepository.save(user);
    }

    /**
     * Récupère la liste des connexions (contacts) de l'utilisateur courant.
     *
     * @param userId l'identifiant de l'utilisateur
     * @return la liste des utilisateurs connectés à l'utilisateur
     * @throws UserNotFoundException si l'utilisateur avec l'id donné n'existe pas
     */
    @Override
    @Transactional
    public List<User> getListOfConnectionOfCurrentUserById(Long userId) {
        log.info("Récupération d'un utilisateur avec ses contacts avec l'id {}", userId);
        User user = userRepository.findById(userId)
                 .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable avec l'id : " + userId));
        Hibernate.initialize(user.getConnections());
        return user.getConnections();
    }

    /**
     * Ajoute une connexion entre l'utilisateur courant et un autre utilisateur identifié par son email.
     *
     * @param userConnected       l'utilisateur connecté
     * @param emailOfUserToConnect l email de l'utilisateur à ajouter
     * @throws EmailNotFoundException  si l'email fourni est vide ou null
     * @throws EmailConflictException  si l'utilisateur essaie de s'ajouter lui-même ou si la connexion existe déjà
     * @throws UserNotFoundException   si l'utilisateur à connecter n'existe pas
     */
    @Override
    @Transactional
    public void addUserConnexion(User userConnected, String emailOfUserToConnect) {
        if(!StringUtils.hasText(emailOfUserToConnect)) {
            throw new EmailNotFoundException("L'email est requis");
        }

        if(userConnected.getEmail().equals(emailOfUserToConnect)) {
            throw new EmailConflictException("Vous ne pouvez pas vous ajouter vous même comme amis");
        }

        log.info("Tentative de récupération de l'utilisateur avec l'email {} pour ajouter une connexion avec l'utilisateur {}", emailOfUserToConnect, userConnected.getEmail());
        User userToConnect = userRepository.findByEmail(emailOfUserToConnect)
                .orElseThrow(() -> new UserNotFoundException("L'utilisateur avec l'email " + emailOfUserToConnect + " n'existe pas, veuillez vérifier."));

        User userConnectedEntity = userRepository.findById(userConnected.getId())
                        .orElseThrow(() -> new UserNotFoundException("L'utilisateur avec l'id " + userConnected.getId() + " n'existe pas"));

        if(userConnectedEntity.getConnections().contains(userToConnect)) {
            throw new EmailConflictException("Cette personne fait déjà partie de vos contacts : " + emailOfUserToConnect + " (" + userToConnect.getUsername() + ")");
        }

        log.info("Ajout d'une connexion entre {} et {}", userConnected.getEmail(), emailOfUserToConnect);
        userConnectedEntity.getConnections().add(userToConnect);

        userRepository.save(userConnectedEntity);
    }

    /**
     * Met à jour les informations d'un utilisateur.
     *
     * @param request DTO contenant les champs à mettre à jour (username, email, password)
     * @param user    l'utilisateur à mettre à jour
     * @throws IllegalArgumentException  si aucune donnée à mettre à jour ou si des données sont invalides
     * @throws EmailConflictException    si l'email est déjà utilisé par un autre utilisateur
     * @throws UsernameConflictException si le nom d'utilisateur est déjà utilisé par un autre utilisateur
     * @throws UnsupportedOperationException si l'utilisateur est OAuth et tente de modifier email ou mot de passe
     */
    @Override
    @Transactional
    public void updateUser(UpdateUserRequest request, User user) {
        log.info("Début de tentative de mise à jour de l'utilisateur");
        Assert.notNull(request, "La requête ne peut être null");
        Assert.notNull(user, "L'utilisateur ne peut être null");

        User userConnected = userRepository.findById(user.getId())
                .orElseThrow(() -> new UserNotFoundException("L'utilisateur avec l'id " + user.getId() + " n'existe pas"));

        validateUpdateRequest(request);
        checkUpdateConflict(request, userConnected);
        boolean isUpdate = checkIfSaveItsOkWithUserConnectedAndRequest(request, userConnected);

        if(isUpdate) {
            userRepository.save(userConnected);
            log.info("Mise à jour de l'utilisateur {} réussie", userConnected.getId());
        }
    }

    /**
     * Effectue un dépôt sur le compte de l'utilisateur.
     *
     * @param amount le montant à déposer, doit être positif
     * @param user   l'utilisateur dont le compte est crédité
     * @throws IllegalArgumentException si le montant est négatif ou nul
     */
    @Override
    @Transactional
    public void depositOnAccount(BigDecimal amount, User user) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("le montant doit être positif");
        }

        BigDecimal amountForDeposit = amount.setScale(2, RoundingMode.HALF_UP);

        BigDecimal newAccount = amountForDeposit.add(user.getAccount());

        user.setAccount(newAccount);
        userRepository.updateAccount(user.getId(), newAccount);
    }

    // UTILITAIRES

    private void validateUpdateRequest(UpdateUserRequest request) {
        String email = request.getEmail();
        String username = request.getUsername();
        String password = request.getPassword();

        if(email.isEmpty() && username.isEmpty() && password.isEmpty()){
            throw new IllegalArgumentException("Aucune données à mettre à jour. Veuillez en choisir au moins une.");
        }

        if(!email.isEmpty() && !EMAIL_REGEX.matcher(email).matches()){
            throw new IllegalArgumentException("L'email n'est pas valide, la mis à jour n'est pas possible : " + request.getEmail() + " Veuillez écrire un mail au bon format.");
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

    private void checkUpdateConflict(UpdateUserRequest request, User userConnected) {
        String email = request.getEmail();
        String username = request.getUsername();

        // Vérifie que le mail est bien différent de celle d'origine, puis regarde si elle existe en bdd
        if(!email.equals(userConnected.getEmail()) && userRepository.findByEmail(email).isPresent() ){
            throw new EmailConflictException("L'email existe déjà : " + request.getEmail() + " Veuillez en choisir une autre.");
        }

        // Vérifie que la meme chose mais pour l'userName
        if(!username.equals(userConnected.getUsername()) && userRepository.findByUsername(username).isPresent()){
            throw new UsernameConflictException("Le nom d'utilisateur existe déjà : " + request.getUsername() + " Veuillez en choisir un autre.");
        }
    }

    private boolean checkIfSaveItsOkWithUserConnectedAndRequest(UpdateUserRequest request, User userConnected) {
        String email = request.getEmail();
        String username = request.getUsername();
        String password = request.getPassword();

        boolean isUpdate = false;

        if(userConnected.getProvider() == AuthProvider.LOCAL) {
            // Utilisateur local : peut modifier email, username, password

            if(!email.equals(userConnected.getEmail()) && !email.isBlank()) {
                log.debug("Mise à jour email: {} → {}", userConnected.getEmail(), email);
                userConnected.setEmail(email);
                isUpdate = true;
            }
            if(!username.equals(userConnected.getUsername()) && !username.isBlank()) {
                log.debug("Mise à jour username: {} → {}", userConnected.getUsername(), username);
                userConnected.setUsername(username);
                isUpdate = true;
            }
            if(password != null && !password.isBlank() && !passwordEncoder.matches(password, userConnected.getPassword())) {
                log.debug("Mise à jour mot de passe");
                userConnected.setPassword(passwordEncoder.encode(password));
                isUpdate = true;
            }
        } else {
            // Utilisateur OAuth : ne peut modifier que username

            if(!email.equals(userConnected.getEmail()) && !email.isBlank()) {
                throw new UnsupportedOperationException("Modification de l'email impossible pour un compte Google ou Facebook.");
            }
            if(password != null && !password.isBlank() && !passwordEncoder.matches(password, userConnected.getPassword())) {
                throw new UnsupportedOperationException("Modification du mot de passe impossible pour un compte Google ou Facebook.");
            }
            if(!username.equals(userConnected.getUsername()) && !username.isBlank()) {
                log.debug("Mise à jour username OAuth: {} → {}", userConnected.getUsername(), username);
                userConnected.setUsername(username);
                isUpdate = true;
            }
        }
        return isUpdate;
    }

    private void verifyProvider(User existingUser, RegisterRequest request) {
        if(existingUser.getProvider() == AuthProvider.LOCAL){
            log.warn("L'utilisateur avec l'email {}, existe déjà.", request.getEmail());
            throw new EmailConflictException("Email déjà utilisé");
        } else {
            log.info("Un utilisateur existe avec cet email via OAuth. Ajout des identifiants locaux.");
            existingUser.setUsername(request.getUserName());
            existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
            existingUser.setProvider(AuthProvider.LOCAL);
            userRepository.save(existingUser);
        }
    }
}
