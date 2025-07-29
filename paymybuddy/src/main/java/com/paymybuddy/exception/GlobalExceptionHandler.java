package com.paymybuddy.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // ERREUR GENERIQUE
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleMissingParams(MissingServletRequestParameterException ex, Model model) {
        log.error("erreur de paramètre manquant : {}", ex.getParameterName());
        String name = ex.getParameterName();
        model.addAttribute("error", "Le paramètre '" + name + "' est manquant");
        return "error";
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleBindException(BindException exception, Model model) {
        log.error("Erreur de binding : {}", exception.getBindingResult());
        model.addAttribute("error", "Une erreur s’est produite lors du traitement du formulaire.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleMethodArgumentNotValid(MethodArgumentNotValidException exception, Model model) {
        log.error("Validation échouée : {}", exception.getMessage());
        model.addAttribute("error", "Les données du formulaire ne sont pas valides.");
    }
}
