package com.paymybuddy.controller;

import com.paymybuddy.model.DTO.UpdateUserRequest;
import com.paymybuddy.model.User;
import com.paymybuddy.service.SecurityUtils;
import com.paymybuddy.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@Slf4j
@RequestMapping("/profil")
public class UpdateProfilController {

    private final UserService userService;

    public UpdateProfilController(UserService userService) {
        this.userService = userService;
    }

    @RequestMapping
    public String showUpdateProfilPage(Model model) {
        model.addAttribute("request", new UpdateUserRequest());
        return "profil";
    }

    @PostMapping
    public String updateProfil(@ModelAttribute("request") @Valid UpdateUserRequest request,
                               BindingResult bindingResult,
                               RedirectAttributes model) {

        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
            model.addFlashAttribute("errors", errors);
            return "redirect:/profil";
        }

        User connectedUser = SecurityUtils.getConnectedUser();

        try {
            userService.updateUser(request, connectedUser);
        } catch (Exception ex) {
            log.error("Erreur lors de la sauvegarde du profil", ex);
            model.addFlashAttribute("error", ex.getMessage());
            return "redirect:/profil";
        }

        log.info("Mis à jour du profil de l'utilisateur {}", connectedUser.getUsername() + " réussie");
        model.addFlashAttribute("success", "les modifications ont bien été enregistrés");
        return "redirect:/profil";
    }
}
