package com.bankms.controller;

import com.bankms.service.CustomUserDetailsService.CustomUserDetails;
import com.bankms.model.Customer;
import com.bankms.model.User;
import com.bankms.model.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "cust1", roles = {"CUSTOMER"})
    @DisplayName("Security: Customer cannot open admin dashboard (expects 403)")
    void testCustomerCannotOpenAdminDashboard() throws Exception {
        mockMvc.perform(get("/dashboard/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Security: Admin can access admin dashboard")
    void testAdminCanOpenAdminDashboard() throws Exception {
        mockMvc.perform(get("/dashboard/admin"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: Customer cannot access another customer's account")
    void testCustomerCannotAccessAnotherCustomerAccount() throws Exception {
        // Create user details for Customer ID 999 (does not own demo account 101020304050 owned by cust1)
        Customer otherCustomer = new Customer("Other Cust", "other@example.com", "555-9999", "Other St");
        otherCustomer.setCustomerId(999L);
        User otherUser = new User("otherUser", "pass", Role.CUSTOMER, true, otherCustomer);
        CustomUserDetails customUserDetails = new CustomUserDetails(otherUser);

        mockMvc.perform(get("/accounts/view/101020304050").with(user(customUserDetails)))
                .andExpect(status().isForbidden());
    }
}
