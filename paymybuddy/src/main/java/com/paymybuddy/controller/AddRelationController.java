package com.paymybuddy.controller;

import com.paymybuddy.model.User;
import com.paymybuddy.service.SecurityUtils;
import com.paymybuddy.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/ajouter-relation")
public class AddRelationController {

    private final UserService userService;

    public AddRelationController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String showAddRelationPage() {
        return "ajouter-relation";
    }

    @PostMapping
    public String processAddRelation(@RequestParam String email, RedirectAttributes model) {
        log.info("Ajout d'une relation");

        try {
            User userConnected = SecurityUtils.getConnectedUser();
            userService.addUserConnexion(userConnected, email);
        } catch (Exception ex) {
            log.error("Erreur lors de l'ajout de la relation", ex);
            model.addFlashAttribute("error", ex.getMessage());
            return "redirect:/ajouter-relation";
        }

        log.info("Utilisateur ajouté en relation avec l'utilisateur {}", email);

        model.addFlashAttribute("success", "Utilisateur ajouté avec succès !");
        return "redirect:/ajouter-relation";
    }
}
