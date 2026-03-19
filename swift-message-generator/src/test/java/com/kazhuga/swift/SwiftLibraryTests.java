package com.kazhuga.swift;

import com.kazhuga.swift.core.SwiftMessage;
import com.kazhuga.swift.demo.SampleDataFactory;
import com.kazhuga.swift.generator.SwiftGenerator;
import com.kazhuga.swift.model.SwiftData.TransferData;
import com.kazhuga.swift.parser.SwiftParser;
import com.kazhuga.swift.util.SwiftFormatUtil;
import com.kazhuga.swift.validation.MT103Validator;
import com.kazhuga.swift.validation.ValidationResult;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Self-contained test suite — no third-party test framework required.
 *
 * Run with:
 *   javac -d out $(find src -name "*.java")
 *   java -cp out com.kazhuga.swift.SwiftLibraryTests
 */
public class SwiftLibraryTests {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("Running SWIFT Library Tests  [com.kazhuga.swift]\n");

        testFormatUtil();
        testMT103Validation();
        testMT103Generation();
        testMT202Generation();
        testMT202COVGeneration();
        testMT940Generation();
        testMT950Generation();
        testParser();
        testRoundTrip();

        System.out.println("\n----------------------------------------------");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        if (failed > 0) System.exit(1);
    }

    private static void testFormatUtil() {
        section("SwiftFormatUtil");
        assertEqual("formatAmount 1234.56", "1234,56", SwiftFormatUtil.formatAmount(new BigDecimal("1234.56")));
        assertEqual("formatAmount 1000.00", "1000,00", SwiftFormatUtil.formatAmount(new BigDecimal("1000.00")));
        assertEqual("formatAmount 0.50",    "0,50",    SwiftFormatUtil.formatAmount(new BigDecimal("0.50")));
        assertEqual("parseAmount",          new BigDecimal("9876.54"), SwiftFormatUtil.parseAmount("9876,54"));
        assertEqual("formatDate",           "230915",  SwiftFormatUtil.formatDate(LocalDate.of(2023, 9, 15)));
        assertEqual("bic11 pads 8-char",    "CHASUS33XXX", SwiftFormatUtil.bic11("CHASUS33"));
        assertEqual("bic11 keeps 11-char",  "CHASUS33XXX", SwiftFormatUtil.bic11("CHASUS33XXX"));
        assertEqual("trunc",                "HELLO",   SwiftFormatUtil.trunc("HELLO WORLD", 5));
        assertEqual("sanitizeSwiftX",       "HELLO WORLD", SwiftFormatUtil.sanitizeSwiftX("HELLO WORLD"));
    }

    private static void testMT103Validation() {
        section("MT103Validator");
        MT103Validator v = new MT103Validator();
        assertTrue("Valid sample passes",   v.validate(SampleDataFactory.sampleMT103()).isValid());
        assertFalse("Null data fails",       v.validate(null).isValid());

        TransferData bad = SampleDataFactory.sampleMT103(); bad.transactionReference = null;
        assertFalse("Missing ref fails",    v.validate(bad).isValid());

        TransferData badCcy = SampleDataFactory.sampleMT103(); badCcy.currency = "US";
        assertFalse("2-char currency fails", v.validate(badCcy).isValid());

        TransferData zeroAmt = SampleDataFactory.sampleMT103(); zeroAmt.amount = BigDecimal.ZERO;
        assertFalse("Zero amount fails",    v.validate(zeroAmt).isValid());

        TransferData badChg = SampleDataFactory.sampleMT103(); badChg.detailsOfCharges = "XYZ";
        assertFalse("Invalid charges fails", v.validate(badChg).isValid());
    }

    private static void testMT103Generation() {
        section("MT103 Generation");
        SwiftGenerator gen = new SwiftGenerator();
        SwiftMessage msg = gen.generateMT103(SampleDataFactory.sampleMT103());
        assertNotNull("MT103 built", msg);
        assertEqual("Message type", "103", msg.getMessageType());
        String f32a = msg.getFieldValue("32A");
        assertNotNull("Field 32A present", f32a);
        assertTrue("32A has USD",    f32a != null && f32a.contains("USD"));
        assertTrue("32A has amount", f32a != null && f32a.contains("125000,00"));
        assertEqual("71A is SHA",  "SHA",  msg.getFieldValue("71A"));
        assertEqual("23B is CRED","CRED",  msg.getFieldValue("23B"));
        String wire = msg.toSwiftString();
        assertTrue("Wire starts {1:", wire.startsWith("{1:"));
        assertTrue("Wire has block 4", wire.contains("{4:"));
        assertTrue("Wire has :20:",  wire.contains(":20:"));
        assertTrue("Wire has :23B:", wire.contains(":23B:"));
        assertTrue("Wire has :32A:", wire.contains(":32A:"));
        assertTrue("Wire has :50",   wire.contains(":50"));
        assertTrue("Wire has :59",   wire.contains(":59"));
        assertTrue("Wire has :71A:", wire.contains(":71A:"));
    }

    private static void testMT202Generation() {
        section("MT202 Generation");
        SwiftGenerator gen = new SwiftGenerator();
        SwiftMessage msg = gen.generateMT202(SampleDataFactory.sampleMT202());
        assertNotNull("MT202 built", msg);
        assertEqual("Message type", "202", msg.getMessageType());
        String wire = msg.toSwiftString();
        assertTrue("MT202 has :21:",  wire.contains(":21:"));
        assertTrue("MT202 has :58A:", wire.contains(":58A:"));
        assertEqual("Field 21 value", "USPAY20230915001", msg.getFieldValue("21"));
    }

    private static void testMT202COVGeneration() {
        section("MT202COV Generation");
        SwiftGenerator gen = new SwiftGenerator();
        SwiftMessage msg = gen.generateMT202COV(SampleDataFactory.sampleMT202COV());
        assertNotNull("MT202COV built", msg);
        assertEqual("Message type", "202COV", msg.getMessageType());
        String wire = msg.toSwiftString();
        assertTrue("MT202COV has :50K:", wire.contains(":50K:"));
        assertTrue("MT202COV has :59:",  wire.contains(":59:"));
    }

    private static void testMT940Generation() {
        section("MT940 Generation");
        SwiftGenerator gen = new SwiftGenerator();
        SwiftMessage msg = gen.generateMT940(SampleDataFactory.sampleMT940());
        assertNotNull("MT940 built", msg);
        assertEqual("Message type", "940", msg.getMessageType());
        String wire = msg.toSwiftString();
        assertTrue("Has :25P:", wire.contains(":25P:"));
        assertTrue("Has :28C:", wire.contains(":28C:"));
        assertTrue("Has :60F:", wire.contains(":60F:"));
        assertTrue("Has :62F:", wire.contains(":62F:"));
        assertTrue("Has :61:",  wire.contains(":61:"));
        assertTrue("Has :86:",  wire.contains(":86:"));
        assertTrue("Has :64:",  wire.contains(":64:"));
        String f62f = msg.getFieldValue("62F");
        assertTrue("62F has EUR",     f62f != null && f62f.contains("EUR"));
        assertTrue("62F has 340700,00", f62f != null && f62f.contains("340700,00"));
    }

    private static void testMT950Generation() {
        section("MT950 Generation");
        SwiftGenerator gen = new SwiftGenerator();
        SwiftMessage msg = gen.generateMT950(SampleDataFactory.sampleMT950());
        assertNotNull("MT950 built", msg);
        assertEqual("Message type", "950", msg.getMessageType());
        String wire = msg.toSwiftString();
        assertTrue("Has :61:",    wire.contains(":61:"));
        assertFalse("No :86:",    wire.contains(":86:"));
    }

    private static void testParser() {
        section("SwiftParser");
        SwiftParser parser = new SwiftParser();
        SwiftGenerator gen = new SwiftGenerator();
        SwiftMessage original = gen.generateMT103(SampleDataFactory.sampleMT103());
        try {
            SwiftMessage parsed = parser.parse(original.toSwiftString());
            assertNotNull("Parsed not null", parsed);
            assertEqual("Parsed 32A", original.getFieldValue("32A"), parsed.getFieldValue("32A"));
            assertEqual("Parsed 71A", original.getFieldValue("71A"), parsed.getFieldValue("71A"));
            assertEqual("Parsed 23B", original.getFieldValue("23B"), parsed.getFieldValue("23B"));
        } catch (Exception e) { fail("Parser exception: " + e.getMessage()); }
    }

    private static void testRoundTrip() {
        section("Round-trip MT940");
        SwiftGenerator gen = new SwiftGenerator();
        SwiftParser parser = new SwiftParser();
        SwiftMessage original = gen.generateMT940(SampleDataFactory.sampleMT940());
        try {
            SwiftMessage parsed = parser.parse(original.toSwiftString());
            assertEqual("Round-trip 20",  original.getFieldValue("20"),  parsed.getFieldValue("20"));
            assertEqual("Round-trip 28C", original.getFieldValue("28C"), parsed.getFieldValue("28C"));
            assertEqual("Round-trip 62F", original.getFieldValue("62F"), parsed.getFieldValue("62F"));
        } catch (Exception e) { fail("Round-trip error: " + e.getMessage()); }
    }

    // -------------------------------------------------------------------------
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
