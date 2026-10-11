package com.bankms.controller;

import com.bankms.dto.DepositForm;
import com.bankms.dto.TransferForm;
import com.bankms.dto.WithdrawForm;
import com.bankms.model.Account;
import com.bankms.model.Transaction;
import com.bankms.model.Transfer;
import com.bankms.model.enums.TransactionType;
import com.bankms.service.AccountService;
import com.bankms.service.CustomUserDetailsService.CustomUserDetails;
import com.bankms.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountService accountService;

    public TransactionController(TransactionService transactionService, AccountService accountService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
    }

    // --- DEPOSIT ---
    @GetMapping("/deposit")
    public String showDepositForm(@RequestParam(value = "accountNumber", required = false) String accountNumber,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  Model model) {
        DepositForm form = new DepositForm();
        if (accountNumber != null) {
            form.setAccountNumber(accountNumber);
        }
        prepareAccountListForUser(userDetails, model);
        model.addAttribute("depositForm", form);
        return "transaction/deposit";
    }

    @PostMapping("/deposit")
    public String processDeposit(@Valid @ModelAttribute("depositForm") DepositForm form,
                                 BindingResult result,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (result.hasErrors()) {
            prepareAccountListForUser(userDetails, model);
            return "transaction/deposit";
        }
        try {
            if ("CUSTOMER".equals(userDetails.getRoleName())) {
                accountService.validateCustomerOwnsAccount(form.getAccountNumber(), userDetails.getCustomerId());
            }
            Transaction txn = transactionService.deposit(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully deposited $" + txn.getAmount() + " into account " + form.getAccountNumber() +
                    ". Ref: " + txn.getReferenceNo());
            return "redirect:/accounts/view/" + form.getAccountNumber();
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepareAccountListForUser(userDetails, model);
            return "transaction/deposit";
        }
    }

    // --- WITHDRAW ---
    @GetMapping("/withdraw")
    public String showWithdrawForm(@RequestParam(value = "accountNumber", required = false) String accountNumber,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {
        WithdrawForm form = new WithdrawForm();
        if (accountNumber != null) {
            form.setAccountNumber(accountNumber);
        }
        prepareAccountListForUser(userDetails, model);
        model.addAttribute("withdrawForm", form);
        return "transaction/withdraw";
    }

    @PostMapping("/withdraw")
    public String processWithdraw(@Valid @ModelAttribute("withdrawForm") WithdrawForm form,
                                  BindingResult result,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        if (result.hasErrors()) {
            prepareAccountListForUser(userDetails, model);
            return "transaction/withdraw";
        }
        try {
            if ("CUSTOMER".equals(userDetails.getRoleName())) {
                accountService.validateCustomerOwnsAccount(form.getAccountNumber(), userDetails.getCustomerId());
            }
            Transaction txn = transactionService.withdraw(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully withdrawn $" + txn.getAmount() + " from account " + form.getAccountNumber() +
                    ". Ref: " + txn.getReferenceNo());
            return "redirect:/accounts/view/" + form.getAccountNumber();
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepareAccountListForUser(userDetails, model);
            return "transaction/withdraw";
        }
    }

    // --- TRANSFER ---
    @GetMapping("/transfer")
    public String showTransferForm(@RequestParam(value = "fromAccount", required = false) String fromAccount,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {
        TransferForm form = new TransferForm();
        if (fromAccount != null) {
            form.setFromAccountNumber(fromAccount);
        }
        prepareAccountListForUser(userDetails, model);
        model.addAttribute("transferForm", form);
        return "transaction/transfer";
    }

    @PostMapping("/transfer")
    public String processTransfer(@Valid @ModelAttribute("transferForm") TransferForm form,
                                  BindingResult result,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        if (result.hasErrors()) {
            prepareAccountListForUser(userDetails, model);
            return "transaction/transfer";
        }
        try {
            if ("CUSTOMER".equals(userDetails.getRoleName())) {
                accountService.validateCustomerOwnsAccount(form.getFromAccountNumber(), userDetails.getCustomerId());
            }
            Transfer transfer = transactionService.transfer(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully transferred $" + transfer.getAmount() + " from " +
                    form.getFromAccountNumber() + " to " + form.getToAccountNumber() +
                    ". Ref: " + transfer.getReferenceNo());
            return "redirect:/accounts/view/" + form.getFromAccountNumber();
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepareAccountListForUser(userDetails, model);
            return "transaction/transfer";
        }
    }

    // --- TRANSACTION HISTORY ---
    @GetMapping("/history")
    public String transactionHistory(
            @RequestParam(value = "accountId", required = false) Long accountId,
            @RequestParam(value = "type", required = false) TransactionType type,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        Page<Transaction> transactionPage;
        if ("CUSTOMER".equals(userDetails.getRoleName())) {
            transactionPage = transactionService.getCustomerFilteredTransactions(
                    userDetails.getCustomerId(), accountId, type, startDateTime, endDateTime, page, size
            );
            model.addAttribute("accounts", accountService.getAccountsByCustomerId(userDetails.getCustomerId()));
        } else {
            transactionPage = transactionService.getFilteredTransactions(
                    accountId, type, startDateTime, endDateTime, page, size
            );
            model.addAttribute("accounts", accountService.getAccounts(null, 0, 100).getContent());
        }

        model.addAttribute("transactions", transactionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactionPage.getTotalPages());
        model.addAttribute("totalElements", transactionPage.getTotalElements());
        model.addAttribute("selectedAccountId", accountId);
        model.addAttribute("selectedType", type);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("transactionTypes", TransactionType.values());

        return "transaction/history";
    }

    private void prepareAccountListForUser(CustomUserDetails userDetails, Model model) {
        if ("CUSTOMER".equals(userDetails.getRoleName())) {
            List<Account> accounts = accountService.getAccountsByCustomerId(userDetails.getCustomerId());
            model.addAttribute("userAccounts", accounts);
            model.addAttribute("isCustomer", true);
        } else {
            List<Account> accounts = accountService.getAccounts(null, 0, 100).getContent();
            model.addAttribute("userAccounts", accounts);
            model.addAttribute("isCustomer", false);
        }
    }
}
