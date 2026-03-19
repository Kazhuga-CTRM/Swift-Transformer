package com.kazhuga.swift.iso20022.pain009;

import com.kazhuga.swift.iso20022.model.Pain009Data.*;
import com.kazhuga.swift.iso20022.validation.Pain009Validator;
import com.kazhuga.swift.validation.ValidationResult;

import java.time.format.DateTimeFormatter;

/**
 * Builds a pain.009.001.08 MandateInitiationRequest XML document.
 *
 * The generated XML conforms to the ISO 20022 pain.009.001.08 schema
 * and is suitable for SEPA Direct Debit mandate initiation and for
 * bilateral mandate exchanges in commodity and treasury settlement.
 *
 * Usage:
 * <pre>
 *   Pain009Builder builder = new Pain009Builder(message);
 *   String xml = builder.build();  // returns validated, indented XML
 * </pre>
 *
 * OpenLink Findur / Endur integration:
 *   Populate Pain009Data.Pain009Message from your deal / counterparty objects,
 *   then pass the XML string to your existing ISO 20022 transport layer or
 *   write it directly to the file drop-zone used by your SWIFT Alliance gateway.
 */
public class Pain009Builder {

    private static final String NAMESPACE =
            "urn:iso:std:iso:20022:tech:xsd:pain.009.001.08";
    private static final String SCHEMA_LOCATION =
            "urn:iso:std:iso:20022:tech:xsd:pain.009.001.08 pain.009.001.08.xsd";

    private static final DateTimeFormatter DT_FMT  =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Pain009Message message;

    public Pain009Builder(Pain009Message message) {
        if (message == null) throw new IllegalArgumentException("Pain009Message must not be null");
        this.message = message;
    }

    /**
     * Validates and builds the pain.009 XML string.
     *
     * @return indented, UTF-8 XML document as a String
     * @throws IllegalStateException if validation errors are found
     */
    public String build() {
        Pain009Validator validator = new Pain009Validator();
        ValidationResult vr       = validator.validate(message);
        if (!vr.isValid()) {
            throw new IllegalStateException("pain.009 validation failed:\n" + vr);
        }
        return buildXml();
    }

    /**
     * Builds without validation — use only when you need partial documents for testing.
     */
    public String buildUnchecked() {
        return buildXml();
    }

    // =========================================================================

    private String buildXml() {
        XmlWriter x = new XmlWriter();

        x.line("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        x.open("Document",
               "xmlns", NAMESPACE,
               "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance",
               "xsi:schemaLocation", SCHEMA_LOCATION);
        x.open("MndtInitnReq");

        buildGroupHeader(x);
        for (MandateData mandate : message.mandates) {
            buildMandate(x, mandate);
        }

        x.close("MndtInitnReq");
        x.close("Document");

        return x.toString();
    }

    // ── Group Header ─────────────────────────────────────────────────────────

    private void buildGroupHeader(XmlWriter x) {
        GroupHeader h = message.groupHeader;
        x.open("GrpHdr");
        x.elem("MsgId",    h.messageId);
        x.elem("CreDtTm",  h.creationDateTime.format(DT_FMT));
        x.elem("NbOfTxs",  String.valueOf(h.numberOfTransactions));
        buildParty(x, "InitgPty", h.initiatingParty);
        if (h.forwardingAgent != null) {
            buildAgent(x, "FwdgAgt", h.forwardingAgent);
        }
        x.close("GrpHdr");
    }

    // ── Mandate ──────────────────────────────────────────────────────────────

