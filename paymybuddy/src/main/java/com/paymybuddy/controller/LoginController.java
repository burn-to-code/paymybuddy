package com.paymybuddy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
// JUSTE UN CONTROLLER TEST
    @GetMapping("/home")
    public String home() {
        return "home";
    }

}
