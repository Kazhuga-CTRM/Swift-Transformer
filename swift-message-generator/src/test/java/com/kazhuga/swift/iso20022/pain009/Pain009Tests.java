package com.kazhuga.swift.iso20022.pain009;

import com.kazhuga.swift.iso20022.model.Pain009Data.*;
import com.kazhuga.swift.iso20022.validation.Pain009Validator;
import com.kazhuga.swift.validation.ValidationResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Self-contained test suite for pain.009 — no third-party framework needed.
 *
 * Run with:
 *   javac -d out $(find src -name "*.java")
 *   java -cp out com.kazhuga.swift.iso20022.pain009.Pain009Tests
 */
public class Pain009Tests {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("Running pain.009 Tests  [com.kazhuga.swift.iso20022]\n");

        testValidation();
        testSepaCoreGeneration();
        testB2BGeneration();
        testOneOffGeneration();
        testAmendmentGeneration();
        testXmlStructure();
        testValidationErrors();

        System.out.println("\n----------------------------------------------");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        if (failed > 0) System.exit(1);
    }

    // =========================================================================

    private static void testValidation() {
        section("pain.009 Validator");
        Pain009Validator v = new Pain009Validator();

        // Valid SEPA Core sample
        ValidationResult vr = v.validate(Pain009SampleFactory.sepaCoreMonthlySample());
        assertTrue("SEPA Core sample passes validation", vr.isValid());

        // Valid B2B sample
        assertTrue("B2B sample passes validation",
                v.validate(Pain009SampleFactory.commoditiesB2BSample()).isValid());

        // Null message
        assertFalse("Null message fails", v.validate(null).isValid());

        // One-off
        assertTrue("One-off sample passes validation",
                v.validate(Pain009SampleFactory.oneOffSpotTradeSample()).isValid());

        // Amendment
        assertTrue("Amendment sample passes validation",
                v.validate(Pain009SampleFactory.amendmentSample()).isValid());
    }

    private static void testValidationErrors() {
        section("Validation Error Cases");
        Pain009Validator v = new Pain009Validator();

        // Missing mandate ID
        Pain009Message msg = Pain009SampleFactory.sepaCoreMonthlySample();
        msg.mandates.get(0).mandateId = null;
        assertFalse("Missing mandateId fails", v.validate(msg).isValid());

        // Invalid sequence type
        Pain009Message msg2 = Pain009SampleFactory.sepaCoreMonthlySample();
        msg2.mandates.get(0).type.sequenceType = "XXXX";
        assertFalse("Invalid sequence type fails", v.validate(msg2).isValid());

        // Final collection date before first
        Pain009Message msg3 = Pain009SampleFactory.sepaCoreMonthlySample();
        msg3.mandates.get(0).finalCollectionDate = LocalDate.of(2023, 1, 1);
        assertFalse("Final date before first date fails", v.validate(msg3).isValid());

        // No debtor account
        Pain009Message msg4 = Pain009SampleFactory.sepaCoreMonthlySample();
        msg4.mandates.get(0).debtorAccount = null;
        assertFalse("Missing debtor account fails", v.validate(msg4).isValid());

        // Negative maximum amount
        Pain009Message msg5 = Pain009SampleFactory.sepaCoreMonthlySample();
        msg5.mandates.get(0).maximumAmount = new BigDecimal("-100");
        assertFalse("Negative amount fails", v.validate(msg5).isValid());
    }

    // =========================================================================

    private static void testSepaCoreGeneration() {
        section("SEPA Core Monthly pain.009 Generation");

        ISO20022Generator gen = new ISO20022Generator();
        String xml = gen.generatePain009(Pain009SampleFactory.sepaCoreMonthlySample());

        assertNotNull("XML not null", xml);
        assertTrue("Has XML declaration",           xml.contains("<?xml version=\"1.0\""));
        assertTrue("Has correct namespace",         xml.contains("pain.009.001.08"));
        assertTrue("Has Document element",          xml.contains("<Document"));
        assertTrue("Has MndtInitnReq element",      xml.contains("<MndtInitnReq>"));
        assertTrue("Has GrpHdr",                    xml.contains("<GrpHdr>"));
        assertTrue("Has message ID",                xml.contains("<MsgId>KAZHUGA-PAIN009-001</MsgId>"));
        assertTrue("Has NbOfTxs = 1",               xml.contains("<NbOfTxs>1</NbOfTxs>"));
        assertTrue("Has mandate element",           xml.contains("<Mndt>"));
        assertTrue("Has mandate ID",                xml.contains("<MndtId>MNDT-ENRG-2024-00145</MndtId>"));
        assertTrue("Has sequence type RCUR",        xml.contains("<Prtry>RCUR</Prtry>"));
        assertTrue("Has local instrument CORE",     xml.contains("<Prtry>CORE</Prtry>"));
        assertTrue("Has frequency MNTH",            xml.contains("<Tp>MNTH</Tp>"));
        assertTrue("Has first collection date",     xml.contains("<FrstColltnDt>2024-04-01</FrstColltnDt>"));
        assertTrue("Has max amount EUR",            xml.contains("Ccy=\"EUR\""));
        assertTrue("Has creditor name",             xml.contains("KAZHUGA ENERGY TRADING LTD"));
        assertTrue("Has creditor IBAN",             xml.contains("DE89370400440532013000"));
        assertTrue("Has creditor BIC",              xml.contains("<BICFI>DEUTDEDB</BICFI>"));
        assertTrue("Has debtor name",               xml.contains("RHEIN INDUSTRIEWERKE GMBH"));
        assertTrue("Has debtor IBAN",               xml.contains("DE44500105175407324931"));
        assertTrue("Has signature date",            xml.contains("<DtOfSgntr>2024-03-10</DtOfSgntr>"));
        assertTrue("Has creditor scheme ID",        xml.contains("DE98ZZZ09999999999"));
        assertTrue("Has remittance info",           xml.contains("MONTHLY GAS SUPPLY CONTRACT"));
        assertTrue("Closes Document",               xml.contains("</Document>"));
    }

    private static void testB2BGeneration() {
        section("B2B Commodities pain.009 Generation");

        String xml = new ISO20022Generator()
                .generatePain009(Pain009SampleFactory.commoditiesB2BSample());

        assertTrue("Has B2B instrument",           xml.contains("<Prtry>B2B</Prtry>"));
        assertTrue("Has WEEK frequency",           xml.contains("<Tp>WEEK</Tp>"));
        assertTrue("Has LME CLEAR as creditor",   xml.contains("LME CLEAR LIMITED"));
        assertTrue("Has USD currency",             xml.contains("Ccy=\"USD\""));
        assertTrue("Has Ccy element USD",          xml.contains("<Ccy>USD</Ccy>"));
        assertTrue("Has margin purpose",           xml.contains("<Cd>MARG</Cd>"));
        assertTrue("Has variation margin ref",     xml.contains("LME VARIATION MARGIN"));
    }

    private static void testOneOffGeneration() {
        section("One-Off Spot Trade pain.009 Generation");

        String xml = new ISO20022Generator()
                .generatePain009(Pain009SampleFactory.oneOffSpotTradeSample());

        assertTrue("Has OOFF sequence type",       xml.contains("<Prtry>OOFF</Prtry>"));
        assertTrue("Has ADHO frequency",           xml.contains("<Tp>ADHO</Tp>"));
        assertTrue("Has spot trade amount",        xml.contains("2375000.00"));
        assertTrue("Has TRAD purpose",             xml.contains("<Cd>TRAD</Cd>"));
        assertTrue("Has crude oil reference",      xml.contains("SPOT CRUDE OIL TRADE"));
        assertTrue("Has UBS BIC",                  xml.contains("UBSWCHZH80A"));
        assertTrue("Has Swiss IBAN",               xml.contains("CH5604835012345678009"));
    }

    private static void testAmendmentGeneration() {
        section("Amendment pain.009 Generation");

        String xml = new ISO20022Generator()
                .generatePain009(Pain009SampleFactory.amendmentSample());

        assertTrue("Has amendment indicator",      xml.contains("<AmdmntInd>true</AmdmntInd>"));
        assertTrue("Has original mandate ID",      xml.contains("<OrgnlMndtId>MNDT-ENRG-2024-00145</OrgnlMndtId>"));
        assertTrue("Has debtor account changed",   xml.contains("<OrgnlDbtrAcctChng>true</OrgnlDbtrAcctChng>"));
        assertTrue("Has debtor agent changed",     xml.contains("<OrgnlDbtrAgtChng>true</OrgnlDbtrAgtChng>"));
        assertTrue("Has new debtor IBAN",          xml.contains("DE72200400600028490900"));
        assertTrue("Has new debtor BIC (Postbank)",xml.contains("PBNKDEFFXXX"));
    }

    private static void testXmlStructure() {
        section("XML Structural Integrity");

        String xml = new ISO20022Generator()
                .generatePain009(Pain009SampleFactory.sepaCoreMonthlySample());

        // Count open vs close tags for key elements
        long openDoc   = countOccurrences(xml, "<Document");
        long closeDoc  = countOccurrences(xml, "</Document>");
        long openMndt  = countOccurrences(xml, "<Mndt>");
        long closeMndt = countOccurrences(xml, "</Mndt>");

        assertEqual("Document tag balanced", openDoc,  closeDoc);
        assertEqual("Mandate tag balanced",  openMndt, closeMndt);

        // No unescaped special characters in content values
        assertFalse("No bare & in content", containsBareAmpersand(xml));

        // Well-formed indentation (spot check: Document is at col 0)
        assertTrue("Document at line start", xml.contains("\n<Document") || xml.startsWith("<Document"));
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    private static long countOccurrences(String text, String sub) {
        long count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) { count++; idx++; }
        return count;
    }

    private static boolean containsBareAmpersand(String xml) {
        // After removing all &amp; &lt; &gt; &quot; &apos; there should be no & left
        String stripped = xml
                .replace("&amp;", "").replace("&lt;", "").replace("&gt;", "")
                .replace("&quot;", "").replace("&apos;", "");
        return stripped.contains("&");
    }

    // ── Assertion helpers ──────────────────────────────────────────────────────

    private static void section(String name) { System.out.println("\n  -- " + name); }

    private static void assertEqual(String label, Object expected, Object actual) {
        if (expected == null && actual == null) { pass(label); return; }
        if (expected != null && expected.equals(actual)) { pass(label); return; }
        fail(label + " -- expected [" + expected + "] but got [" + actual + "]");
    }

    private static void assertTrue(String label, boolean c)  { if (c) pass(label); else fail(label); }
    private static void assertFalse(String label, boolean c) { if (!c) pass(label); else fail(label + " -- expected false"); }
    private static void assertNotNull(String label, Object o){ if (o != null) pass(label); else fail(label + " -- was null"); }

    private static void pass(String label) { System.out.println("    PASS: " + label); passed++; }
    private static void fail(String label) { System.out.println("    FAIL: " + label); failed++; }
}