    private void buildMandate(XmlWriter x, MandateData m) {
        x.open("Mndt");

        // Identification
        x.elem("MndtId",     m.mandateId);
        x.elem("MndtReqId",  m.mandateRequestId);

        // Type
        if (m.type != null) {
            x.open("Tp");
            x.open("SeqTp");
            x.elem("Prtry", m.type.sequenceType);
            x.close("SeqTp");
            if (m.type.localInstrumentCode != null) {
                x.open("LclInstrm");
                x.elem("Prtry", m.type.localInstrumentCode);
                x.close("LclInstrm");
            }
            if (m.type.categoryPurposeCode != null) {
                x.open("CtgyPurp");
                x.elem("Cd", m.type.categoryPurposeCode);
                x.close("CtgyPurp");
            }
            x.close("Tp");
        }

        // Occurrence / frequency
        x.open("Ocrncs");
        if (m.frequency != null) {
            x.open("Frqcy");
            x.elem("Tp", m.frequency.code);
            if (m.frequency.pointInTime != null) {
                x.elem("PtInTm", String.valueOf(m.frequency.pointInTime));
            }
            x.close("Frqcy");
        }
        if (m.firstCollectionDate != null) {
            x.elem("FrstColltnDt", m.firstCollectionDate.format(DATE_FMT));
        }
        if (m.finalCollectionDate != null) {
            x.elem("FnlColltnDt", m.finalCollectionDate.format(DATE_FMT));
        }
        x.close("Ocrncs");

        // Maximum amount
        if (m.maximumAmount != null && m.currency != null) {
            x.elem("MaxAmt", m.maximumAmount.toPlainString(), "Ccy", m.currency);
        }

        // Amendment information
        if (m.amendment != null && m.amendment.hasAmendments()) {
            buildAmendment(x, m);
        }

        // Creditor scheme identifier
        if (m.creditorSchemeId != null) {
            x.open("CdtrSchmeId");
            x.open("Id");
            x.open("PrvtId");
            x.open("Othr");
            x.elem("Id", m.creditorSchemeId);
            x.open("SchmeNm");
            x.elem("Prtry", "SEPA");
            x.close("SchmeNm");
            x.close("Othr");
            x.close("PrvtId");
            x.close("Id");
            x.close("CdtrSchmeId");
        }

        // Creditor
        buildAgent(x, "CdtrAgt", m.creditorAgent);
        buildParty(x, "Cdtr",    m.creditor);
        buildAccount(x, "CdtrAcct", m.creditorAccount);

        // Debtor
        buildAgent(x, "DbtrAgt", m.debtorAgent);
        buildParty(x, "Dbtr",    m.debtor);
        buildAccount(x, "DbtrAcct", m.debtorAccount);

        // Mandate related information (signature)
        x.open("MndtRltdInf");
        if (m.signatureDate != null) {
            x.elem("DtOfSgntr", m.signatureDate.format(DATE_FMT));
        }
        if (m.signaturePlace != null) {
            x.elem("SgntrPlc", m.signaturePlace);
        }
        x.close("MndtRltdInf");

        // Purpose
        if (m.purposeCode != null) {
            x.open("Purp");
            x.elem("Cd", m.purposeCode);
            x.close("Purp");
        }

        // Remittance information
        if (m.remittanceInformation != null) {
            x.open("RmtInf");
            x.elem("Ustrd", m.remittanceInformation);
            x.close("RmtInf");
        }

        x.close("Mndt");
    }

    // ── Amendment ────────────────────────────────────────────────────────────

    private void buildAmendment(XmlWriter x, MandateData m) {
        AmendmentIndicator a = m.amendment;
        x.open("Amdmnt");
        if (m.originalMandateId != null) {
            x.elem("OrgnlMndtId", m.originalMandateId);
        }
        x.elem("AmdmntInd", "true");
        if (a.originalMandateIdChanged)           x.elem("OrgnlMndtIdChng",       "true");
        if (a.originalCreditorSchemeIdChanged)    x.elem("OrgnlCdtrSchmeIdChng",  "true");
        if (a.originalCreditorAgentChanged)       x.elem("OrgnlCdtrAgtChng",      "true");
        if (a.originalDebtorChanged)              x.elem("OrgnlDbtrChng",          "true");
        if (a.originalDebtorAccountChanged)       x.elem("OrgnlDbtrAcctChng",      "true");
        if (a.originalDebtorAgentChanged)         x.elem("OrgnlDbtrAgtChng",       "true");
        if (a.originalFinalCollectionDateChanged) x.elem("OrgnlFnlColltnDtChng",  "true");
        if (a.originalAmountChanged)              x.elem("OrgnlAmtChng",           "true");
        x.close("Amdmnt");
    }

    // ── Party ─────────────────────────────────────────────────────────────────

