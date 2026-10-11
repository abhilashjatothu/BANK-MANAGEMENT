package com.bankms.controller;

import com.bankms.dto.AccountForm;
import com.bankms.model.Account;
import com.bankms.model.Customer;
import com.bankms.model.Transaction;
import com.bankms.model.enums.AccountStatus;
import com.bankms.model.enums.AccountType;
import com.bankms.service.AccountService;
import com.bankms.service.CustomerService;
import com.bankms.service.CustomUserDetailsService.CustomUserDetails;
import com.bankms.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final CustomerService customerService;
    private final TransactionService transactionService;

    public AccountController(AccountService accountService,
                             CustomerService customerService,
                             TransactionService transactionService) {
        this.accountService = accountService;
        this.customerService = customerService;
        this.transactionService = transactionService;
    }

    @GetMapping
    public String listAccounts(@RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "10") int size,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model) {
        if ("CUSTOMER".equals(userDetails.getRoleName())) {
            Page<Account> accountPage = accountService.getAccountsByCustomerId(userDetails.getCustomerId(), page, size);
            model.addAttribute("accounts", accountPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", accountPage.getTotalPages());
            model.addAttribute("totalElements", accountPage.getTotalElements());
            model.addAttribute("isCustomerView", true);
        } else {
            Page<Account> accountPage = accountService.getAccounts(keyword, page, size);
            model.addAttribute("accounts", accountPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", accountPage.getTotalPages());
            model.addAttribute("totalElements", accountPage.getTotalElements());
            model.addAttribute("keyword", keyword);
            model.addAttribute("isCustomerView", false);
        }
        return "account/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public String showOpenAccountForm(@RequestParam(value = "customerId", required = false) Long customerId, Model model) {
        AccountForm form = new AccountForm();
        if (customerId != null) {
            form.setCustomerId(customerId);
        }
        List<Customer> customers = customerService.findAll();
        model.addAttribute("accountForm", form);
        model.addAttribute("customers", customers);
        model.addAttribute("accountTypes", AccountType.values());
        model.addAttribute("accountStatuses", AccountStatus.values());
        return "account/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public String openAccount(@Valid @ModelAttribute("accountForm") AccountForm form,
                              BindingResult result,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("customers", customerService.findAll());
            model.addAttribute("accountTypes", AccountType.values());
            model.addAttribute("accountStatuses", AccountStatus.values());
            return "account/form";
        }
        try {
            Account account = accountService.openAccount(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Account " + account.getAccountNumber() + " successfully opened!");
            return "redirect:/accounts/view/" + account.getAccountNumber();
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("customers", customerService.findAll());
            model.addAttribute("accountTypes", AccountType.values());
            model.addAttribute("accountStatuses", AccountStatus.values());
            return "account/form";
        }
    }

    @GetMapping("/view/{accountNumber}")
    public String viewAccount(@PathVariable("accountNumber") String accountNumber,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              @RequestParam(value = "size", defaultValue = "10") int size,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              Model model) {
        if ("CUSTOMER".equals(userDetails.getRoleName())) {
            accountService.validateCustomerOwnsAccount(accountNumber, userDetails.getCustomerId());
        }

        Account account = accountService.getAccountByNumber(accountNumber);
        Page<Transaction> transactions = transactionService.getFilteredTransactions(
                account.getAccountId(), null, null, null, page, size
        );

        model.addAttribute("account", account);
        model.addAttribute("transactions", transactions.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactions.getTotalPages());
        model.addAttribute("allStatuses", AccountStatus.values());
        return "account/view";
    }

    @PostMapping("/status/{accountNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public String updateStatus(@PathVariable("accountNumber") String accountNumber,
                               @RequestParam("status") AccountStatus status,
                               RedirectAttributes redirectAttributes) {
        try {
            accountService.updateAccountStatus(accountNumber, status);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Account " + accountNumber + " status updated to " + status);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/accounts/view/" + accountNumber;
    }
}
