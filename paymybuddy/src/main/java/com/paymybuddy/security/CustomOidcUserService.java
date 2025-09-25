package com.paymybuddy.security;

import com.paymybuddy.model.AuthProvider;
import com.paymybuddy.model.User;
import com.paymybuddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Random;

/**
 * Service OIDC personnalisé pour gérer l'authentification OAuth2 (ex. Google) dans PayMyBuddy.
 *
 * <p>
 * Charge les informations utilisateur depuis le fournisseur OAuth2, crée un nouvel utilisateur
 * si nécessaire, et gère les conflits avec les comptes locaux.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    /**
     * Charge un utilisateur à partir de la requête OIDC.
     *
     * <p>
     * Vérifie que l'email est fourni par le fournisseur OAuth2, crée un utilisateur
     * si nécessaire, et empêche les conflits avec les comptes locaux.
     * </p>
     *
     * @param request la requête OIDC contenant les informations de l'utilisateur
     * @return un OidcUser enrichi avec l'utilisateur de l'application
     * @throws OAuth2AuthenticationException si l'email n'est pas fourni ou si un compte local existe
     */
    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser OidcUser = super.loadUser(request);

        String email = OidcUser.getAttribute("email");
        String username = OidcUser.getAttribute("name");
        String provider = request.getClientRegistration().getRegistrationId();

        if (!StringUtils.hasText(email)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_email"), "Email is required from OAuth2 provider");
        }

        final User user = userRepository.findByEmail(email)
                .orElse(createNewUser(email, username, provider));

        if (user.getProvider() == AuthProvider.LOCAL) {
                throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"),
                        "Un compte local existe déjà pour cet email. Veuillez vous connecter avec votre email et mot de passe.");
        }

        user.setProvider(AuthProvider.valueOf(provider.toUpperCase()));

        userRepository.save(user);

        return new CustomOidcUser(OidcUser, user);
    }

    /**
     * Crée un nouvel utilisateur avec email et username unique.
     *
     * @param email    l'email de l'utilisateur
     * @param username le nom d'utilisateur proposé
     * @return le nouvel utilisateur créé
     */
    private User createNewUser(final String email, final String username, final String provider) {
        final User user = new User();
        user.setEmail(email);
        user.setUsername(generateUniqueUsername(username));
        user.setProvider(AuthProvider.valueOf(provider.toUpperCase()));
        return user;
    }

    /**
     * Génère un nom d'utilisateur unique en ajoutant un suffixe aléatoire
     * si nécessaire pour éviter les doublons en base.
     *
     * @param username le nom d'utilisateur de base
     * @return un nom d'utilisateur unique
     */
    private String generateUniqueUsername(final String username) {
        final int min = 11;
        final int max = 99;
        final var random = new Random();
        final int suffix = random.nextInt(max - min + 1) + min;

        final String usernameSuffix = username + suffix;

        if (userRepository.findByUsername(usernameSuffix).isEmpty()) {
            return usernameSuffix;
        }

        return generateUniqueUsername(usernameSuffix);
    }


}
