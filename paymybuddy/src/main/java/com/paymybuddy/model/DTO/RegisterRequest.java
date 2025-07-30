package com.paymybuddy.model.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "UserName is required")
    @NotEmpty(message = "UserName is required")
    private String userName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @NotEmpty(message = "Password is required")
    private String password;
}
