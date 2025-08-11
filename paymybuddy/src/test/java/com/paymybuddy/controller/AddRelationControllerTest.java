package com.paymybuddy.controller;

import com.paymybuddy.model.User;
import com.paymybuddy.service.SecurityUtils;
import com.paymybuddy.service.UserService;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AddRelationControllerTest {

    private UserService userService;
    private AddRelationController controller;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        controller = new AddRelationController(userService);
        securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    void showAddRelationPage_shouldReturnViewName() {
        String view = controller.showAddRelationPage();
        assertEquals("ajouter-relation", view);
    }

    @Test
    void processAddRelation_success_shouldRedirectWithSuccessFlash() {
        User user = new User();
        user.setUsername("testUser");

        // Mock SecurityUtils.getConnectedUser() pour retourner un user
        securityUtilsMock.when(SecurityUtils::getConnectedUser).thenReturn(user);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String emailToAdd = "friend@example.com";

        String result = controller.processAddRelation(emailToAdd, redirectAttributes);

        assertEquals("redirect:/ajouter-relation", result);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("success"));
        assertEquals("Utilisateur ajouté avec succès !", redirectAttributes.getFlashAttributes().get("success"));

        verify(userService, times(1)).addUserConnexion(user, emailToAdd);
    }

    @Test
    void processAddRelation_whenException_shouldRedirectWithErrorFlash() {
        User user = new User();
        user.setUsername("testUser");

        securityUtilsMock.when(SecurityUtils::getConnectedUser).thenReturn(user);

        // Simuler une exception levée par userService.addUserConnexion()
        doThrow(new RuntimeException("Erreur critique")).when(userService).addUserConnexion(any(), anyString());

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String emailToAdd = "friend@example.com";

        String result = controller.processAddRelation(emailToAdd, redirectAttributes);

        assertEquals("redirect:/ajouter-relation", result);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("error"));
        assertEquals("Erreur critique", redirectAttributes.getFlashAttributes().get("error"));

        verify(userService, times(1)).addUserConnexion(user, emailToAdd);
    }
}
