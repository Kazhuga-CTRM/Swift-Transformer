package com.kazhuga.swift.iso20022.validation;

import com.kazhuga.swift.iso20022.model.Pain009Data.*;
import com.kazhuga.swift.validation.ValidationResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates a pain.009 (MandateInitiationRequest) message and its mandates.
 *
 * Applies rules from:
 *   - ISO 20022 pain.009.001.08 schema
 *   - SEPA Credit Transfer rulebook (where applicable)
 *   - EPC Direct Debit scheme rules
 */
public class Pain009Validator {

    private static final Pattern BIC_PATTERN      = Pattern.compile("[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?");
    private static final Pattern IBAN_PATTERN     = Pattern.compile("[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("[A-Z]{3}");
    private static final Pattern MSG_ID_PATTERN   = Pattern.compile("[A-Za-z0-9/\\-?:(). ,'+]{1,35}");

    private static final Set<String> VALID_SEQUENCE_TYPES =
            new HashSet<>(Arrays.asList("FRST", "RCUR", "FNAL", "OOFF"));

    private static final Set<String> VALID_LOCAL_INSTRUMENTS =
            new HashSet<>(Arrays.asList("CORE", "B2B", "COR1", "PRIV"));

    private static final Set<String> VALID_FREQUENCY_CODES =
            new HashSet<>(Arrays.asList("DAIL", "WEEK", "TOWK", "MNTH", "TOMN", "QUTR", "SEMI", "YEAR", "ADHO"));

    // =========================================================================

    public ValidationResult validate(Pain009Message message) {
        ValidationResult result = new ValidationResult();

        if (message == null) {
            result.addError("Document", "pain.009 message must not be null");
            return result;
        }

        validateGroupHeader(result, message.groupHeader != null
                ? message.groupHeader : null,
                message.mandates != null ? message.mandates.size() : 0);

        if (message.mandates == null || message.mandates.isEmpty()) {
            result.addError("Mandate", "At least one mandate is required");
        } else {
            for (int i = 0; i < message.mandates.size(); i++) {
                validateMandate(result, message.mandates.get(i), i + 1);
            }
        }

        return result;
    }

    // -------------------------------------------------------------------------

    private void validateGroupHeader(ValidationResult r, GroupHeader hdr, int mandateCount) {
        if (hdr == null) {
            r.addError("GrpHdr", "Group Header is mandatory");
            return;
        }

        // Message ID
        if (isBlank(hdr.messageId)) {
            r.addError("GrpHdr/MsgId", "Message ID is mandatory (max 35 chars)");
        } else if (hdr.messageId.length() > 35) {
            r.addError("GrpHdr/MsgId", "Message ID must not exceed 35 characters");
        }

        // Creation date/time
        if (hdr.creationDateTime == null) {
            r.addError("GrpHdr/CreDtTm", "Creation date/time is mandatory");
        }

        // Number of transactions
        if (hdr.numberOfTransactions <= 0) {
            r.addError("GrpHdr/NbOfTxs", "Number of transactions must be at least 1");
        } else if (hdr.numberOfTransactions != mandateCount) {
            r.addError("GrpHdr/NbOfTxs",
                    "Declared number of transactions (" + hdr.numberOfTransactions
                    + ") does not match actual mandate count (" + mandateCount + ")");
        }

        // Initiating party
        if (hdr.initiatingParty == null || isBlank(hdr.initiatingParty.name)) {
            r.addError("GrpHdr/InitgPty", "Initiating party name is mandatory");
        }
    }

    // -------------------------------------------------------------------------

    private void validateMandate(ValidationResult r, MandateData m, int index) {
        String pfx = "Mandate[" + index + "]";

        if (m == null) {
            r.addError(pfx, "Mandate object must not be null");
            return;
        }

        // ── Identification ────────────────────────────────────────────────────
        if (isBlank(m.mandateId)) {
            r.addError(pfx + "/MndtId", "Mandate ID is mandatory (max 35 chars)");
        } else if (m.mandateId.length() > 35) {
            r.addError(pfx + "/MndtId", "Mandate ID must not exceed 35 characters");
        }

        if (isBlank(m.mandateRequestId)) {
            r.addError(pfx + "/MndtReqId", "Mandate Request ID is mandatory (max 35 chars)");
        } else if (m.mandateRequestId.length() > 35) {
            r.addError(pfx + "/MndtReqId", "Mandate Request ID must not exceed 35 characters");
        }

        // ── Type ──────────────────────────────────────────────────────────────
        if (m.type == null) {
            r.addError(pfx + "/Tp", "Mandate type is mandatory");
        } else {
            if (isBlank(m.type.sequenceType)) {
                r.addError(pfx + "/Tp/SeqTp", "Sequence type is mandatory");
            } else if (!VALID_SEQUENCE_TYPES.contains(m.type.sequenceType)) {
                r.addError(pfx + "/Tp/SeqTp",
                        "Invalid sequence type '" + m.type.sequenceType
                        + "'. Valid: " + VALID_SEQUENCE_TYPES);
            }
            if (!isBlank(m.type.localInstrumentCode)
                    && !VALID_LOCAL_INSTRUMENTS.contains(m.type.localInstrumentCode)) {
                r.addWarning(pfx + "/Tp/LclInstrm",
                        "Unrecognised local instrument code: " + m.type.localInstrumentCode
                        + ". Expected one of: " + VALID_LOCAL_INSTRUMENTS);
            }
        }

        // ── Frequency ─────────────────────────────────────────────────────────
        if (m.frequency == null) {
            r.addError(pfx + "/Ocrncs/Frqcy", "Frequency is mandatory");
        } else if (!isBlank(m.frequency.code)
                   && !VALID_FREQUENCY_CODES.contains(m.frequency.code)) {
            r.addError(pfx + "/Ocrncs/Frqcy",
                    "Invalid frequency code '" + m.frequency.code
                    + "'. Valid: " + VALID_FREQUENCY_CODES);
        }

        // ── Collection dates ──────────────────────────────────────────────────
        if (m.firstCollectionDate == null) {
            r.addError(pfx + "/FrstColltnDt", "First collection date is mandatory");
        } else if (m.firstCollectionDate.isBefore(LocalDate.now())) {
            r.addWarning(pfx + "/FrstColltnDt",
                    "First collection date is in the past — verify if intentional");
        }

        if (m.finalCollectionDate != null && m.firstCollectionDate != null
                && !m.finalCollectionDate.isAfter(m.firstCollectionDate)) {
            r.addError(pfx + "/FnlColltnDt",
                    "Final collection date must be after first collection date");
        }

        // ── Amount / currency ─────────────────────────────────────────────────
        if (m.maximumAmount != null) {
            if (m.maximumAmount.compareTo(BigDecimal.ZERO) <= 0) {
                r.addError(pfx + "/MaxAmt", "Maximum amount must be positive");
            }
            validateCurrency(r, pfx + "/MaxAmt@Ccy", m.currency);
        }

        // ── Creditor ──────────────────────────────────────────────────────────
        validateParty(r, pfx + "/Cdtr",   m.creditor,      true);
        validateAccount(r, pfx + "/CdtrAcct", m.creditorAccount, true);
        validateAgent(r, pfx + "/CdtrAgt",   m.creditorAgent,   true);

        // ── Debtor ────────────────────────────────────────────────────────────
        validateParty(r, pfx + "/Dbtr",    m.debtor,        true);
        validateAccount(r, pfx + "/DbtrAcct", m.debtorAccount, true);
        validateAgent(r, pfx + "/DbtrAgt",   m.debtorAgent,   true);

        // ── Signature ─────────────────────────────────────────────────────────
        if (m.signatureDate == null) {
            r.addWarning(pfx + "/MndtRltdInf/DtOfSgntr",
                    "Signature date is recommended — some schemes require it");
        }

        // ── Amendment cross-checks ────────────────────────────────────────────
        if (m.amendment != null && m.amendment.hasAmendments()) {
            if (isBlank(m.originalMandateId)) {
                r.addError(pfx + "/Amdmnt/OrgnlMndtId",
                        "Original mandate ID is required when amendment flags are set");
            }
        }
    }

    // -------------------------------------------------------------------------

    private void validateParty(ValidationResult r, String path, PartyId party, boolean mandatory) {
        if (party == null) {
            if (mandatory) r.addError(path, "Party is mandatory");
            return;
        }
        if (isBlank(party.name)) {
            r.addError(path + "/Nm", "Party name is mandatory (max 140 chars)");
        } else if (party.name.length() > 140) {
            r.addError(path + "/Nm", "Party name must not exceed 140 characters");
        }
        if (!isBlank(party.lei) && party.lei.length() != 20) {
            r.addError(path + "/Id/LEI", "LEI must be exactly 20 characters");
        }
    }

    private void validateAccount(ValidationResult r, String path, AccountId acct, boolean mandatory) {
        if (acct == null) {
            if (mandatory) r.addError(path, "Account identification is mandatory");
            return;
        }
        if (isBlank(acct.iban) && isBlank(acct.bban)) {
            r.addError(path + "/Id", "Either IBAN or BBAN must be provided");
        }
        if (!isBlank(acct.iban) && !IBAN_PATTERN.matcher(acct.iban).matches()) {
            r.addError(path + "/Id/IBAN",
                    "IBAN format invalid: must start with 2-letter country code and check digits");
        }
        if (!isBlank(acct.currency)) {
            validateCurrency(r, path + "/Ccy", acct.currency);
        }
    }

    private void validateAgent(ValidationResult r, String path, BankAgent agent, boolean mandatory) {
        if (agent == null) {
            if (mandatory) r.addError(path, "Bank agent is mandatory");
            return;
        }
        if (isBlank(agent.bic) && isBlank(agent.memberIdentification)) {
            r.addError(path + "/FinInstnId",
                    "Either BIC or clearing system member ID must be provided");
        }
        if (!isBlank(agent.bic) && !BIC_PATTERN.matcher(agent.bic.toUpperCase()).matches()) {
            r.addError(path + "/FinInstnId/BICFI",
                    "Invalid BIC format: " + agent.bic);
        }
    }

    private void validateCurrency(ValidationResult r, String path, String currency) {
        if (isBlank(currency)) {
            r.addError(path, "Currency code is mandatory");
        } else if (!CURRENCY_PATTERN.matcher(currency).matches()) {
            r.addError(path, "Currency must be a 3-letter ISO 4217 code, got: " + currency);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}


// ─── Adapter to make Pain009Message fields accessible ────────────────────────
