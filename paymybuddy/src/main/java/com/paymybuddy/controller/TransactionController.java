package com.paymybuddy.controller;

import com.paymybuddy.model.DTO.TransactionRequest;
import com.paymybuddy.model.User;
import com.paymybuddy.service.TransactionService;
import com.paymybuddy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/transferer")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String showTransactionPage(Model model, Principal principal) {
        TransactionRequest request = new TransactionRequest();
        User currentUser = userService.getCurrentUserByEMail(principal.getName(), "transaction", request);

        model.addAttribute("request", request);
        model.addAttribute("contacts", currentUser.getConnections());
        model.addAttribute("transactions", transactionService.getTransactionByUserSender(currentUser));

        return "transferer";
    }

    @PostMapping
    public String processTransaction(
            @ModelAttribute("request") @Valid TransactionRequest request,
            BindingResult bindingResult,
            Model model,
            Principal principal) {

        User currentUser = userService.getCurrentUserByEMail(principal.getName(), "transaction", request);

        if (bindingResult.hasErrors()) {
            model.addAttribute("transactions", transactionService.getTransactionByUserSender(currentUser));
            return "transferer";
        }

        transactionService.saveNewTransaction(request, currentUser);

        return "redirect:/transferer";
    }
}
