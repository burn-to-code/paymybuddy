package com.paymybuddy.service;

import com.paymybuddy.model.DTO.RegisterRequest;
import com.paymybuddy.model.User;

public interface UserService {

    void registerUser(RegisterRequest request);

    User getCurrentUserById(Long userId);
}
