package com.bankms.controller;

import com.bankms.dto.AccountSummaryDto;
import com.bankms.model.Account;
import com.bankms.model.Transaction;
import com.bankms.service.AccountService;
import com.bankms.service.CustomUserDetailsService.CustomUserDetails;
import com.bankms.service.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final ReportService reportService;
    private final AccountService accountService;

    public DashboardController(ReportService reportService, AccountService accountService) {
        this.reportService = reportService;
        this.accountService = accountService;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Model model) {
        AccountSummaryDto summary = reportService.getSystemSummary();
        List<Transaction> recentTransactions = reportService.getRecentTransactions();

        model.addAttribute("summary", summary);
        model.addAttribute("recentTransactions", recentTransactions);
        model.addAttribute("roleTitle", "Administrator Dashboard");
        return "dashboard/admin";
    }

    @GetMapping("/employee")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public String employeeDashboard(Model model) {
        AccountSummaryDto summary = reportService.getSystemSummary();
        List<Transaction> recentTransactions = reportService.getRecentTransactions();

        model.addAttribute("summary", summary);
        model.addAttribute("recentTransactions", recentTransactions);
        model.addAttribute("roleTitle", "Staff / Employee Dashboard");
        return "dashboard/employee";
    }

    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String customerDashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Long customerId = userDetails.getCustomerId();
        List<Account> accounts = accountService.getAccountsByCustomerId(customerId);
        List<Transaction> recentTransactions = reportService.getRecentTransactionsForCustomer(customerId);

        model.addAttribute("accounts", accounts);
        model.addAttribute("recentTransactions", recentTransactions);
        model.addAttribute("customerName", userDetails.getCustomerName());
        model.addAttribute("roleTitle", "Customer Portal");
        return "dashboard/customer";
    }
}
