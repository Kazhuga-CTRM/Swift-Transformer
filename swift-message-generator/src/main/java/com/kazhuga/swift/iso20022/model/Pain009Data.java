package com.kazhuga.swift.iso20022.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data models for ISO 20022 pain.009 – Mandate Initiation Request.
 *
 * pain.009.001.08 (MandateInitiationRequestV08)
 *
 * A pain.009 message is sent by a creditor (or its agent) to a debtor's bank
 * to establish a direct debit mandate. Once accepted, the mandate authorises
 * future pain.008 (CustomerDirectDebitInitiation) collections against the
 * debtor's account.
 *
 * Typical flow in treasury / commodities:
 *   1. Creditor sends pain.009 → Debtor's bank
 *   2. Bank returns pain.010 (MandateAmendmentRequest) or pain.011 (MandateCancellation)
 *   3. Agreed mandate is used in pain.008 DirectDebit collections
 *
 * Compatible with OpenLink Findur/Endur direct debit settlement legs.
 */
public final class Pain009Data {

    private Pain009Data() {}

    // =========================================================================
    //  PartyIdentification – creditor or debtor party
    // =========================================================================
    public static class PartyId {
        public String name;                 // Full legal name
        public String bic;                  // BIC of the party's bank
        public String lei;                  // Legal Entity Identifier (optional)
        public String taxId;               // Tax/registration number (optional)
        public PostalAddress address;

        public PartyId() {}
        public PartyId(String name) { this.name = name; }

        @Override
        public String toString() { return "PartyId{name='" + name + "'}"; }
    }

    // =========================================================================
    //  PostalAddress
    // =========================================================================
    public static class PostalAddress {
        public String streetName;
        public String buildingNumber;
        public String postCode;
        public String townName;
        public String countrySubDivision;
        public String country;             // ISO 3166-1 alpha-2 e.g. "GB"

        public PostalAddress() {}
        public PostalAddress(String street, String town, String postCode, String country) {
            this.streetName    = street;
            this.townName      = town;
            this.postCode      = postCode;
            this.country       = country;
        }
    }

    // =========================================================================
    //  AccountIdentification – IBAN or BBAN
    // =========================================================================
    public static class AccountId {
        public String iban;                // Preferred
        public String bban;                // If IBAN not available
        public String currency;            // ISO 4217
        public String accountName;        // Optional friendly name

        public AccountId() {}
        public AccountId(String iban, String currency) {
            this.iban     = iban;
            this.currency = currency;
        }

        /** Returns IBAN if present, otherwise BBAN */
        public String primary() { return iban != null ? iban : bban; }
    }

    // =========================================================================
    //  BankAgent – BIC + optional clearing system member ID
    // =========================================================================
    public static class BankAgent {
        public String bic;                  // BICFI
        public String clearingSystemCode;   // e.g. "CHAPS", "SEPA", "FEDWIRE"
        public String memberIdentification; // Sort code, ABA routing, etc.
        public String name;
        public PostalAddress address;

        public BankAgent() {}
        public BankAgent(String bic) { this.bic = bic; }
        public BankAgent(String bic, String name) { this.bic = bic; this.name = name; }
    }

    // =========================================================================
    //  MandateTypeInformation – sequence and local instrument
    // =========================================================================
    public static class MandateType {
        /**
         * Sequence type of the direct debit:
         *   FRST – First collection
         *   RCUR – Recurring
         *   FNAL – Final collection
         *   OOFF – One-off
         */
        public String sequenceType = "RCUR";

        /**
         * Local instrument code (scheme):
         *   CORE – SEPA Core Direct Debit
         *   B2B  – SEPA Business-to-Business
         *   COR1 – SEPA Core (next-day)
         *   PRIV – Private/bilateral scheme
         */
        public String localInstrumentCode = "CORE";

        /** Category purpose code e.g. CASH, SUPP, TRAD */
        public String categoryPurposeCode;
    }

    // =========================================================================
    //  Frequency – how often the mandate may be used
    // =========================================================================
    public static class Frequency {
        /**
         * Frequency code:
         *   DAIL, WEEK, TOWK, MNTH, TOMN, QUTR, SEMI, YEAR, ADHO
         */
        public String code = "MNTH";

        /** Point-in-time within period: 1=first day, 31=last day, etc. */
        public Integer pointInTime;

        public Frequency() {}
        public Frequency(String code) { this.code = code; }
    }

