package com.kazhuga.swift.iso20022.pain009;

import com.kazhuga.swift.iso20022.model.Pain009Data.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Realistic sample pain.009 messages for testing and demonstration.
 *
 * Scenarios covered:
 *   1. SEPA Core recurring direct debit – energy supplier collecting monthly gas fees
 *   2. B2B mandate – commodities firm authorising weekly margin calls
 *   3. One-off mandate – single settlement instruction for a spot trade
 *   4. Amendment to an existing mandate
 */
public final class Pain009SampleFactory {

    private Pain009SampleFactory() {}

    // =========================================================================
    //  Scenario 1 – SEPA Core Recurring Mandate
    //  Energy supplier collecting monthly gas fees from industrial customer
    // =========================================================================

    public static Pain009Message sepaCoreMonthlySample() {

        // ── Group Header ──────────────────────────────────────────────────────
        GroupHeader hdr = new GroupHeader();
        hdr.messageId          = "KAZHUGA-PAIN009-001";
        hdr.creationDateTime   = LocalDateTime.of(2024, 3, 15, 9, 30, 0);
        hdr.numberOfTransactions = 1;

        PartyId initiatingParty = new PartyId("KAZHUGA ENERGY TRADING LTD");
        initiatingParty.lei = "5493001KJTIIGC8Y1R12";
        hdr.initiatingParty = initiatingParty;

        Pain009Message message = new Pain009Message(hdr);

        // ── Mandate ───────────────────────────────────────────────────────────
        MandateData m = new MandateData();
        m.mandateId        = "MNDT-ENRG-2024-00145";
        m.mandateRequestId = "MREQ-ENRG-2024-00145";
        m.creditorSchemeId = "DE98ZZZ09999999999";      // SEPA creditor identifier

        // Type: SEPA Core, recurring
        m.type = new MandateType();
        m.type.sequenceType       = "RCUR";
        m.type.localInstrumentCode = "CORE";
        m.type.categoryPurposeCode = "ENRG";            // Energy payment

        // Monthly collection
        m.frequency = new Frequency("MNTH");
        m.firstCollectionDate  = LocalDate.of(2024, 4, 1);
        m.finalCollectionDate  = LocalDate.of(2026, 3, 31);

        // Max collection EUR 50,000 per month
        m.maximumAmount = new BigDecimal("50000.00");
        m.currency      = "EUR";

        // Creditor (energy supplier)
        m.creditor = new PartyId("KAZHUGA ENERGY TRADING LTD");
        m.creditor.address = new PostalAddress(
                "Theodor-Heuss-Allee 70", "Frankfurt am Main", "60486", "DE");

        m.creditorAccount = new AccountId("DE89370400440532013000", "EUR");
        m.creditorAgent   = new BankAgent("DEUTDEDB", "DEUTSCHE BANK AG");

        // Debtor (industrial gas customer)
        m.debtor = new PartyId("RHEIN INDUSTRIEWERKE GMBH");
        m.debtor.lei     = "5493001KJTIIGC8Y1R99";
        m.debtor.address = new PostalAddress(
                "Industriestrasse 45", "Duisburg", "47051", "DE");

        m.debtorAccount = new AccountId("DE44500105175407324931", "EUR");
        m.debtorAgent   = new BankAgent("COBADEFFXXX", "COMMERZBANK AG");

        m.signatureDate  = LocalDate.of(2024, 3, 10);
        m.signaturePlace = "Frankfurt am Main";
        m.purposeCode    = "ENRG";
        m.remittanceInformation = "MONTHLY GAS SUPPLY CONTRACT REF/2024/ENRG/00145";

        message.addMandate(m);
        return message;
    }

    // =========================================================================
    //  Scenario 2 – B2B Mandate for Weekly Commodities Margin Calls
    //  Commodities firm authorising exchange to collect variation margin weekly
    // =========================================================================

