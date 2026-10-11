package com.bankms.controller;

import com.bankms.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("GET / redirects unauthenticated user to /login")
    void testRootRedirect() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("GET /login renders login view without credentials")
    void testLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    @DisplayName("GET /register renders registration view")
    void testRegisterPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registerForm"));
    }

    @Test
    @DisplayName("GET /forgot-password renders forgot password view")
    void testForgotPasswordPage() throws Exception {
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"))
                .andExpect(model().attributeExists("forgotPasswordForm"));
    }

    @Test
    @DisplayName("POST /register successfully registers customer and redirects to login")
    void testHandleRegisterSuccess() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "New Test Customer")
                        .param("email", "newtestcust@example.com")
                        .param("phone", "+1 555-4321")
                        .param("address", "789 Pine Street")
                        .param("username", "newtestuser")
                        .param("accountType", "SAVINGS")
                        .param("initialDeposit", "250.00")
                        .param("password", "SecurePassword@123")
                        .param("confirmPassword", "SecurePassword@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));
    }

    @Test
    @DisplayName("POST /forgot-password successfully resets password for seeded customer and redirects to login")
    void testHandleForgotPasswordSuccess() throws Exception {
        // cust1 is seeded in test DB with email abhilash@bankms.com
        mockMvc.perform(post("/forgot-password")
                        .with(csrf())
                        .param("username", "cust1")
                        .param("email", "abhilash@bankms.com")
                        .param("newPassword", "BrandNewPass@123")
                        .param("confirmNewPassword", "BrandNewPass@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?reset=true"));
    }
}
