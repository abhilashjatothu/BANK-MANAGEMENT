package com.bankms.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ReferenceNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private ReferenceNumberGenerator() {
    }

    public static String generateTransactionReference() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int randomDigits = 1000 + RANDOM.nextInt(9000);
        return "TXN-" + timestamp + "-" + randomDigits;
    }

    public static String generateTransferReference() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int randomDigits = 1000 + RANDOM.nextInt(9000);
        return "TRF-" + timestamp + "-" + randomDigits;
    }

    public static String generateAccountNumber() {
        // Unique 12-digit account number starting with standard bank code e.g. 10
        long number = 100000000000L + (long) (RANDOM.nextDouble() * 899999999999L);
        return String.valueOf(number);
    }
}
