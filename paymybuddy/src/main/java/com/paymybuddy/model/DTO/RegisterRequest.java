package com.paymybuddy.model.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "L'userName est requis")
    private String userName;

    @NotBlank(message = "L'email est requis")
    @Email(message = "L'email est invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est requis")
    private String password;
}
