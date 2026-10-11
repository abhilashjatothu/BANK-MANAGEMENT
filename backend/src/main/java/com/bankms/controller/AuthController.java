package com.bankms.controller;

import com.bankms.dto.ForgotPasswordForm;
import com.bankms.dto.RegisterForm;
import com.bankms.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String rootRedirect(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "registered", required = false) String registered,
                            @RequestParam(value = "reset", required = false) String reset,
                            Model model,
                            Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Account created successfully! Please sign in with your credentials.");
        }
        if (reset != null) {
            model.addAttribute("successMessage", "Password reset successful! Please sign in with your new password.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("registerForm") RegisterForm form,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerCustomerWithAccount(form);
            redirectAttributes.addFlashAttribute("successMessage", "Account created successfully! Please sign in with your credentials.");
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "An unexpected error occurred during registration. Please try again.");
            return "auth/register";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        if (!model.containsAttribute("forgotPasswordForm")) {
            model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@Valid @ModelAttribute("forgotPasswordForm") ForgotPasswordForm form,
                                       BindingResult bindingResult,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }

        try {
            userService.resetPassword(form);
            redirectAttributes.addFlashAttribute("successMessage", "Password reset successful! Please sign in with your new password.");
            return "redirect:/login?reset=true";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/forgot-password";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Unable to reset password. Please check your details and try again.");
            return "auth/forgot-password";
        }
    }

    @GetMapping("/dashboard")
    public String routeDashboard(Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }
        for (GrantedAuthority auth : authentication.getAuthorities()) {
            String role = auth.getAuthority();
            if ("ROLE_ADMIN".equals(role)) {
                return "redirect:/dashboard/admin";
            } else if ("ROLE_EMPLOYEE".equals(role)) {
                return "redirect:/dashboard/employee";
            } else if ("ROLE_CUSTOMER".equals(role)) {
                return "redirect:/dashboard/customer";
            }
        }
        return "redirect:/login";
    }
}
