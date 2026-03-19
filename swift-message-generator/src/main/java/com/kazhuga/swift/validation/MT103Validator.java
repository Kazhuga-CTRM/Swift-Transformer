package com.kazhuga.swift.validation;

import com.kazhuga.swift.model.SwiftData.TransferData;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates the data needed to build an MT103 message.
 *
 * Covers mandatory fields, format constraints, and key business rules
 * defined in the SWIFT Standards Release Guide.
 */
public class MT103Validator {

    private static final Pattern BIC_PATTERN      = Pattern.compile("[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?");
    private static final Pattern REF_PATTERN      = Pattern.compile("[A-Za-z0-9/\\-?:(). ,'+]{1,16}");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("[A-Z]{3}");

    private static final Set<String> VALID_BANK_OP_CODES =
            new HashSet<>(Arrays.asList("CRED", "CRTS", "SPAY", "SSTD", "SPRI"));

    private static final Set<String> VALID_CHARGE_CODES =
            new HashSet<>(Arrays.asList("OUR", "BEN", "SHA"));

    public ValidationResult validate(TransferData data) {
        ValidationResult result = new ValidationResult();

        if (data == null) {
            result.addError("ALL", "TransferData must not be null");
            return result;
        }

        // Field 20 – Transaction Reference
        if (isBlank(data.transactionReference)) {
            result.addError("20", "Transaction Reference (field 20) is mandatory");
        } else if (data.transactionReference.length() > 16) {
            result.addError("20", "Transaction Reference must not exceed 16 characters");
        } else if (!REF_PATTERN.matcher(data.transactionReference).matches()) {
            result.addError("20", "Transaction Reference contains invalid SWIFT characters");
        }

        // Field 23B – Bank Operation Code
        if (isBlank(data.bankOperationCode)) {
            result.addError("23B", "Bank Operation Code (field 23B) is mandatory");
        } else if (!VALID_BANK_OP_CODES.contains(data.bankOperationCode.toUpperCase())) {
            result.addWarning("23B", "Unrecognised Bank Operation Code: " + data.bankOperationCode
                    + ". Expected one of: " + VALID_BANK_OP_CODES);
        }

        // Field 32A – Value Date / Currency / Amount
        if (data.valueDate == null) {
            result.addError("32A", "Value Date (field 32A) is mandatory");
        } else if (data.valueDate.getYear() < 2000) {
            result.addWarning("32A", "Value Date appears to be before year 2000 – verify");
        }
        validateCurrency(result, "32A", data.currency);
        validateAmount(result, "32A", data.amount);

        // Field 50 – Ordering Customer
        if (data.orderingCustomer == null) {
            result.addError("50K", "Ordering Customer (field 50) is mandatory");
        } else if (isBlank(data.orderingCustomer.name)) {
            result.addWarning("50K", "Ordering Customer name is blank");
        }

        // Field 59 – Beneficiary Customer
        if (data.beneficiaryCustomer == null) {
            result.addError("59", "Beneficiary Customer (field 59) is mandatory");
        } else if (isBlank(data.beneficiaryCustomer.name)) {
            result.addWarning("59", "Beneficiary Customer name is blank");
        }

        // Field 71A – Details of Charges
        if (isBlank(data.detailsOfCharges)) {
            result.addError("71A", "Details of Charges (field 71A) is mandatory");
        } else if (!VALID_CHARGE_CODES.contains(data.detailsOfCharges.toUpperCase())) {
            result.addError("71A", "Details of Charges must be one of OUR, BEN, SHA – got: "
                    + data.detailsOfCharges);
        }

        // Optional BIC validations
        validateBic(result, "52A", data.orderingInstitution   != null ? data.orderingInstitution.bic   : null, false);
        validateBic(result, "53A", data.senderCorrespondent   != null ? data.senderCorrespondent.bic   : null, false);
        validateBic(result, "54A", data.receiverCorrespondent != null ? data.receiverCorrespondent.bic : null, false);
        validateBic(result, "56A", data.intermediaryBank      != null ? data.intermediaryBank.bic      : null, false);
        validateBic(result, "57A", data.accountWithInstitution!= null ? data.accountWithInstitution.bic: null, false);

        // Field 33B – Instructed Amount (optional)
        if (data.instructedAmount != null) {
            validateCurrency(result, "33B", data.instructedCurrency);
            if (data.instructedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                result.addError("33B", "Instructed Amount must be positive");
            }
        }

        // Field 70 – Remittance Info length
        if (!isBlank(data.remittanceInfo) && data.remittanceInfo.length() > 140) {
            result.addWarning("70", "Remittance Info exceeds 140 characters; SWIFT limit is 4×35");
        }

        return result;
    }

    // -------------------------------------------------------------------------

    private void validateCurrency(ValidationResult r, String field, String currency) {
        if (isBlank(currency)) {
            r.addError(field, "Currency code is mandatory");
        } else if (!CURRENCY_PATTERN.matcher(currency).matches()) {
            r.addError(field, "Currency must be a 3-letter ISO 4217 code, got: " + currency);
        }
    }

    private void validateAmount(ValidationResult r, String field, BigDecimal amount) {
        if (amount == null) {
            r.addError(field, "Amount is mandatory");
        } else if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            r.addError(field, "Amount must be greater than zero");
        } else if (amount.precision() - amount.scale() > 15) {
            r.addError(field, "Amount integer part exceeds 15 digits (SWIFT maximum)");
        }
    }

    private void validateBic(ValidationResult r, String field, String bic, boolean mandatory) {
        if (isBlank(bic)) {
            if (mandatory) r.addError(field, "BIC is mandatory for field " + field);
            return;
        }
        if (!BIC_PATTERN.matcher(bic.toUpperCase()).matches()) {
            r.addError(field, "Invalid BIC format: " + bic + " (expected 8 or 11 chars: 4a+2a+2c[+3c])");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
