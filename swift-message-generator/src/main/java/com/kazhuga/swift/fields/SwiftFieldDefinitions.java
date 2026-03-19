package com.kazhuga.swift.fields;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Catalogue of standard SWIFT field tags, their human-readable names,
 * format codes, and which message types use them.
 *
 * Format codes follow SWIFT notation:
 *   n   = digits only
 *   a   = uppercase letters only
 *   c   = alphanumeric uppercase
 *   x   = any SWIFT character set
 *   d   = decimal (digits + one comma)
 *   /   = slash separator
 *   !   = fixed length
 *
 * Example: "6!n3!a15d" = 6 digits, 3 uppercase letters, 15 decimal digits
 */
public final class SwiftFieldDefinitions {

    private SwiftFieldDefinitions() {}

    public static final class FieldDef {
        public final String tag;
        public final String name;
        public final String formatCode;
        public final String usedIn;    // comma-separated MT types

        public FieldDef(String tag, String name, String formatCode, String usedIn) {
            this.tag        = tag;
            this.name       = name;
            this.formatCode = formatCode;
            this.usedIn     = usedIn;
        }

        @Override
        public String toString() {
            return tag + " – " + name + " [" + formatCode + "] (used in MT" + usedIn + ")";
        }
    }

    private static final Map<String, FieldDef> REGISTRY = new HashMap<>();

    static {
        register("20",  "Transaction Reference Number",                      "16x",                           "103,202,202COV,940,950");
        register("21",  "Related Reference",                                 "16x",                           "202,202COV");
        register("13C", "Time Indication",                                   "/8c/4!n1!x4!n",                 "103");
        register("23B", "Bank Operation Code",                               "4!a",                           "103");
        register("23E", "Instruction Code",                                  "4!a[/30x]",                     "103");
        register("26T", "Transaction Type Code",                             "3!a",                           "103");
        register("32A", "Value Date / Currency / Interbank Settled Amount",  "6!n3!a15d",                     "103,202,202COV");
        register("33B", "Currency / Instructed Amount",                      "3!a15d",                        "103");
        register("36",  "Exchange Rate",                                     "12d",                            "103");
        register("50A", "Ordering Customer (Account/BIC)",                   "[/34x]\\n4!a2!a2!c[3!c]",       "103");
        register("50F", "Ordering Customer (Party Identifier)",              "35x\\n4*35x",                   "103");
        register("50K", "Ordering Customer (Name & Address)",                "[/34x]\\n4*35x",                "103");
        register("51A", "Sending Institution",                               "[/1!a][/34x]\\n4!a2!a2!c[3!c]","103");
        register("52A", "Ordering Institution (BIC)",                        "[/1!a][/34x]\\n4!a2!a2!c[3!c]","103,202");
        register("52D", "Ordering Institution (Name/Address)",               "[/1!a][/34x]\\n4*35x",          "103,202");
        register("53A", "Sender's Correspondent (BIC)",                      "[/1!a][/34x]\\n4!a2!a2!c[3!c]","103,202");
        register("53B", "Sender's Correspondent (Location)",                 "[/1!a][/34x]\\n[35x]",          "103,202");
        register("54A", "Receiver's Correspondent (BIC)",                    "[/1!a][/34x]\\n4!a2!a2!c[3!c]","103,202");
        register("55A", "Third Reimbursement Institution (BIC)",             "[/1!a][/34x]\\n4!a2!a2!c[3!c]","103");
        register("56A", "Intermediary Institution (BIC)",                    "[/1!a][/34x]\\n4!a2!a2!c[3!c]","103,202");
        register("57A", "Account With Institution (BIC)",                    "[/1!a][/34x]\\n4!a2!a2!c[3!c]","103,202");
        register("57D", "Account With Institution (Name/Address)",           "[/1!a][/34x]\\n4*35x",          "103,202");
        register("58A", "Beneficiary Institution (BIC)",                     "[/1!a][/34x]\\n4!a2!a2!c[3!c]","202,202COV");
        register("58D", "Beneficiary Institution (Name/Address)",            "[/1!a][/34x]\\n4*35x",          "202,202COV");
        register("59",  "Beneficiary Customer (No option)",                  "[/34x]\\n4*35x",                "103");
        register("59A", "Beneficiary Customer (BIC)",                        "[/34x]\\n4!a2!a2!c[3!c]",       "103");
        register("59F", "Beneficiary Customer (Structured)",                 "[/34x]\\n4*35x",                "103");
        register("70",  "Remittance Information",                            "4*35x",                         "103");
        register("71A", "Details of Charges",                                "3!a",                           "103");
        register("71F", "Sender's Charges",                                  "3!a15d",                        "103");
        register("71G", "Receiver's Charges",                                "3!a15d",                        "103");
        register("72",  "Sender to Receiver Information",                    "6*35x",                         "103,202,202COV");
        register("77B", "Regulatory Reporting",                              "3*35x",                         "103");
        register("77T", "Envelope Contents",                                 "9000z",                         "202COV");

        // MT 940 / 950
        register("25",  "Account Identification",                            "35x",                           "940,950");
        register("25P", "Account Identification with BIC",                   "35x\\n4!a2!a2!c[3!c]",          "940");
        register("28C", "Statement Number / Sequence Number",                "5n[/5n]",                       "940,950");
        register("60F", "Opening Balance (Final)",                           "1!a6!n3!a15d",                  "940,950");
        register("60M", "Opening Balance (Intermediate)",                    "1!a6!n3!a15d",                  "940,950");
        register("61",  "Statement Line",                                    "6!n[4!n]2a[1!a]15d1!a3!c16x[//16x][\\n34x]", "940,950");
        register("62F", "Closing Balance (Final)",                           "1!a6!n3!a15d",                  "940,950");
        register("62M", "Closing Balance (Intermediate)",                    "1!a6!n3!a15d",                  "940,950");
        register("64",  "Closing Available Balance",                         "1!a6!n3!a15d",                  "940");
        register("65",  "Forward Available Balance",                         "1!a6!n3!a15d",                  "940");
        register("86",  "Information to Account Owner",                      "6*65x",                         "940");
    }

    private static void register(String tag, String name, String format, String usedIn) {
        REGISTRY.put(tag, new FieldDef(tag, name, format, usedIn));
    }

    /** Lookup a field definition by tag; returns null if not found */
    public static FieldDef get(String tag) {
        return REGISTRY.get(tag);
    }

    /** Returns the human-readable name for a tag, or the tag itself if unknown */
    public static String getName(String tag) {
        FieldDef def = REGISTRY.get(tag);
        return def != null ? def.name : tag;
    }

    /** Returns all registered definitions */
    public static Map<String, FieldDef> getAll() {
        return Collections.unmodifiableMap(REGISTRY);
    }
}
