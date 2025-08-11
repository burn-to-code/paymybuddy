package com.paymybuddy.controller;

import com.paymybuddy.model.DTO.RegisterRequest;
import com.paymybuddy.service.SecurityUtils;
import com.paymybuddy.service.UserService;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegisterControllerTest {

    private UserService userService;
    private RegisterController controller;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        controller = new RegisterController(userService);
        securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    void register_get_whenUserNotConnected_returnsRegisterView() {
        securityUtilsMock.when(SecurityUtils::isConnected).thenReturn(false);

        var model = mock(org.springframework.ui.Model.class);

        String view = controller.register(model);

        assertEquals("register", view);
        verify(model, times(1)).addAttribute(eq("request"), any(RegisterRequest.class));
    }

    @Test
    void register_get_whenUserConnected_redirectsToTransferer() {
        securityUtilsMock.when(SecurityUtils::isConnected).thenReturn(true);

        var model = mock(org.springframework.ui.Model.class);

        String view = controller.register(model);

        assertEquals("redirect:/transferer", view);
        verifyNoInteractions(model);
    }

    @Test
    void register_post_whenBindingErrors_redirectsWithErrorsFlash() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        RegisterRequest request = new RegisterRequest();

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        var error1 = mock(org.springframework.validation.ObjectError.class);
        when(error1.getDefaultMessage()).thenReturn("Erreur 1");
        var error2 = mock(org.springframework.validation.ObjectError.class);
        when(error2.getDefaultMessage()).thenReturn("Erreur 2");

        when(bindingResult.getAllErrors()).thenReturn(List.of(error1, error2));

        String view = controller.register(request, bindingResult, redirectAttributes);

        assertEquals("redirect:/register", view);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("errors"));

        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) redirectAttributes.getFlashAttributes().get("errors");
        assertEquals(2, errors.size());
        assertTrue(errors.contains("Erreur 1"));
        assertTrue(errors.contains("Erreur 2"));
    }

    @Test
    void register_post_whenServiceThrowsException_redirectsWithErrorFlash() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        RegisterRequest request = new RegisterRequest();

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        doThrow(new RuntimeException("Erreur critique")).when(userService).registerUser(any());

        String view = controller.register(request, bindingResult, redirectAttributes);

        assertEquals("redirect:/register", view);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("error"));
        assertEquals("Erreur critique", redirectAttributes.getFlashAttributes().get("error"));
    }

    @Test
    void register_post_whenSuccess_redirectsWithSuccessFlash() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        RegisterRequest request = new RegisterRequest();

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        String view = controller.register(request, bindingResult, redirectAttributes);

        assertEquals("redirect:/login", view);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("success"));
        assertEquals("Votre inscription à été enregistré ! Veuillez vous connecté pour accéder à votre compte.",
                redirectAttributes.getFlashAttributes().get("success"));

        verify(userService, times(1)).registerUser(request);
    }
}

