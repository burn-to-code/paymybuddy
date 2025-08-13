package com.paymybuddy.service;

import com.paymybuddy.model.DTO.RegisterRequest;
import com.paymybuddy.model.DTO.UpdateUserRequest;
import com.paymybuddy.model.User;

import java.math.BigDecimal;
import java.util.List;

public interface UserService {

    void registerUser(RegisterRequest request);

    List<User> getListOfConnectionOfCurrentUserById(Long userId);

    void addUserConnexion(User userConnected, String emailOfAnotherUser);

    void updateUser(UpdateUserRequest request, User user);

    void depositOnAccount(BigDecimal amount, User user);
}
