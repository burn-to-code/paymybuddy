package com.paymybuddy.controller;

import com.paymybuddy.service.SecurityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        if(SecurityUtils.isConnected()) {
            return "redirect:/transferer";
        }
        return "login";
    }
}
