package com.paymybuddy.service;

import com.paymybuddy.config.UserDetailsImpl;
import com.paymybuddy.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Slf4j
public final class SecurityUtils {


    private SecurityUtils() {
    }

    public static Optional<User> findConnectedUser() {
        final Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication != null) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof User user) {
                log.debug("User '{}' is connected", user.getUsername());
                System.out.println("User '" + user.getUsername() + "' is connected");
                return Optional.of(user);
            } else if (principal instanceof UserDetailsImpl(User user)) {
                log.debug("User '{}' is connected via UserDetailsImpl", user.getUsername());
                System.out.println("User '" + user.getUsername() + "' is connected via UserDetailsImpl");
                return Optional.of(user);
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
