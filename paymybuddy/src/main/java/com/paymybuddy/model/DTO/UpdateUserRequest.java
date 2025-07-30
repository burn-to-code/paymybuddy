package com.paymybuddy.model.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @NotNull(message = "Un nom d'utilisateur esr requis")
    private String username;

    @NotNull(message = "Un email  est requis")
    private String email;

    @NotNull(message = "Un mot de passe est requis")
    private String password;
}
