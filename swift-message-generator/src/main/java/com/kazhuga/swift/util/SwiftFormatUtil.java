package com.kazhuga.swift.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Formatting and parsing helpers for SWIFT field values.
 */
public final class SwiftFormatUtil {

    private SwiftFormatUtil() {}

    private static final DateTimeFormatter YYMMDD   = DateTimeFormatter.ofPattern("yyMMdd");
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MMDD     = DateTimeFormatter.ofPattern("MMdd");

    /** Format date as YYMMDD (used in most SWIFT fields) */
    public static String formatDate(LocalDate date) {
        return date == null ? "" : date.format(YYMMDD);
    }

    /** Format date as YYYYMMDD */
    public static String formatDateLong(LocalDate date) {
        return date == null ? "" : date.format(YYYYMMDD);
    }

    /** Format date as MMDD (entry date suffix in Field 61) */
    public static String formatMonthDay(LocalDate date) {
        return date == null ? "" : date.format(MMDD);
    }

    /**
     * Format amount in SWIFT notation: comma as decimal separator.
     * e.g. 1234567.89 → "1234567,89"
     */
    public static String formatAmount(BigDecimal amount) {
        if (amount == null) return "0,";
        BigDecimal scaled = amount.setScale(2, java.math.RoundingMode.HALF_UP);
        return scaled.toPlainString().replace(".", ",");
    }

    /** Parse a SWIFT amount string (comma decimal) back to BigDecimal */
    public static BigDecimal parseAmount(String swiftAmount) {
        if (swiftAmount == null || swiftAmount.isBlank()) return BigDecimal.ZERO;
        return new BigDecimal(swiftAmount.replace(",", "."));
    }

    /** Pads a BIC to 11 characters, appending "XXX" if it is only 8 */
    public static String bic11(String bic) {
        if (bic == null) return "";
        if (bic.length() == 8) return bic + "XXX";
        return bic;
    }

    /** Returns the first 8 characters of a BIC */
    public static String bic8(String bic) {
        if (bic == null) return "";
        return bic.length() > 8 ? bic.substring(0, 8) : bic;
    }

    /**
     * Sanitises a string for SWIFT character set X:
     * removes characters outside A-Z, a-z, 0-9, / - ? : ( ) . , ' + space newline
     */
    public static String sanitizeSwiftX(String input) {
        if (input == null) return "";
        return input.replaceAll("[^A-Za-z0-9/\\-?:(). ,'+ \n]", "");
    }

    /** Truncates a string to maxLen characters */
    public static String trunc(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    /**
     * Formats the value for a combined account/name/address SWIFT field
     * such as field 50K, 52D, 59, etc.
     */
    public static String formatNameAddress(String accountNo, String name,
                                           String address1, String address2) {
        StringBuilder sb = new StringBuilder();
        if (accountNo != null && !accountNo.isBlank())
            sb.append("/").append(trunc(accountNo, 34)).append("\n");
        if (name     != null && !name.isBlank())     sb.append(trunc(name, 35)).append("\n");
        if (address1 != null && !address1.isBlank()) sb.append(trunc(address1, 35)).append("\n");
        if (address2 != null && !address2.isBlank()) sb.append(trunc(address2, 35)).append("\n");
        String result = sb.toString();
        if (result.endsWith("\n")) result = result.substring(0, result.length() - 1);
        return result;
    }

    /**
     * Formats a BIC-based party field:  [/account]\nBIC11
     */
    public static String formatBicParty(String accountNo, String bic) {
        StringBuilder sb = new StringBuilder();
        if (accountNo != null && !accountNo.isBlank())
            sb.append("/").append(trunc(accountNo, 34)).append("\n");
        sb.append(bic11(bic));
        return sb.toString();
    }

    /** Left-pads a number with zeros to the given width */
    public static String zeroPad(int n, int width) {
        return String.format("%0" + width + "d", n);
    }

    /** Generates a pseudo-random alphanumeric reference of specified length */
    public static String generateReference(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
