package com.bankms.controller;

import com.bankms.dto.CustomerForm;
import com.bankms.model.Account;
import com.bankms.model.Customer;
import com.bankms.service.AccountService;
import com.bankms.service.CustomerService;
import com.bankms.service.CustomUserDetailsService.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final AccountService accountService;

    public CustomerController(CustomerService customerService, AccountService accountService) {
        this.customerService = customerService;
        this.accountService = accountService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public String listCustomers(@RequestParam(value = "keyword", required = false) String keyword,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "10") int size,
                                Model model) {
        Page<Customer> customerPage = customerService.getCustomers(keyword, page, size);
        model.addAttribute("customers", customerPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", customerPage.getTotalPages());
        model.addAttribute("totalElements", customerPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        return "customer/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public String showCreateForm(Model model) {
        model.addAttribute("customerForm", new CustomerForm());
        model.addAttribute("isEdit", false);
        return "customer/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public String createCustomer(@Valid @ModelAttribute("customerForm") CustomerForm form,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "customer/form";
        }
        try {
            Customer customer = customerService.createCustomer(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Customer '" + customer.getName() + "' created successfully!");
            return "redirect:/customers/view/" + customer.getCustomerId();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", false);
            return "customer/form";
        }
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Customer customer = customerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + id));

        CustomerForm form = new CustomerForm();
        form.setId(customer.getCustomerId());
        form.setName(customer.getName());
        form.setEmail(customer.getEmail());
        form.setPhone(customer.getPhone());
        form.setAddress(customer.getAddress());
        if (customer.getUser() != null) {
            form.setUsername(customer.getUser().getUsername());
        }

        model.addAttribute("customerForm", form);
        model.addAttribute("isEdit", true);
        return "customer/form";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public String updateCustomer(@PathVariable("id") Long id,
                                 @Valid @ModelAttribute("customerForm") CustomerForm form,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", true);
            return "customer/form";
        }
        try {
            customerService.updateCustomer(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Customer profile updated successfully!");
            return "redirect:/customers/view/" + id;
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", true);
            return "customer/form";
        }
    }

    @GetMapping("/view/{id}")
    public String viewCustomer(@PathVariable("id") Long id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model) {
        // Enforce customer ownership: CUSTOMER role can only view their own record
        if ("CUSTOMER".equals(userDetails.getRoleName())) {
            if (!id.equals(userDetails.getCustomerId())) {
                throw new AccessDeniedException("You are not authorized to view other customer records.");
            }
        }

        Customer customer = customerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + id));
        List<Account> accounts = accountService.getAccountsByCustomerId(id);

        model.addAttribute("customer", customer);
        model.addAttribute("accounts", accounts);
        return "customer/view";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteCustomer(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            customerService.deleteCustomer(id);
            redirectAttributes.addFlashAttribute("successMessage", "Customer deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customers";
    }
}
