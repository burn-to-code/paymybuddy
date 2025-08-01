package com.paymybuddy.security;

import com.paymybuddy.model.AuthProvider;
import com.paymybuddy.model.User;
import com.paymybuddy.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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
