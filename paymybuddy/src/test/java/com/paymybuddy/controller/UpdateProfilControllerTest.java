package com.paymybuddy.controller;

import com.paymybuddy.model.DTO.UpdateUserRequest;
import com.paymybuddy.model.User;
import com.paymybuddy.service.SecurityUtils;
import com.paymybuddy.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UpdateProfilControllerTest {

    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        userService = mock(UserService.class);
        UpdateProfilController controller = new UpdateProfilController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void showUpdateProfilPage_shouldReturnProfilViewAndModel() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(new UpdateProfilController(userService))
                .setViewResolvers(new InternalResourceViewResolver("/WEB-INF/views/", ".jsp"))
                .build();

        mockMvc.perform(get("/profil"))
                .andExpect(status().isOk())
                .andExpect(view().name("profil"))
                .andExpect(model().attributeExists("request"))
                .andExpect(model().attribute("request", instanceOf(UpdateUserRequest.class)));
    }

    @Test
    void updateProfil_withValidationErrors_redirectsToProfilWithErrors() throws Exception {

        mockMvc.perform(post("/profil/update")
                        .param("username", "")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errors"))
                .andExpect(redirectedUrl("/profil"));
    }

    @Test
    void updateProfil_withServiceException_redirectsToProfilWithError() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("newUser");

        User connectedUser = new User();
        connectedUser.setUsername("user1");

        try (MockedStatic<SecurityUtils> mockedSecurity = Mockito.mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getConnectedUser).thenReturn(connectedUser);

            doThrow(new RuntimeException("Service failed")).when(userService).updateUser(any(), eq(connectedUser));

            mockMvc.perform(post("/profil/update")
                            .param("username", "newUser")
                            .param("email", "")
                            .param("password", "")
                    )
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("error", "Service failed"))
                    .andExpect(redirectedUrl("/profil"));
        }
    }

    @Test
    void updateProfil_success_redirectsWithSuccessFlash() throws Exception {
        User connectedUser = new User();
        connectedUser.setUsername("user1");

        try (MockedStatic<SecurityUtils> mockedSecurity = Mockito.mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getConnectedUser).thenReturn(connectedUser);

            doNothing().when(userService).updateUser(any(UpdateUserRequest.class), eq(connectedUser));

            mockMvc.perform(post("/profil/update")
                            .param("username", "validUser")
                            .param("email", "valid@example.com")
                            .param("password", "<PASSWORD>")
                    )
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("success", "les modifications ont bien été enregistrés"))
                    .andExpect(redirectedUrl("/profil"));
        }
    }

    @Test
    void depositMoney_success_redirectsWithSuccessFlash() throws Exception {
        User connectedUser = new User();
        connectedUser.setUsername("user1");

        try (MockedStatic<SecurityUtils> mockedSecurity = Mockito.mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getConnectedUser).thenReturn(connectedUser);

            doNothing().when(userService).depositOnAccount(any(BigDecimal.class), eq(connectedUser));

            mockMvc.perform(post("/profil/deposit")
                            .param("amount", "50.00")
                    )
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("successDeposit", "Montant ajouté avec succès !"))
                    .andExpect(redirectedUrl("/profil"));
        }
    }

    @Test
    void depositMoney_serviceException_redirectsWithErrorFlash() throws Exception {
        User connectedUser = new User();
        connectedUser.setUsername("user1");

        try (MockedStatic<SecurityUtils> mockedSecurity = Mockito.mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getConnectedUser).thenReturn(connectedUser);

            doThrow(new RuntimeException("Deposit failed")).when(userService).depositOnAccount(any(), eq(connectedUser));

            mockMvc.perform(post("/profil/deposit")
                            .param("amount", "25.00")
                    )
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("error", "Deposit failed"))
                    .andExpect(redirectedUrl("/profil"));
        }
    }
}

