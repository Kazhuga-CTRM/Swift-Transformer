package com.kazhuga.swift.demo;

import com.kazhuga.swift.model.SwiftData.BankParty;
import com.kazhuga.swift.model.SwiftData.Customer;
import com.kazhuga.swift.model.SwiftData.StatementData;
import com.kazhuga.swift.model.SwiftData.StatementLine;
import com.kazhuga.swift.model.SwiftData.TransferData;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Factory that produces realistic sample data objects for each message type.
 *
 * Use these as starting points and replace field values with data from your
 * own systems.
 */
public final class SampleDataFactory {

    private SampleDataFactory() {}

    // =========================================================================
    //  MT103 – Single Customer Credit Transfer
    //  Scenario: US company pays a UK supplier via SWIFT
    // =========================================================================

    public static TransferData sampleMT103() {
        TransferData data = new TransferData();

        // References & codes
        data.transactionReference = "USPAY20230915001";
        data.bankOperationCode    = "CRED";
        data.detailsOfCharges     = "SHA";

        // Value date and amount
        data.valueDate = LocalDate.of(2023, 9, 15);
        data.currency  = "USD";
        data.amount    = new BigDecimal("125000.00");

        // Instructed amount (customer billed in GBP)
        data.instructedCurrency = "GBP";
        data.instructedAmount   = new BigDecimal("99800.00");
        data.exchangeRate       = new BigDecimal("1.25200");

        // Ordering customer (payer)
        Customer oc = new Customer();
        oc.accountNo = "US12345678901234567890";
        oc.name      = "ACME CORPORATION";
        oc.address1  = "100 MAIN STREET";
        oc.address2  = "SUITE 200";
        oc.city      = "NEW YORK";
        oc.country   = "US";
        data.orderingCustomer = oc;

        // Ordering institution (sender's bank)
        BankParty oi = new BankParty();
        oi.bic  = "CHASUS33";
        oi.name = "JPMORGAN CHASE BANK";
        data.orderingInstitution = oi;

        // Sender's correspondent
        data.senderCorrespondent = new BankParty("CITIUS33");

        // Account with institution (beneficiary's bank)
        BankParty awi = new BankParty();
        awi.bic       = "HSBCGB2L";
        awi.accountNo = "GB29NWBK60161331926819";
        awi.name      = "HSBC BANK PLC";
        data.accountWithInstitution = awi;

        // Beneficiary customer (payee)
        Customer bc = new Customer();
        bc.accountNo = "GB29NWBK60161331926819";
        bc.name      = "BRITISH SUPPLIER LTD";
        bc.address1  = "14 CANNON STREET";
        bc.city      = "LONDON";
        bc.country   = "GB";
        data.beneficiaryCustomer = bc;

        // Remittance information
        data.remittanceInfo = "INV/2023/09/00456 PAYMENT FOR SERVICES";

        // Block 1 / 2 sender and receiver BICs
        data.senderBic   = "CHASUS33XXX";
        data.receiverBic = "HSBCGB2LXXX";

        return data;
    }

    // =========================================================================
    //  MT202 – Financial Institution Transfer
    //  Scenario: Nostro account funding in USD
    // =========================================================================

    public static TransferData sampleMT202() {
        TransferData data = new TransferData();

        data.transactionReference = "NOSTRO20230915A";
        data.relatedReference     = "USPAY20230915001";
        data.bankOperationCode    = "CRED";
        data.detailsOfCharges     = "OUR";
        data.valueDate            = LocalDate.of(2023, 9, 15);
        data.currency             = "USD";
        data.amount               = new BigDecimal("5000000.00");

        BankParty oi = new BankParty("CHASUS33");
        oi.name = "JPMORGAN CHASE BANK";
        data.orderingInstitution = oi;

        data.senderCorrespondent   = new BankParty("BOFAUS3N");
        data.receiverCorrespondent = new BankParty("CITIUS33");

        BankParty bene = new BankParty();
        bene.bic       = "HSBCGB2L";
        bene.accountNo = "400-123456";
        bene.name      = "HSBC BANK PLC";
        data.accountWithInstitution = bene;

        data.senderToReceiverInfo = "/ACC/FOR NOSTRO FUNDING";

        data.senderBic   = "CHASUS33XXX";
        data.receiverBic = "CITIUS33XXX";

        return data;
    }

    // =========================================================================
    //  MT202COV – Cover Payment
    //  Scenario: Cover message accompanying the MT103 above
    // =========================================================================

    public static TransferData sampleMT202COV() {
        TransferData data = sampleMT202();
        data.transactionReference = "COVER20230915001";
        data.relatedReference     = "USPAY20230915001";

        Customer oc = new Customer();
        oc.accountNo = "US12345678901234567890";
        oc.name      = "ACME CORPORATION";
        oc.address1  = "100 MAIN STREET, NEW YORK, US";
        data.orderingCustomer = oc;

        Customer bc = new Customer();
        bc.accountNo = "GB29NWBK60161331926819";
        bc.name      = "BRITISH SUPPLIER LTD";
        bc.address1  = "14 CANNON STREET, LONDON, GB";
        data.beneficiaryCustomer = bc;

        data.remittanceInfo = "INV/2023/09/00456 PAYMENT FOR SERVICES";

        return data;
    }

