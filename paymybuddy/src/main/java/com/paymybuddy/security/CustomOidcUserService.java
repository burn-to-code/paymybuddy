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

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

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
                .orElse(createNewUser(email, username));

        if (user.getProvider() == AuthProvider.LOCAL) {
                throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"),
                        "Un compte local existe déjà pour cet email. Veuillez vous connecter avec votre email et mot de passe.");
        }

        user.setProvider(AuthProvider.valueOf(provider.toUpperCase()));

        userRepository.save(user);

        return new CustomOidcUser(OidcUser, user);
    }

    private User createNewUser(final String email, final String username) {
        final User user = new User();
        user.setEmail(email);
        user.setUsername(generateUniqueUsername(username));
        return user;
    }

    private String generateUniqueUsername(final String username) {
        final int min = 11;
        final int max = 99;
        final var random = new Random();
        final double suffix = random.nextInt(max - min + 1) + min;

        final String usernameSuffix = username + suffix;

        if (userRepository.findByUsername(usernameSuffix).isEmpty()) {
            return usernameSuffix;
        }

        return generateUniqueUsername(usernameSuffix);
    }


}
