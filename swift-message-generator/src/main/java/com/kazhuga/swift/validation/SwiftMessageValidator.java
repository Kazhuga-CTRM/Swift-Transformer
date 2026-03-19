package com.kazhuga.swift.validation;

import com.kazhuga.swift.model.SwiftData.StatementData;
import com.kazhuga.swift.model.SwiftData.StatementLine;
import com.kazhuga.swift.model.SwiftData.TransferData;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Validators for MT202, MT202COV, MT940, and MT950 messages.
 */
public class SwiftMessageValidator {

    private static final Pattern CURRENCY_PATTERN = Pattern.compile("[A-Z]{3}");

    // =========================================================================
    //  MT202 / MT202COV
    // =========================================================================

    public ValidationResult validateMT202(TransferData data) {
        return validateMT202Internal(data, false);
    }

    public ValidationResult validateMT202COV(TransferData data) {
        return validateMT202Internal(data, true);
    }

    private ValidationResult validateMT202Internal(TransferData data, boolean isCov) {
        ValidationResult result  = new ValidationResult();
        String           msgType = isCov ? "MT202COV" : "MT202";

        if (data == null) {
            result.addError("ALL", msgType + " TransferData must not be null");
            return result;
        }

        if (isBlank(data.transactionReference)) {
            result.addError("20", "Transaction Reference is mandatory");
        } else if (data.transactionReference.length() > 16) {
            result.addError("20", "Transaction Reference must not exceed 16 characters");
        }

        if (isBlank(data.relatedReference)) {
            result.addError("21", "Related Reference (field 21) is mandatory for " + msgType);
        } else if (data.relatedReference.length() > 16) {
            result.addError("21", "Related Reference must not exceed 16 characters");
        }

        if (data.valueDate == null) result.addError("32A", "Value Date is mandatory");
        validateCurrency(result, "32A", data.currency);
        validateAmount(result, "32A", data.amount);

        if (data.beneficiaryCustomer == null && data.accountWithInstitution == null) {
            result.addError("58A", "Beneficiary Institution (field 58A) is mandatory for " + msgType);
        }

        if (isCov) {
            if (data.orderingCustomer == null || isBlank(data.orderingCustomer.name)) {
                result.addWarning("50", "MT202COV should include underlying ordering customer (50K)");
            }
            if (data.beneficiaryCustomer == null || isBlank(data.beneficiaryCustomer.name)) {
                result.addWarning("59", "MT202COV should include underlying beneficiary customer (59)");
            }
        }

        return result;
    }

    // =========================================================================
    //  MT940
    // =========================================================================

    public ValidationResult validateMT940(StatementData data) {
        ValidationResult result = new ValidationResult();

        if (data == null) {
            result.addError("ALL", "StatementData must not be null");
            return result;
        }

        if (isBlank(data.transactionReference))  result.addError("20", "Transaction Reference is mandatory");
        if (isBlank(data.accountIdentification)) result.addError("25", "Account Identification (field 25) is mandatory");
        if (data.statementNumber <= 0)           result.addWarning("28C", "Statement number should be positive");

        validateCurrency(result, "60F/62F", data.currency);

        if (data.openingBalance == null) result.addError("60F", "Opening Balance is mandatory");
        if (data.closingBalance == null) result.addError("62F", "Closing Balance is mandatory");
        if (data.openingDate    == null) result.addError("60F", "Opening Balance date is mandatory");
        if (data.closingDate    == null) result.addError("62F", "Closing Balance date is mandatory");

        if (data.openingBalanceIndicator != 'C' && data.openingBalanceIndicator != 'D') {
            result.addError("60F", "Opening balance indicator must be 'C' (credit) or 'D' (debit)");
        }
        if (data.closingBalanceIndicator != 'C' && data.closingBalanceIndicator != 'D') {
            result.addError("62F", "Closing balance indicator must be 'C' (credit) or 'D' (debit)");
        }

        if (data.lines != null) {
            for (int i = 0; i < data.lines.size(); i++) {
                StatementLine line   = data.lines.get(i);
                String        prefix = "Line[" + (i + 1) + "]";
                if (line.valueDate == null)
                    result.addError("61." + prefix, "Statement line value date is mandatory");
                if (line.amount == null || line.amount.compareTo(BigDecimal.ZERO) <= 0)
                    result.addError("61." + prefix, "Statement line amount must be positive");
                if (isBlank(line.transactionType))
                    result.addWarning("61." + prefix, "Transaction type code is missing");
                if (isBlank(line.referenceForAccountOwner))
                    result.addError("61." + prefix, "Reference for account owner is mandatory");
            }
        }

        return result;
    }

    // =========================================================================
    //  MT950
    // =========================================================================

    public ValidationResult validateMT950(StatementData data) {
        ValidationResult result = validateMT940(data);
        result.addInfo("86", "Field 86 (Information to Account Owner) is not used in MT950");
        return result;
    }

    // -------------------------------------------------------------------------

    private void validateCurrency(ValidationResult r, String field, String currency) {
        if (isBlank(currency)) {
            r.addError(field, "Currency code is mandatory");
        } else if (!CURRENCY_PATTERN.matcher(currency).matches()) {
            r.addError(field, "Currency must be 3-letter ISO code, got: " + currency);
        }
    }

    private void validateAmount(ValidationResult r, String field, BigDecimal amount) {
        if (amount == null) {
            r.addError(field, "Amount is mandatory");
        } else if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            r.addError(field, "Amount must be greater than zero");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
