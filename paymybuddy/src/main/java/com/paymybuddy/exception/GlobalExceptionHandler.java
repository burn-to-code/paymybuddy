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
    public String handleBindException(BindException exception, Model model) {
        log.error("Erreur de binding : {}", exception.getBindingResult());
        model.addAttribute("error", "Une erreur s’est produite lors du traitement du formulaire.");
        return "error";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleMethodArgumentNotValid(MethodArgumentNotValidException exception, Model model) {
        log.error("Validation échouée : {}", exception.getMessage());
        model.addAttribute("error", "Les données du formulaire ne sont pas valides.");
        return "error";
    }

    // ERREUR PERSONNALISE AVEC REDIRECTION

    @ExceptionHandler(UsernameConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleUsernameConflictException(UsernameConflictException exception, Model model) {
        createModel(exception, model, exception.getFormData());
        return safeGetUrlName(exception);

    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(EmailConflictException.class)
    public String handleEmailConflictException(EmailConflictException exception, Model model) {
        createModel(exception, model, exception.getFormData());
        return safeGetUrlName(exception);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(UsernameNotFoundException.class)
    public String handleUsernameNotFoundException(UsernameNotFoundException exception, Model model) {
        createModel(exception, model, exception.getFormData());
        return safeGetUrlName(exception);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(EmailNotFoundException.class)
    public String handleEmailNotFoundException(EmailNotFoundException exception, Model model) {
        createModel(exception, model, exception.getFormData());
        return safeGetUrlName(exception);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(TransactionBusinessException.class)
    public String handleTransactionBusinessException(TransactionBusinessException exception, Model model) {
        createModel(exception, model, exception.getFormData());
        return safeGetUrlName(exception);
    }

    //UTILITAIRES
    private void createModel(Exception exception, Model model, Object formData) {
        log.error(exception.getMessage());
        model.addAttribute("request", formData);
        model.addAttribute("error", exception.getMessage());
    }

    private String safeGetUrlName(ExceptionWithUrlName ex) {
        String url = ex.getUrlName();
        if (url == null || url.isEmpty()) {
            log.error("Une erreur esr survenue : l'Url lors de la redirection de l'exception esr nulle ou vide.");
            return "error";
        }
        return url;
    }
}