    public static Pain009Message commoditiesB2BSample() {

        GroupHeader hdr = new GroupHeader();
        hdr.messageId          = "KAZHUGA-PAIN009-002";
        hdr.creationDateTime   = LocalDateTime.of(2024, 3, 15, 10, 0, 0);
        hdr.numberOfTransactions = 1;

        hdr.initiatingParty = new PartyId("KAZHUGA COMMODITIES PLC");
        hdr.initiatingParty.lei = "5493001KJTIIGC8Y1R34";

        Pain009Message message = new Pain009Message(hdr);

        MandateData m = new MandateData();
        m.mandateId        = "MNDT-CMM-VM-2024-0088";
        m.mandateRequestId = "MREQ-CMM-VM-2024-0088";

        // B2B, weekly variation margin
        m.type = new MandateType();
        m.type.sequenceType        = "RCUR";
        m.type.localInstrumentCode = "B2B";
        m.type.categoryPurposeCode = "MARG";           // Margin payment

        m.frequency = new Frequency("WEEK");
        m.firstCollectionDate = LocalDate.of(2024, 4, 5);
        m.finalCollectionDate = LocalDate.of(2024, 12, 31);

        // No fixed maximum — margin calls are variable; include advisory cap
        m.maximumAmount = new BigDecimal("5000000.00");
        m.currency      = "USD";

        // Creditor: exchange clearing house
        m.creditor = new PartyId("LME CLEAR LIMITED");
        m.creditor.address = new PostalAddress(
                "56 Leadenhall Street", "London", "EC3A 2DX", "GB");

        m.creditorAccount = new AccountId("GB29NWBK60161331926819", "USD");
        m.creditorAgent   = new BankAgent("NWBKGB2L", "NATWEST BANK PLC");

        // Debtor: commodity trading firm
        m.debtor = new PartyId("KAZHUGA COMMODITIES PLC");
        m.debtor.lei     = "5493001KJTIIGC8Y1R34";
        m.debtor.address = new PostalAddress(
                "30 St Mary Axe", "London", "EC3A 8BF", "GB");

        m.debtorAccount = new AccountId("GB12MIDL40051512345678", "USD");
        m.debtorAgent   = new BankAgent("MIDLGB22XXX", "HSBC BANK PLC");

        m.signatureDate  = LocalDate.of(2024, 3, 14);
        m.signaturePlace = "London";
        m.purposeCode    = "MARG";
        m.remittanceInformation =
                "LME VARIATION MARGIN MNDT/2024/0088 METALS FUTURES PORTFOLIO";

        message.addMandate(m);
        return message;
    }

    // =========================================================================
    //  Scenario 3 – One-off mandate for spot trade settlement
    //  Single collection to settle a spot crude oil trade
    // =========================================================================

    public static Pain009Message oneOffSpotTradeSample() {

        GroupHeader hdr = new GroupHeader();
        hdr.messageId          = "KAZHUGA-PAIN009-003";
        hdr.creationDateTime   = LocalDateTime.of(2024, 3, 15, 14, 0, 0);
        hdr.numberOfTransactions = 1;

        hdr.initiatingParty = new PartyId("KAZHUGA ENERGY TRADING LTD");

        Pain009Message message = new Pain009Message(hdr);

        MandateData m = new MandateData();
        m.mandateId        = "MNDT-SPOT-2024-00312";
        m.mandateRequestId = "MREQ-SPOT-2024-00312";

        // One-off — single collection only
        m.type = new MandateType();
        m.type.sequenceType        = "OOFF";
        m.type.localInstrumentCode = "CORE";

        m.frequency = new Frequency("ADHO");           // Ad-hoc

        // Fixed amount — spot trade settlement
        m.firstCollectionDate = LocalDate.of(2024, 3, 20);
        // No final date — OOFF collects once

        m.maximumAmount = new BigDecimal("2375000.00"); // 50,000 bbl × USD 47.50
        m.currency      = "USD";

        // Creditor: oil producer
        m.creditor = new PartyId("NORTH SEA OIL CORP SA");
        m.creditor.address = new PostalAddress(
                "Rue du Rhone 14", "Geneva", "1204", "CH");

        m.creditorAccount = new AccountId("CH5604835012345678009", "USD");
        m.creditorAgent   = new BankAgent("UBSWCHZH80A", "UBS SWITZERLAND AG");

        // Debtor: refinery paying for crude delivery
        m.debtor = new PartyId("KAZHUGA ENERGY TRADING LTD");
        m.debtor.address = new PostalAddress(
                "Theodor-Heuss-Allee 70", "Frankfurt am Main", "60486", "DE");

        m.debtorAccount = new AccountId("DE89370400440532013000", "USD");
        m.debtorAgent   = new BankAgent("DEUTDEDB", "DEUTSCHE BANK AG");

        m.signatureDate  = LocalDate.of(2024, 3, 15);
        m.signaturePlace = "Geneva";
        m.purposeCode    = "TRAD";
        m.remittanceInformation =
                "SPOT CRUDE OIL TRADE CONF/2024/03/00312 50000BBL BRENT DATED";

        message.addMandate(m);
        return message;
    }