    // =========================================================================
    //  MT940 – Customer Statement
    //  Scenario: Monthly EUR account statement for a German corporate
    // =========================================================================

    public static StatementData sampleMT940() {
        StatementData data = new StatementData();

        data.transactionReference  = "STMT20230930EUR";
        data.accountIdentification = "DE89370400440532013000";
        data.accountBic            = "DEUTDEDB";
        data.statementNumber       = 9;
        data.sequenceNumber        = 1;
        data.currency              = "EUR";
        data.isIntermediate        = false;

        // Opening balance: credit EUR 245,300.00
        data.openingBalanceIndicator = 'C';
        data.openingDate             = LocalDate.of(2023, 9, 1);
        data.openingBalance          = new BigDecimal("245300.00");

        // Statement lines
        //  +12,500  +87,000  +32,750  +15,600  = +147,850
        //  - 3,200  - 1,250  -48,000           = - 52,450
        //  Net change = +95,400  →  closing = 340,700
        addLine(data, LocalDate.of(2023, 9, 4),  true,  new BigDecimal("12500.00"),
                "TRF", "INV456RECEIPT",  "Customer payment for Invoice 456");
        addLine(data, LocalDate.of(2023, 9, 7),  false, new BigDecimal("3200.00"),
                "CHK", "CHQ00012345",   "Cheque payment – supplier");
        addLine(data, LocalDate.of(2023, 9, 11), true,  new BigDecimal("87000.00"),
                "TRF", "CORP-TRF-0911", "Intercompany transfer from HQ");
        addLine(data, LocalDate.of(2023, 9, 15), false, new BigDecimal("1250.00"),
                "FEE", "BANKFEES-SEP",  "Bank service charges September");
        addLine(data, LocalDate.of(2023, 9, 18), true,  new BigDecimal("32750.00"),
                "TRF", "CUST-PMT-0918", "Customer settlement – order 7821");
        addLine(data, LocalDate.of(2023, 9, 22), false, new BigDecimal("48000.00"),
                "TRF", "PAY-VENDOR-09", "Vendor payment – quarterly invoice");
        addLine(data, LocalDate.of(2023, 9, 28), true,  new BigDecimal("15600.00"),
                "INT", "INT-SEP23",     "Interest credited");

        // Closing balance: credit EUR 340,700.00
        data.closingBalanceIndicator = 'C';
        data.closingDate             = LocalDate.of(2023, 9, 30);
        data.closingBalance          = new BigDecimal("340700.00");

        data.closingAvailableBalance = new BigDecimal("340700.00");
        data.closingAvailableDate    = LocalDate.of(2023, 9, 30);

        data.senderBic   = "DEUTDEDBXXX";
        data.receiverBic = "BMUNDE8BXXX";

        return data;
    }

    // =========================================================================
    //  MT950 – Bank Statement (nostro reconciliation)
    // =========================================================================

    public static StatementData sampleMT950() {
        StatementData data = new StatementData();

        data.transactionReference  = "NOSTROSTMT230930";
        data.accountIdentification = "400-123456-USD";
        data.statementNumber       = 3;
        data.sequenceNumber        = 0;
        data.currency              = "USD";
        data.isIntermediate        = false;

        data.openingBalanceIndicator = 'C';
        data.openingDate             = LocalDate.of(2023, 9, 1);
        data.openingBalance          = new BigDecimal("10000000.00");

        //  +2,500,000  + 800,000 = +3,300,000
        //  -1,200,000  -3,100,000 = -4,300,000
        //  Net change = -1,000,000  →  closing = 9,000,000
        addLine(data, LocalDate.of(2023, 9, 5),  true,  new BigDecimal("2500000.00"),
                "TRF", "NOSTRO-FUND-01", null);
        addLine(data, LocalDate.of(2023, 9, 12), false, new BigDecimal("1200000.00"),
                "TRF", "NOSTRO-SETT-02", null);
        addLine(data, LocalDate.of(2023, 9, 19), true,  new BigDecimal("800000.00"),
                "TRF", "NOSTRO-FUND-03", null);
        addLine(data, LocalDate.of(2023, 9, 26), false, new BigDecimal("3100000.00"),
                "TRF", "NOSTRO-SETT-04", null);

        data.closingBalanceIndicator = 'C';
        data.closingDate             = LocalDate.of(2023, 9, 30);
        data.closingBalance          = new BigDecimal("9000000.00");

        data.senderBic   = "CITIUS33XXX";
        data.receiverBic = "CHASUS33XXX";

        return data;
    }

    // -------------------------------------------------------------------------

    private static void addLine(StatementData data,
                                LocalDate date, boolean isCredit,
                                BigDecimal amount, String type,
                                String ref, String info) {
        StatementLine line = new StatementLine(date, isCredit, amount, type, ref);
        line.additionalInfo = info;
        data.lines.add(line);
    }
}
