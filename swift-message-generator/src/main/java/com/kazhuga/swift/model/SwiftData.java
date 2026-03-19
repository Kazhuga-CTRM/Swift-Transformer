package com.kazhuga.swift.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain-old-Java-object models used to supply data to the message builders.
 * All inner classes are static so they can be used independently.
 */
public final class SwiftData {

    private SwiftData() {}

    // =========================================================================
    //  BankParty – identifies a bank or institution
    // =========================================================================
    public static class BankParty {
        public String bic;
        public String accountNo;
        public String name;
        public String address1;
        public String address2;
        public String city;
        public String country;

        public BankParty() {}
        public BankParty(String bic) { this.bic = bic; }
        public BankParty(String bic, String accountNo, String name) {
            this.bic = bic; this.accountNo = accountNo; this.name = name;
        }

        /** 8-char BIC (strip branch code if 11 chars given) */
        public String bic8() {
            if (bic == null) return "";
            return bic.length() > 8 ? bic.substring(0, 8) : bic;
        }

        /** Full BIC with branch code (padded with XXX if not supplied) */
        public String bic11() {
            if (bic == null) return "";
            if (bic.length() == 8) return bic + "XXX";
            return bic;
        }

        @Override
        public String toString() { return "BankParty{bic='" + bic + "', name='" + name + "'}"; }
    }

    // =========================================================================
    //  Customer – ordering customer or beneficiary
    // =========================================================================
    public static class Customer {
        public String accountNo;
        public String name;
        public String address1;
        public String address2;
        public String city;
        public String country;
        public String bic;

        public Customer() {}
        public Customer(String accountNo, String name) {
            this.accountNo = accountNo; this.name = name;
        }

        @Override
        public String toString() { return "Customer{account='" + accountNo + "', name='" + name + "'}"; }
    }

    // =========================================================================
    //  TransferData – input data for MT103 / MT202 / MT202COV
    // =========================================================================
    public static class TransferData {
        public String     transactionReference;    // Field 20
        public String     relatedReference;         // Field 21 (MT202)
        public String     bankOperationCode;        // Field 23B
        public String     instructionCode;          // Field 23E (optional)
        public LocalDate  valueDate;                // Field 32A
        public String     currency;                 // ISO 4217
        public BigDecimal amount;
        public String     instructedCurrency;       // Field 33B (optional)
        public BigDecimal instructedAmount;         // Field 33B (optional)
        public BigDecimal exchangeRate;             // Field 36  (optional)

        // Parties
        public Customer  orderingCustomer;          // Field 50K/50A/50F
        public BankParty orderingInstitution;       // Field 52A
        public BankParty senderCorrespondent;       // Field 53A
        public BankParty receiverCorrespondent;     // Field 54A
        public BankParty intermediaryBank;          // Field 56A
        public BankParty accountWithInstitution;    // Field 57A
        public Customer  beneficiaryCustomer;       // Field 59/59A/59F

        // Details
        public String     remittanceInfo;           // Field 70
        public String     detailsOfCharges;         // Field 71A (OUR / BEN / SHA)
        public String     senderToReceiverInfo;     // Field 72
        public String     regulatoryReporting;      // Field 77B

        // Header overrides (auto-generated if blank)
        public String senderBic;
        public String receiverBic;
    }

    // =========================================================================
    //  StatementLine – a single line in an MT940/MT950 statement (Field 61)
    // =========================================================================
    public static class StatementLine {
        public LocalDate  valueDate;
        public LocalDate  entryDate;          // optional (month-day only in FIN)
        public boolean    isCredit;           // D or C
        public boolean    isFunds;            // F = funds indicator
        public BigDecimal amount;
        public String     transactionType;    // 3-char SWIFT code e.g. "TRF", "CHK"
        public String     referenceForAccountOwner;
        public String     referenceOfAccountServicingInstitution;  // optional
        public String     additionalInfo;     // Field 86

        public StatementLine() {}

        public StatementLine(LocalDate valueDate, boolean isCredit, BigDecimal amount,
                             String txType, String ref) {
            this.valueDate             = valueDate;
            this.isCredit              = isCredit;
            this.amount                = amount;
            this.transactionType       = txType;
            this.referenceForAccountOwner = ref;
        }
    }

    // =========================================================================
    //  StatementData – input data for MT940 / MT950
    // =========================================================================
    public static class StatementData {
        public String     transactionReference;    // Field 20
        public String     accountIdentification;   // Field 25
        public String     accountBic;              // Field 25P (optional)
        public int        statementNumber;          // Field 28C first part
        public int        sequenceNumber;           // Field 28C second part (0 = omit)
        public boolean    isIntermediate;           // true → 60M/62M, false → 60F/62F
        public char       openingBalanceIndicator;  // C or D
        public LocalDate  openingDate;
        public String     currency;
        public BigDecimal openingBalance;
        public BigDecimal closingBalance;
        public char       closingBalanceIndicator;
        public LocalDate  closingDate;
        public BigDecimal closingAvailableBalance;  // Field 64 (optional)
        public LocalDate  closingAvailableDate;

        public List<StatementLine> lines = new ArrayList<>();

        // Header
        public String senderBic;
        public String receiverBic;
    }
}
