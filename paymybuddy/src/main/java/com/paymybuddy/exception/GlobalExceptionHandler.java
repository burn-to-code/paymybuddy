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
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 400
    public String handleMissingParams(MissingServletRequestParameterException ex, Model model) {
        String name = ex.getParameterName();
        model.addAttribute("error", "Le paramètre '" + name + "' est manquant");
        return "error";
    }

    @ExceptionHandler(BindException.class)
    public String handleBindException(BindException exception, Model model) {
        log.error("Erreur de binding : {}", exception.getMessage());
        model.addAttribute("error", "Une erreur s’est produite lors du traitement du formulaire.");
        return "error/genericError"; // ou "register" si tu veux renvoyer vers un formulaire spécifique
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleMethodArgumentNotValid(MethodArgumentNotValidException exception, Model model) {
        log.error("Validation échouée : {}", exception.getMessage());
        model.addAttribute("error", "Les données du formulaire ne sont pas valides.");
        return "error/genericError";
    }

    // ERREUR PERSONNALISE AVEC REDIRECTION

    @ExceptionHandler(UsernameConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleUsernameConflictException(UsernameConflictException exception, Model model) {
        createModel(exception, model, exception.getFormData());
        return exception.getUrlName();

    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(EmailConflictException.class)
    public String handleEmailConflictException(EmailConflictException exception, Model model) {
        createModel(exception, model, exception.getFormData());
        return exception.getUrlName();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(UsernameNotFoundException.class)
    public String handleUsernameNotFoundException(UsernameNotFoundException exception, Model model) {
        createModel(exception, model, exception.getFormData());
        return exception.getUrlName();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(EmailNotFoundException.class)
    public String handleEmailNotFoundException(EmailNotFoundException exception, Model model) {
        createModel(exception, model, exception.getFormData());
        return exception.getUrlName();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(TransactionBusinessException.class)
    public String handleTransactionBusinessException(TransactionBusinessException exception, Model model) {
        createModel(exception, model, exception.getFormData());
        return exception.getUrlName();
    }

    //UTILITAIRES
    private void createModel(Exception exception, Model model, Object formData) {
        log.error(exception.getMessage());
        model.addAttribute("request", formData);
        model.addAttribute("error", exception.getMessage());
    }
}
