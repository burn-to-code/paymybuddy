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

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_email"), "Email is required from OAuth2 provider");
        }

        User existingUser = userRepository.findByEmail(email).orElse(null);

        if (existingUser != null && existingUser.getProvider() == AuthProvider.LOCAL) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"),
                    "Un compte local existe déjà pour cet email. Veuillez vous connecter avec votre email et mot de passe.");
        }


        if(existingUser == null) {
            existingUser = new User();
            existingUser.setEmail(email);
            existingUser.setUsername(generateUniqueUsername(username));
            existingUser.setProvider(AuthProvider.valueOf(provider.toUpperCase()));
            userRepository.save(existingUser);
        } else {
            existingUser.setProvider(AuthProvider.valueOf(provider.toUpperCase()));
            userRepository.save(existingUser);
        }

        return new CustomOidcUser(OidcUser, existingUser);
    }

    private String generateUniqueUsername(String base) {
        String username = base;
        int suffix = 0;

        while (userRepository.findByUsername(username).isPresent()) {
            suffix++;
            username = base + suffix;
        }
        return username;
    }



}
