package com.paymybuddy.service;

import com.paymybuddy.security.CustomOidcUser;
import com.paymybuddy.security.UserDetailsImpl;
import com.paymybuddy.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Slf4j
public class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<User> findConnectedUser() {
        final Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication != null) {
            Object principal = authentication.getPrincipal();

            System.out.println("Principal class: " + principal.getClass().getName());
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

    public static User getConnectedUser() {
        return findConnectedUser().orElseThrow(() -> new IllegalStateException("No user connected"));
    }

    public static Long getConnectedUserId() {
        return findConnectedUser()
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("No user connected"));
    }

    public static boolean isConnected() {
        return findConnectedUser().isPresent();
    }

}