    private void buildParty(XmlWriter x, String tag, PartyId party) {
        if (party == null) return;
        x.open(tag);
        if (party.name != null) x.elem("Nm", party.name);
        if (party.address != null) {
            x.open("PstlAdr");
            PostalAddress a = party.address;
            if (a.streetName    != null) x.elem("StrtNm",    a.streetName);
            if (a.buildingNumber!= null) x.elem("BldgNb",    a.buildingNumber);
            if (a.postCode      != null) x.elem("PstCd",     a.postCode);
            if (a.townName      != null) x.elem("TwnNm",     a.townName);
            if (a.country       != null) x.elem("Ctry",      a.country);
            x.close("PstlAdr");
        }
        if (party.lei != null || party.taxId != null) {
            x.open("Id");
            x.open("OrgId");
            if (party.lei != null) x.elem("LEI", party.lei);
            if (party.taxId != null) {
                x.open("Othr");
                x.elem("Id", party.taxId);
                x.open("SchmeNm");
                x.elem("Cd", "TXID");
                x.close("SchmeNm");
                x.close("Othr");
            }
            x.close("OrgId");
            x.close("Id");
        }
        x.close(tag);
    }

    // ── Account ───────────────────────────────────────────────────────────────

    private void buildAccount(XmlWriter x, String tag, AccountId acct) {
        if (acct == null) return;
        x.open(tag);
        x.open("Id");
        if (acct.iban != null) {
            x.elem("IBAN", acct.iban);
        } else if (acct.bban != null) {
            x.open("Othr");
            x.elem("Id", acct.bban);
            x.close("Othr");
        }
        x.close("Id");
        if (acct.currency    != null) x.elem("Ccy",  acct.currency);
        if (acct.accountName != null) x.elem("Nm",   acct.accountName);
        x.close(tag);
    }

    // ── Bank Agent ────────────────────────────────────────────────────────────

    private void buildAgent(XmlWriter x, String tag, BankAgent agent) {
        if (agent == null) return;
        x.open(tag);
        x.open("FinInstnId");
        if (agent.bic != null) x.elem("BICFI", agent.bic);
        if (agent.clearingSystemCode != null || agent.memberIdentification != null) {
            x.open("ClrSysMmbId");
            if (agent.clearingSystemCode != null) {
                x.open("ClrSysId");
                x.elem("Cd", agent.clearingSystemCode);
                x.close("ClrSysId");
            }
            if (agent.memberIdentification != null) {
                x.elem("MmbId", agent.memberIdentification);
            }
            x.close("ClrSysMmbId");
        }
        if (agent.name    != null) x.elem("Nm", agent.name);
        if (agent.address != null) {
            x.open("PstlAdr");
            PostalAddress a = agent.address;
            if (a.streetName != null) x.elem("StrtNm", a.streetName);
            if (a.postCode   != null) x.elem("PstCd",  a.postCode);
            if (a.townName   != null) x.elem("TwnNm",  a.townName);
            if (a.country    != null) x.elem("Ctry",   a.country);
            x.close("PstlAdr");
        }
        x.close("FinInstnId");
        x.close(tag);
    }

    // =========================================================================
    //  Internal indent-aware XML writer
    // =========================================================================

    private static final class XmlWriter {
        private final StringBuilder sb     = new StringBuilder();
        private       int           indent = 0;

        /** Opens a tag with optional attribute pairs */
        void open(String tag, String... attrPairs) {
            sb.append("  ".repeat(indent)).append("<").append(tag);
            for (int i = 0; i < attrPairs.length; i += 2) {
                sb.append(" ").append(attrPairs[i])
                  .append("=\"").append(escape(attrPairs[i + 1])).append("\"");
            }
            sb.append(">\n");
            indent++;
        }

        /** Closes a tag */
        void close(String tag) {
            indent--;
            sb.append("  ".repeat(indent)).append("</").append(tag).append(">\n");
        }

        /** Inline element: <tag>text</tag> with optional attribute pairs */
        void elem(String tag, String text, String... attrPairs) {
            sb.append("  ".repeat(indent)).append("<").append(tag);
            for (int i = 0; i < attrPairs.length; i += 2) {
                sb.append(" ").append(attrPairs[i])
                  .append("=\"").append(escape(attrPairs[i + 1])).append("\"");
            }
            sb.append(">").append(escape(text)).append("</").append(tag).append(">\n");
        }

        void line(String raw) {
            sb.append(raw).append("\n");
        }

        private static String escape(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
        }

        @Override
        public String toString() { return sb.toString(); }
    }
}
