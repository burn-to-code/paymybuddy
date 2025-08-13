package com.paymybuddy.service;

import com.paymybuddy.security.CustomOidcUser;
import com.paymybuddy.security.UserDetailsImpl;
import com.paymybuddy.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
/**
 * Classe utilitaire pour récupérer l'utilisateur actuellement connecté (principal)
 * dans le contexte de sécurité Spring Security.
 *
 * <p>
 * Permet de gérer les utilisateurs locaux (UserDetailsImpl), OAuth2 (CustomOidcUser) et les objets User directement.
 * </p>
 */
@Slf4j
public class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Récupère l'utilisateur actuellement connecté.
     *
     * @return un Optional contenant l'utilisateur connecté, ou vide si aucun utilisateur n'est connecté
     */
    public static Optional<User> findConnectedUser() {
        final Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication != null) {
            Object principal = authentication.getPrincipal();

            log.info("Principal class: {}", principal.getClass().getName());
            switch (principal) {
                case User user -> {
                    log.debug("User '{}' is connected", user.getUsername());
                    return Optional.of(user);
                }
                case UserDetailsImpl(User user) -> {
                    log.debug("User '{}' is connected via UserDetailsImpl", user.getUsername());
                    return Optional.of(user);
                }
                case CustomOidcUser customOidcUser -> {
                    log.debug("User '{}' is connected via OAuth2UserImpl", customOidcUser.getUser().getUsername());
                    return Optional.of(customOidcUser.getUser());
                }
                default -> {
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Récupère l'utilisateur actuellement connecté.
     *
     * @return l'utilisateur connecté
     * @throws IllegalStateException si aucun utilisateur n'est connecté
     */
    public static User getConnectedUser() {
        return findConnectedUser().orElseThrow(() -> new IllegalStateException("No user connected"));
    }

    /**
     * Récupère l'ID de l'utilisateur actuellement connecté.
     *
     * @return l'ID de l'utilisateur connecté
     * @throws IllegalStateException si aucun utilisateur n'est connecté
     */
    public static Long getConnectedUserId() {
        return findConnectedUser()
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("No user connected"));
    }

    /**
     * Vérifie si un utilisateur est actuellement connecté.
     *
     * @return true si un utilisateur est connecté, false sinon
     */
    public static boolean isConnected() {
        return findConnectedUser().isPresent();
    }

}
