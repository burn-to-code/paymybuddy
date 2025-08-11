package com.paymybuddy.controller;

import com.paymybuddy.model.DTO.RegisterRequest;
import com.paymybuddy.service.SecurityUtils;
import com.paymybuddy.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class RegisterController {

    private final UserService userService;

    @GetMapping("/register")
    public String register(Model model) {
        if(SecurityUtils.isConnected()) {
            return "redirect:/transferer";
        }
        model.addAttribute("request", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest request, BindingResult bindingResult, RedirectAttributes model) {

        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
            model.addFlashAttribute("errors", errors);
            return "redirect:/register";
        }
        try {
            userService.registerUser(request);
        } catch (Exception ex) {
            model.addFlashAttribute("error", ex.getMessage());
            return "redirect:/register";
        }

        model.addFlashAttribute("success", "Votre inscription à été enregistré ! Veuillez vous connecté pour accéder à votre compte.");
        return "redirect:/login";
    }
}
