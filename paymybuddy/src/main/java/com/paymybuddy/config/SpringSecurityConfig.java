package com.paymybuddy.config;

import com.paymybuddy.repository.UserRepository;
import com.paymybuddy.security.CustomOAuth2FailureHandler;
import com.paymybuddy.security.CustomOidcUserService;
import com.paymybuddy.security.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration de Spring Security pour l'application PayMyBuddy.
 *
 * <p>
 * Définit :
 * <ul>
 *     <li>la gestion de l'authentification par formulaire et OAuth2 (Google, etc.)</li>
 *     <li>les pages publiques et les restrictions d'accès aux ressources</li>
 *     <li>le chiffrement des mots de passe avec BCrypt</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {
    private final UserDetailsServiceImpl userDetailsService;
    private final UserRepository userRepository;

    public SpringSecurityConfig(UserDetailsServiceImpl userDetailsService, UserRepository userRepository) {
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    /**
     * Configure la chaîne de filtres de sécurité HTTP.
     *
     * <p>
     * Autorise certaines URLs publiques (CSS, JS, login, register) et exige une authentification pour toutes les autres.
     * Configure le login par formulaire et OAuth2, avec gestion des succès et échec de connexion.
     * </p>
     *
     * @param http HttpSecurity pour configurer la sécurité web
     * @param customOidcUserService le service OIDC personnalisé pour récupérer les informations utilisateur OAuth2
     * @return la chaîne de filtres de sécurité configurée
     * @throws Exception si une erreur survient lors de la configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CustomOidcUserService customOidcUserService) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/login.css", "/js/**", "/register", "/login").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/transferer", true)
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .failureHandler(new CustomOAuth2FailureHandler())
                        .defaultSuccessUrl("/transferer", true)
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOidcUserService)
                )
            );


        return http.build();
    }

    /**
     * Bean pour le service OIDC personnalisé.
     *
     * @return un objet CustomOidcUserService
     */
    @Bean
    public CustomOidcUserService customOAuth2UserService() {
        return new CustomOidcUserService(userRepository);
    }

    /**
     * Configure l'AuthenticationManager avec le UserDetailsService et le PasswordEncoder.
     *
     * @param http HttpSecurity utilisé pour récupérer l'AuthenticationManagerBuilder
     * @return l'AuthenticationManager configuré
     * @throws Exception si une erreur survient lors de la configuration
     */
    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder auth = http.getSharedObject(AuthenticationManagerBuilder.class);
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        return auth.build();
    }

    /**
     * Bean pour le chiffrement des mots de passe.
     *
     * @return un PasswordEncoder utilisant BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
