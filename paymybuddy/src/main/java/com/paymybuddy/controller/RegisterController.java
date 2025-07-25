package com.paymybuddy.controller;

import com.paymybuddy.model.DTO.RegisterRequest;
import com.paymybuddy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;

@Controller
public class RegisterController {

    @Autowired
    private UserService userService;


    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("request", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest request, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "register";
        }

        userService.registerUser(request);
        return "redirect:/login";
    }
}
