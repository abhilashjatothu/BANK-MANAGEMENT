package com.bankms.controller;

import com.bankms.dto.AccountSummaryDto;
import com.bankms.model.Transaction;
import com.bankms.service.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public String viewSummaryReport(Model model) {
        AccountSummaryDto summary = reportService.getSystemSummary();
        List<Map<String, Object>> totalsPerType = reportService.getTransactionTotalsPerType();
        List<Transaction> recentTransactions = reportService.getRecentTransactions();

        model.addAttribute("summary", summary);
        model.addAttribute("totalsPerType", totalsPerType);
        model.addAttribute("recentTransactions", recentTransactions);
        return "report/summary";
    }
}