    // =========================================================================
    //  Scenario 4 – Amendment to an existing mandate
    //  Creditor updating the debtor's account after a bank migration
    // =========================================================================

    public static Pain009Message amendmentSample() {

        GroupHeader hdr = new GroupHeader();
        hdr.messageId          = "KAZHUGA-PAIN009-004";
        hdr.creationDateTime   = LocalDateTime.of(2024, 3, 15, 16, 30, 0);
        hdr.numberOfTransactions = 1;
        hdr.initiatingParty = new PartyId("KAZHUGA ENERGY TRADING LTD");

        Pain009Message message = new Pain009Message(hdr);

        MandateData m = new MandateData();
        m.mandateId        = "MNDT-ENRG-2024-00145";   // Same mandate ID
        m.mandateRequestId = "MREQ-ENRG-2024-00145-AMD01";

        m.type = new MandateType();
        m.type.sequenceType        = "RCUR";
        m.type.localInstrumentCode = "CORE";

        m.frequency           = new Frequency("MNTH");
        m.firstCollectionDate = LocalDate.of(2024, 4, 1);
        m.finalCollectionDate = LocalDate.of(2026, 3, 31);

        m.maximumAmount = new BigDecimal("50000.00");
        m.currency      = "EUR";

        // Amendment: debtor's account has changed (bank migration)
        m.originalMandateId = "MNDT-ENRG-2024-00145";
        m.amendment = new AmendmentIndicator();
        m.amendment.originalDebtorAccountChanged = true;
        m.amendment.originalDebtorAgentChanged   = true;

        // Creditor unchanged
        m.creditor = new PartyId("KAZHUGA ENERGY TRADING LTD");
        m.creditor.address = new PostalAddress(
                "Theodor-Heuss-Allee 70", "Frankfurt am Main", "60486", "DE");
        m.creditorAccount = new AccountId("DE89370400440532013000", "EUR");
        m.creditorAgent   = new BankAgent("DEUTDEDB", "DEUTSCHE BANK AG");

        // Debtor — NEW account after bank migration
        m.debtor = new PartyId("RHEIN INDUSTRIEWERKE GMBH");
        m.debtor.address = new PostalAddress(
                "Industriestrasse 45", "Duisburg", "47051", "DE");

        m.debtorAccount = new AccountId("DE72200400600028490900", "EUR");  // new IBAN
        m.debtorAgent   = new BankAgent("PBNKDEFFXXX", "POSTBANK AG");    // new bank

        m.signatureDate  = LocalDate.of(2024, 3, 12);
        m.signaturePlace = "Duisburg";
        m.purposeCode    = "ENRG";
        m.remittanceInformation =
                "AMENDMENT TO MNDT-ENRG-2024-00145 DEBTOR ACCOUNT MIGRATION";

        message.addMandate(m);
        return message;
    }
}
