package com.bankms.controller.api;

import com.bankms.dto.*;
import com.bankms.model.Account;
import com.bankms.model.Customer;
import com.bankms.model.Transaction;
import com.bankms.model.Transfer;
import com.bankms.model.User;
import com.bankms.service.AccountService;
import com.bankms.service.CustomerService;
import com.bankms.service.ReportService;
import com.bankms.service.TransactionService;
import com.bankms.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * REST API Controller serving the separate frontend SPA client.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BankApiController {

    private final CustomerService customerService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final ReportService reportService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public BankApiController(CustomerService customerService,
                             AccountService accountService,
                             TransactionService transactionService,
                             ReportService reportService,
                             UserService userService,
                             PasswordEncoder passwordEncoder) {
        this.customerService = customerService;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.reportService = reportService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // --- Authentication ---
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
        }

        Optional<User> userOpt = userService.findByUsername(username.trim());
        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password"));
        }

        User user = userOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getUserId());
        response.put("username", user.getUsername());
        response.put("role", user.getRole().name());
        if (user.getCustomer() != null) {
            response.put("customerId", user.getCustomer().getCustomerId());
            response.put("customerName", user.getCustomer().getName());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> apiRegister(@Valid @RequestBody RegisterForm form) {
        try {
            User user = userService.registerCustomerWithAccount(form);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Account registered successfully");
            response.put("username", user.getUsername());
            response.put("role", user.getRole().name());
            if (user.getCustomer() != null) {
                response.put("customerId", user.getCustomer().getCustomerId());
                response.put("customerName", user.getCustomer().getName());
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to register: " + ex.getMessage()));
        }
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<?> apiForgotPassword(@Valid @RequestBody ForgotPasswordForm form) {
        try {
            userService.resetPassword(form);
            return ResponseEntity.ok(Map.of("message", "Password has been successfully reset."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reset password: " + ex.getMessage()));
        }
    }

    // --- System Stats ---
    @GetMapping("/stats")
    public ResponseEntity<AccountSummaryDto> getStats() {
        return ResponseEntity.ok(reportService.getSystemSummary());
    }

    // --- Customers ---
    @GetMapping("/customers")
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return ResponseEntity.ok(customerService.findAll());
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<?> getCustomer(@PathVariable("id") Long id) {
        return customerService.findById(id)
                .map(c -> {
                    Map<String, Object> res = new HashMap<>();
                    res.put("customer", c);
                    res.put("accounts", accountService.getAccountsByCustomerId(id));
                    return ResponseEntity.ok((Object) res);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/customers")
    public ResponseEntity<?> createCustomer(@Valid @RequestBody CustomerForm form) {
        try {
            Customer customer = customerService.createCustomer(form);
            return ResponseEntity.status(HttpStatus.CREATED).body(customer);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // --- Accounts ---
    @GetMapping("/accounts")
    public ResponseEntity<List<Account>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAccounts(null, 0, 100).getContent());
    }

    @GetMapping("/accounts/{accountNumber}")
    public ResponseEntity<?> getAccount(@PathVariable("accountNumber") String accountNumber) {
        try {
            Account account = accountService.getAccountByNumber(accountNumber);
            List<Transaction> transactions = transactionService.getFilteredTransactions(
                    account.getAccountId(), null, null, null, 0, 50
            ).getContent();

            Map<String, Object> res = new HashMap<>();
            res.put("account", account);
            res.put("transactions", transactions);
            return ResponseEntity.ok(res);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/accounts")
    public ResponseEntity<?> openAccount(@Valid @RequestBody AccountForm form) {
        try {
            Account account = accountService.openAccount(form);
            return ResponseEntity.status(HttpStatus.CREATED).body(account);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // --- Transactions ---
    @PostMapping("/transactions/deposit")
    public ResponseEntity<?> deposit(@Valid @RequestBody DepositForm form) {
        try {
            Transaction txn = transactionService.deposit(form);
            return ResponseEntity.ok(txn);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/transactions/withdraw")
    public ResponseEntity<?> withdraw(@Valid @RequestBody WithdrawForm form) {
        try {
            Transaction txn = transactionService.withdraw(form);
            return ResponseEntity.ok(txn);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/transactions/transfer")
    public ResponseEntity<?> transfer(@Valid @RequestBody TransferForm form) {
        try {
            Transfer transfer = transactionService.transfer(form);
            return ResponseEntity.ok(transfer);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(
            @RequestParam(value = "accountId", required = false) Long accountId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return ResponseEntity.ok(transactionService.getFilteredTransactions(accountId, null, null, null, page, size).getContent());
    }
}