    // =========================================================================
    //  AmendmentIndicator – tracks changes to an existing mandate
    // =========================================================================
    public static class AmendmentIndicator {
        public boolean originalMandateIdChanged;
        public boolean originalCreditorSchemeIdChanged;
        public boolean originalCreditorAgentChanged;
        public boolean originalDebtorChanged;
        public boolean originalDebtorAccountChanged;
        public boolean originalDebtorAgentChanged;
        public boolean originalFinalCollectionDateChanged;
        public boolean originalAmountChanged;

        /** Returns true if any amendment flag is set */
        public boolean hasAmendments() {
            return originalMandateIdChanged || originalCreditorSchemeIdChanged
                || originalCreditorAgentChanged || originalDebtorChanged
                || originalDebtorAccountChanged || originalDebtorAgentChanged
                || originalFinalCollectionDateChanged || originalAmountChanged;
        }
    }

    // =========================================================================
    //  MandateData – a single mandate within the pain.009 message
    // =========================================================================
    public static class MandateData {

        // ── Identification ────────────────────────────────────────────────
        /** Unique mandate ID assigned by the creditor (max 35 chars) */
        public String mandateId;

        /** Reference assigned by the creditor scheme (e.g. SEPA creditor ID) */
        public String creditorSchemeId;

        /** Unique request ID for this pain.009 message (max 35 chars) */
        public String mandateRequestId;

        // ── Type ─────────────────────────────────────────────────────────
        public MandateType type = new MandateType();

        // ── Occurrence ───────────────────────────────────────────────────
        /** Frequency of the direct debit collection */
        public Frequency frequency = new Frequency();

        /**
         * First collection date – the date on which the first direct debit
         * collection under this mandate will take place.
         */
        public LocalDate firstCollectionDate;

        /**
         * Final collection date – the last date on which a direct debit
         * may be collected. Null means open-ended.
         */
        public LocalDate finalCollectionDate;

        // ── Amount ───────────────────────────────────────────────────────
        /**
         * Maximum collection amount per instruction.
         * Null means no limit is specified in the mandate.
         */
        public BigDecimal maximumAmount;

        /** Currency of the maximum amount (ISO 4217) */
        public String currency;

        // ── Creditor ─────────────────────────────────────────────────────
        /** Creditor (beneficiary of the direct debit) */
        public PartyId creditor;

        /** Creditor's bank account */
        public AccountId creditorAccount;

        /** Creditor's bank */
        public BankAgent creditorAgent;

        // ── Debtor ───────────────────────────────────────────────────────
        /** Debtor (account owner from whom funds are collected) */
        public PartyId debtor;

        /** Debtor's bank account to be debited */
        public AccountId debtorAccount;

        /** Debtor's bank */
        public BankAgent debtorAgent;

        // ── Reference Info ────────────────────────────────────────────────
        /** Remittance / purpose of the mandate */
        public String remittanceInformation;

        /** Purpose code e.g. GDDS (goods), SUPP (supplier), TRAD (trade) */
        public String purposeCode;

        // ── Amendment ────────────────────────────────────────────────────
        /** Amendment flags – only populated if this is modifying an existing mandate */
        public AmendmentIndicator amendment;

        /** ID of the original mandate being amended (if applicable) */
        public String originalMandateId;

        // ── Electronic Signature ─────────────────────────────────────────
        /** Date on which the debtor signed / accepted the mandate */
        public LocalDate signatureDate;

        /** Place where the mandate was signed */
        public String signaturePlace;
    }

    // =========================================================================
    //  GroupHeader – wraps one or more mandates in the message
    // =========================================================================
    public static class GroupHeader {
        /** Unique message ID assigned by the initiating party (max 35 chars) */
        public String messageId;

        /** Date and time the message was created */
        public LocalDateTime creationDateTime;

        /** Number of individual mandates in this message */
        public int numberOfTransactions;

        /** The party that initiates the mandate request (usually the creditor) */
        public PartyId initiatingParty;

        /** Optional: forwarding agent BIC */
        public BankAgent forwardingAgent;
    }

    // =========================================================================
    //  Pain009Message – the complete message
    // =========================================================================
    public static class Pain009Message {
        public GroupHeader          groupHeader;
        public List<MandateData>    mandates = new ArrayList<>();

        public Pain009Message() {}

        public Pain009Message(GroupHeader groupHeader) {
            this.groupHeader = groupHeader;
        }

        public void addMandate(MandateData mandate) {
            mandates.add(mandate);
            if (groupHeader != null) {
                groupHeader.numberOfTransactions = mandates.size();
            }
        }
    }
}
