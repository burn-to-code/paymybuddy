package com.paymybuddy.security;

import com.paymybuddy.model.AuthProvider;
import com.paymybuddy.model.User;
import com.paymybuddy.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service chargé de récupérer les informations d'un utilisateur pour l'authentification Spring Security.
 * Implémente UserDetailsService pour permettre la connexion locale par email/mot de passe.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Charge un utilisateur par son email pour l'authentification Spring Security.
     *
     * <p>
     * Vérifie que l'utilisateur existe et que son compte est un compte local
     * (non OAuth2) avec un mot de passe défini.
     * </p>
     *
     * @param email l'email de l'utilisateur à authentifier
     * @return un objet UserDetails représentant l'utilisateur pour Spring Security
     * @throws UsernameNotFoundException si aucun utilisateur avec cet email n'existe
     * @throws BadCredentialsException   si l'utilisateur existe mais n'a pas de compte local valide
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getPassword() == null || user.getProvider() != AuthProvider.LOCAL) {
            throw new BadCredentialsException("Connexion locale non autorisée pour ce compte.");
        }

        return new UserDetailsImpl(user);
    }
}
