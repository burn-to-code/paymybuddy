package com.paymybuddy.controller;

import com.paymybuddy.model.DTO.TransactionRequest;
import com.paymybuddy.model.Transaction;
import com.paymybuddy.model.User;
import com.paymybuddy.service.SecurityUtils;
import com.paymybuddy.service.TransactionService;
import com.paymybuddy.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/transferer")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;

    @Autowired
    public TransactionController(TransactionService transactionService, UserService userService) {
        this.transactionService = transactionService;
        this.userService = userService;
    }

    @GetMapping
    public String showTransactionPage(Model model) {
        TransactionRequest request = new TransactionRequest();
        request.setUserReceiverId(0L);

        final Long connectedUser = SecurityUtils.getConnectedUserId();
        User currentUser;
        List<Transaction> transactions;

        try {
            currentUser = userService.getCurrentUserById(connectedUser);
            transactions = transactionService.getTransactionByUserSenderId(connectedUser);
        } catch (Exception ex) {
            log.error("Une erreur est survenu lors de la récupération de l'user courant ou de ses transactions", ex);
            model.addAttribute("error", ex.getMessage());
            return "redirect:/transferer";
        }

        model.addAttribute("request", request);
        model.addAttribute("contacts", currentUser.getConnections());
        model.addAttribute("transactions", transactionService.getTransactionDTOToShow(transactions));

        return "transferer";
    }

    @PostMapping
    public String processTransaction(
            @ModelAttribute("request") @Valid TransactionRequest request,
            BindingResult bindingResult,
            RedirectAttributes model) {

        User connectedUser = SecurityUtils.getConnectedUser();

        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
            model.addFlashAttribute("errors", errors);
            return "redirect:/transferer";
        }

        try {
            transactionService.saveNewTransaction(request, connectedUser);
        }catch (Exception ex){
            log.error("Erreur lors de la sauvegarde de la transaction", ex);
            model.addFlashAttribute("error", ex.getMessage());
            return "redirect:/transferer";
        }

        model.addFlashAttribute("success", "transation effectuée avec succès");
        return "redirect:/transferer";
    }
}
