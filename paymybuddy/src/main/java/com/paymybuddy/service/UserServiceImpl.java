package com.paymybuddy.service;

import com.paymybuddy.exception.EmailConflictException;
import com.paymybuddy.exception.UserNotFoundException;
import com.paymybuddy.exception.UsernameConflictException;
import com.paymybuddy.model.DTO.RegisterRequest;
import com.paymybuddy.model.User;
import com.paymybuddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void registerUser(RegisterRequest request) {
        log.info("Création utilisateur avec email {}", request.getEmail());

        if(userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("L'utilisateur avec l'email {}, existe déjà.", request.getEmail());
            throw new EmailConflictException("Email déjà utilisé");
        }
        if(userRepository.findByUsername(request.getUserName()).isPresent()) {
            log.warn("L'utilisateur avec le nom d'utilisateur {}, existe déjà", request.getUserName());
            throw new UsernameConflictException("UserName déjà utilisé");
        }

        User user = new User();

        user.setUsername(request.getUserName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);
    }

    @Override
    @Transactional
    public User getCurrentUserById(Long userId) {
        log.info("Récupération d'un utilisateur avec ses contacts avec l'id {}", userId);
        return userRepository.findByIdWithConnections(userId)
                 .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable avec l'id : " + userId));
    }
}
