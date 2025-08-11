package com.paymybuddy.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // ERREUR Générique
    @ExceptionHandler(MissingServletRequestParameterException.class)
//    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleMissingParams(MissingServletRequestParameterException ex, RedirectAttributes model) {
        log.error("erreur de paramètre manquant : {}", ex.getParameterName());
        String name = ex.getParameterName();
        model.addFlashAttribute("error", "Le paramètre '" + name + "' est manquant");
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleBindException(BindException exception, RedirectAttributes model) {
        log.error("Erreur de binding : {}", exception.getBindingResult());
        model.addFlashAttribute("error", "Une erreur s’est produite lors du traitement du formulaire.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleMethodArgumentNotValid(MethodArgumentNotValidException exception, RedirectAttributes model) {
        log.error("Validation échouée : {}", exception.getMessage());
        model.addFlashAttribute("error", "Les données du formulaire ne sont pas valides.");
    }
}
