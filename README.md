# SWIFT Message Generator — Full Documentation

> **Package:** `com.kazhuga.swift` · **License:** MIT · **Java:** 11+ · **Dependencies:** Zero

---

## Table of Contents

1. [Overview](#1-overview)
2. [Supported Message Types](#2-supported-message-types)
3. [Project Structure](#3-project-structure)
4. [Getting Started](#4-getting-started)
5. [Core Concepts](#5-core-concepts)
6. [Data Models](#6-data-models)
7. [Generating Messages](#7-generating-messages)
   - [MT103 – Single Customer Credit Transfer](#71-mt103--single-customer-credit-transfer)
   - [MT202 – Financial Institution Transfer](#72-mt202--financial-institution-transfer)
   - [MT202COV – Cover Payment](#73-mt202cov--cover-payment)
   - [MT940 – Customer Statement](#74-mt940--customer-statement)
   - [MT950 – Bank Statement](#75-mt950--bank-statement)
8. [Parsing Wire Format](#8-parsing-wire-format)
9. [Validation](#9-validation)
10. [SWIFT Format Reference](#10-swift-format-reference)
11. [Field Catalogue](#11-field-catalogue)
12. [Running Tests](#12-running-tests)
13. [Extending the Library](#13-extending-the-library)
14. [FAQ](#14-faq)
15. [Disclaimer](#15-disclaimer)

---

## 1. Overview

The **SWIFT Message Generator** is a pure Java library for generating, validating, and parsing SWIFT FIN messages. It targets developers building payment systems, treasury platforms, bank integrations, and testing tools who need standards-compliant MT message output without pulling in heavyweight vendor SDKs.

### Key design principles

| Principle | Implementation |
|---|---|
| **Zero runtime dependencies** | No Spring, Jackson, or third-party jars required |
| **Validate before you build** | Every builder validates input data before emitting wire bytes |
| **Round-trip fidelity** | `SwiftParser` parses any generated wire string back to a typed object |
| **Correct SWIFT formatting** | YYMMDD dates, comma decimal amounts, 11-char BICs, and field sequences handled automatically |
| **Extensible architecture** | Add new MT types by subclassing `AbstractMessageBuilder` |

---

## 2. Supported Message Types

| Type | Name | Category | Use Case |
|------|------|----------|----------|
| **MT103** | Single Customer Credit Transfer | Payments (Cat 1) | Cross-border payment initiated by a corporate customer |
| **MT202** | General Financial Institution Transfer | Payments (Cat 2) | Bank-to-bank fund movement, nostro funding |
| **MT202COV** | General Financial Institution Transfer (Cover) | Payments (Cat 2) | Cover leg accompanying an MT103 under the serial payment method |
| **MT940** | Customer Statement Message | Cash Management (Cat 9) | Bank sends a detailed account statement to a corporate client |
| **MT950** | Statement Message | Cash Management (Cat 9) | Bank-to-bank nostro reconciliation statement |

---

## 3. Project Structure

```
swift-message-generator/
├── src/
│   ├── main/java/com/kazhuga/swift/
│   │   ├── core/
│   │   │   ├── SwiftField.java          # A single :TAG:VALUE field
│   │   │   ├── SwiftBlock.java          # One of the five SWIFT blocks
│   │   │   └── SwiftMessage.java        # Full message (blocks 1–5)
│   │   │
│   │   ├── fields/
│   │   │   └── SwiftFieldDefinitions.java  # 50+ field tag catalogue
│   │   │
│   │   ├── model/
│   │   │   └── SwiftData.java           # All input POJOs:
│   │   │                                #   TransferData, StatementData,
│   │   │                                #   StatementLine, BankParty, Customer
│   │   │
│   │   ├── util/
│   │   │   └── SwiftFormatUtil.java     # Date / amount / BIC helpers
│   │   │
│   │   ├── validation/
│   │   │   ├── ValidationResult.java    # Holds errors / warnings / info
│   │   │   ├── MT103Validator.java      # MT103-specific rules
│   │   │   └── SwiftMessageValidator.java  # MT202, MT940, MT950 rules
│   │   │
│   │   ├── messages/
│   │   │   ├── AbstractMessageBuilder.java  # Base class — headers + helpers
│   │   │   ├── MT103Builder.java
│   │   │   ├── MT202Builder.java        # Handles both MT202 and MT202COV
│   │   │   ├── MT940Builder.java        # Handles both MT940 and MT950
│   │   │   └── MT950Builder.java        # Thin wrapper over MT940Builder
│   │   │
│   │   ├── parser/
│   │   │   ├── SwiftParser.java         # Wire string → SwiftMessage
│   │   │   └── SwiftParseException.java
│   │   │
│   │   ├── generator/
│   │   │   └── SwiftGenerator.java      # ← Main entry point
│   │   │
│   │   └── demo/
│   │       ├── SampleDataFactory.java   # Realistic sample data
│   │       └── SwiftDemo.java           # Runnable demonstration
│   │
│   └── test/java/com/kazhuga/swift/
│       └── SwiftLibraryTests.java       # 61 self-contained unit tests
│
├── build.sh                             # Shell build script (no Maven needed)
├── pom.xml                              # Maven build file
├── LICENSE                             # MIT
└── README.md
```

---

## 4. Getting Started

### Prerequisites

- Java 11 or later (`java -version`)
- No other tools required

### Clone and run in 30 seconds

```bash
git clone https://github.com/YOUR_ORG/swift-message-generator.git
cd swift-message-generator
chmod +x build.sh
./build.sh          # compiles + runs demo
./build.sh test     # runs 61 unit tests
./build.sh clean    # removes compiled output
```

### Maven

```bash
mvn compile
mvn test
mvn exec:java -Dexec.mainClass=com.kazhuga.swift.demo.SwiftDemo
```

### Using in your own project

Copy the `src/main/java/com/kazhuga/swift/` tree into your source root. No configuration is required — all classes compile against the standard Java 11 library.

If you use Maven, add the source directory to your build or publish the JAR to your internal repository.

---

## 5. Core Concepts

### The five SWIFT blocks

Every FIN message is composed of up to five blocks:

```
{1:F01CHASUS33XXX0000123456}        ← Block 1: Basic Header
{2:I103HSBCGB2LXXXU}               ← Block 2: Application Header
{3:{108:ABCDE12345678901}}          ← Block 3: User Header (optional)
{4:                                 ← Block 4: Text Block (your fields)
:20:PAYREF001
:23B:CRED
:32A:230915USD125000,00
...
-}
{5:{CHK:C2JKGQNJ6688}}             ← Block 5: Trailer (optional)
```

This library manages all five blocks. You only need to populate the data model — the builder handles field ordering, formatting, and block construction.

### SWIFT field format

Block 4 fields follow the pattern `:TAG:VALUE`, where:

- **TAG** is 2 digits optionally followed by a letter — e.g. `32A`, `50K`, `71A`
- **VALUE** is free-form text governed by the field's format specification
- Multi-line values use `\n` within the value (e.g. name/address fields)

### SWIFT character set (set X)

Field values may only contain: `A–Z a–z 0–9 / - ? : ( ) . , ' + space newline`

`SwiftFormatUtil.sanitizeSwiftX(input)` strips any other characters.

---

## 6. Data Models

All input data is supplied through plain Java objects in `com.kazhuga.swift.model.SwiftData`.

### BankParty

Represents a bank or financial institution.

```java
BankParty bank = new BankParty();
bank.bic       = "HSBCGB2L";          // 8 or 11-char BIC
bank.accountNo = "GB29NWBK60161331926819"; // IBAN or account number
bank.name      = "HSBC BANK PLC";
bank.address1  = "8 CANADA SQUARE";
bank.city      = "LONDON";
bank.country   = "GB";
```

Convenience constructors:

```java
new BankParty("HSBCGB2L")                          // BIC only
new BankParty("HSBCGB2L", "GB29...", "HSBC BANK")  // BIC + account + name
```

### Customer

Represents an ordering customer or beneficiary (corporate or individual).

```java
Customer customer = new Customer();
customer.accountNo = "DE89370400440532013000";
customer.name      = "ACME CORPORATION";
customer.address1  = "100 MAIN STREET";
customer.address2  = "SUITE 200";
customer.city      = "BERLIN";
customer.country   = "DE";
customer.bic       = null;  // if set, field 50A / 59A is used instead of 50K / 59
```

### TransferData

Input for MT103, MT202, MT202COV.

```java
TransferData data = new TransferData();

// Mandatory for MT103
data.transactionReference = "PAYREF20230915001"; // max 16 chars
data.bankOperationCode    = "CRED";              // CRED / CRTS / SPAY / SSTD / SPRI
data.valueDate            = LocalDate.of(2023, 9, 15);
data.currency             = "USD";               // ISO 4217
data.amount               = new BigDecimal("125000.00");
data.detailsOfCharges     = "SHA";               // OUR / BEN / SHA
data.orderingCustomer     = ...;                 // Customer
data.beneficiaryCustomer  = ...;                 // Customer

// Mandatory for MT202 (in addition to above minus 23B / 71A)
data.relatedReference     = "USPAY20230915001";  // max 16 chars

// Optional
data.instructedCurrency   = "GBP";
data.instructedAmount     = new BigDecimal("99800.00");
data.exchangeRate         = new BigDecimal("1.25200");
data.orderingInstitution  = ...;  // BankParty → Field 52A
data.senderCorrespondent  = ...;  // BankParty → Field 53A
data.receiverCorrespondent= ...;  // BankParty → Field 54A
data.intermediaryBank     = ...;  // BankParty → Field 56A
data.accountWithInstitution=...;  // BankParty → Field 57A
data.remittanceInfo       = "INV/2023/09/00456 PAYMENT FOR SERVICES";
data.senderToReceiverInfo = "/ACC/FOR NOSTRO FUNDING";
data.regulatoryReporting  = "/ORDERRES/BE//MEILAAN 1, 1000 BRUSSEL";

// Block 1/2 headers (auto-generated defaults if omitted)
data.senderBic   = "CHASUS33XXX";
data.receiverBic = "HSBCGB2LXXX";
```

### StatementData

Input for MT940 / MT950.

```java
StatementData data = new StatementData();
data.transactionReference  = "STMT20230930EUR";
data.accountIdentification = "DE89370400440532013000"; // Field 25
data.accountBic            = "DEUTDEDB";               // Field 25P (optional)
data.statementNumber       = 9;                        // Field 28C first part
data.sequenceNumber        = 1;                        // Field 28C second part (0 = omit)
data.currency              = "EUR";
data.isIntermediate        = false;  // true → fields 60M/62M, false → 60F/62F
data.openingBalanceIndicator = 'C';  // C = credit, D = debit
data.openingDate             = LocalDate.of(2023, 9, 1);
data.openingBalance          = new BigDecimal("245300.00");
data.closingBalanceIndicator = 'C';
data.closingDate             = LocalDate.of(2023, 9, 30);
data.closingBalance          = new BigDecimal("340700.00");
// Optional closing available balance (Field 64)
data.closingAvailableBalance = new BigDecimal("340700.00");
data.closingAvailableDate    = LocalDate.of(2023, 9, 30);
data.senderBic   = "DEUTDEDBXXX";
data.receiverBic = "BMUNDE8BXXX";
// Statement lines added separately — see Section 7.4
```

### StatementLine

A single entry in an MT940/MT950 statement (Field 61 + optional Field 86).

```java
StatementLine line = new StatementLine(
    LocalDate.of(2023, 9, 4),  // value date
    true,                       // isCredit (true = C, false = D)
    new BigDecimal("12500.00"), // amount
    "TRF",                      // transaction type (3 chars)
    "INV456RECEIPT"             // reference for account owner (max 16 chars)
);
line.entryDate     = null;                        // optional MMDD entry date
line.isFunds       = false;                       // true adds 'F' to debit/credit indicator
line.additionalInfo = "Customer payment for Invoice 456"; // Field 86 (MT940 only)
line.referenceOfAccountServicingInstitution = "BANKREF001"; // optional //ref in Field 61
data.lines.add(line);
```

---

## 7. Generating Messages

All generation goes through `SwiftGenerator` — instantiate once and reuse.

```java
SwiftGenerator generator = new SwiftGenerator();
```

### 7.1 MT103 – Single Customer Credit Transfer

```java
TransferData data = new TransferData();
data.transactionReference = "PAYREF20230915001";
data.bankOperationCode    = "CRED";
data.detailsOfCharges     = "SHA";
data.valueDate            = LocalDate.of(2023, 9, 15);
data.currency             = "USD";
data.amount               = new BigDecimal("125000.00");

// Ordering customer
Customer oc = new Customer("US12345678901234567890", "ACME CORPORATION");
oc.address1 = "100 MAIN STREET";
oc.city     = "NEW YORK";
data.orderingCustomer = oc;

// Beneficiary
Customer bc = new Customer("GB29NWBK60161331926819", "BRITISH SUPPLIER LTD");
bc.address1 = "14 CANNON STREET";
bc.city     = "LONDON";
data.beneficiaryCustomer = bc;

// Account with institution (beneficiary bank)
data.accountWithInstitution = new BankParty("HSBCGB2L", "GB29NWBK60161331926819", "HSBC BANK PLC");

data.remittanceInfo = "INV/2023/09/00456 PAYMENT FOR SERVICES";
data.senderBic      = "CHASUS33XXX";
data.receiverBic    = "HSBCGB2LXXX";

// Generate
SwiftMessage msg  = generator.generateMT103(data);
String       wire = msg.toSwiftString();     // raw wire format
System.out.println(msg.toPrettyString());    // formatted debug output
```

**Required fields for MT103:** `transactionReference`, `bankOperationCode`, `valueDate`, `currency`, `amount`, `orderingCustomer`, `beneficiaryCustomer`, `detailsOfCharges`

**Optional fields:** `instructedCurrency/Amount` (33B), `exchangeRate` (36), `orderingInstitution` (52A), `senderCorrespondent` (53A), `receiverCorrespondent` (54A), `intermediaryBank` (56A), `accountWithInstitution` (57A), `remittanceInfo` (70), `senderToReceiverInfo` (72), `regulatoryReporting` (77B)

---

### 7.2 MT202 – Financial Institution Transfer

```java
TransferData data = new TransferData();
data.transactionReference = "NOSTRO20230915A";
data.relatedReference     = "PAYREF20230915001";  // mandatory for MT202
data.valueDate            = LocalDate.of(2023, 9, 15);
data.currency             = "USD";
data.amount               = new BigDecimal("5000000.00");

// Ordering institution
data.orderingInstitution  = new BankParty("CHASUS33");

// Beneficiary institution (Field 58A)
data.accountWithInstitution = new BankParty("HSBCGB2L", "400-123456", "HSBC BANK PLC");

data.senderBic   = "CHASUS33XXX";
data.receiverBic = "CITIUS33XXX";

SwiftMessage msg = generator.generateMT202(data);
```

**Required fields for MT202:** `transactionReference`, `relatedReference`, `valueDate`, `currency`, `amount`, and a beneficiary institution (via `accountWithInstitution` or `beneficiaryCustomer.bic`)

---

### 7.3 MT202COV – Cover Payment

MT202COV is identical to MT202 but also carries the underlying customer credit transfer details (fields 50K and 59) in a `/COV/` sub-sequence.

```java
TransferData data = new TransferData();
data.transactionReference = "COVER20230915001";
data.relatedReference     = "PAYREF20230915001";
data.valueDate            = LocalDate.of(2023, 9, 15);
data.currency             = "USD";
data.amount               = new BigDecimal("125000.00");

// Beneficiary institution
data.accountWithInstitution = new BankParty("HSBCGB2L", "400-123456", "HSBC BANK PLC");

// Underlying ordering customer
Customer oc = new Customer("US12345678901234567890", "ACME CORPORATION");
oc.address1 = "100 MAIN STREET, NEW YORK, US";
data.orderingCustomer = oc;

// Underlying beneficiary
Customer bc = new Customer("GB29NWBK60161331926819", "BRITISH SUPPLIER LTD");
bc.address1 = "14 CANNON STREET, LONDON, GB";
data.beneficiaryCustomer = bc;

data.remittanceInfo = "INV/2023/09/00456";
data.senderBic      = "CHASUS33XXX";
data.receiverBic    = "CITIUS33XXX";

SwiftMessage msg = generator.generateMT202COV(data);
```

---

### 7.4 MT940 – Customer Statement

```java
StatementData data = new StatementData();
data.transactionReference  = "STMT20230930EUR";
data.accountIdentification = "DE89370400440532013000";
data.accountBic            = "DEUTDEDB";      // adds field 25P
data.statementNumber       = 9;
data.sequenceNumber        = 1;
data.currency              = "EUR";
data.isIntermediate        = false;

// Opening balance
data.openingBalanceIndicator = 'C';
data.openingDate             = LocalDate.of(2023, 9, 1);
data.openingBalance          = new BigDecimal("245300.00");

// Statement lines
StatementLine credit = new StatementLine(
        LocalDate.of(2023, 9, 4), true,
        new BigDecimal("12500.00"), "TRF", "INV456RECEIPT");
credit.additionalInfo = "Customer payment for Invoice 456";
data.lines.add(credit);

StatementLine debit = new StatementLine(
        LocalDate.of(2023, 9, 7), false,
        new BigDecimal("3200.00"), "CHK", "CHQ00012345");
debit.additionalInfo = "Cheque payment – supplier";
data.lines.add(debit);

// Closing balance
data.closingBalanceIndicator = 'C';
data.closingDate             = LocalDate.of(2023, 9, 30);
data.closingBalance          = new BigDecimal("254600.00");

// Closing available balance (Field 64, optional)
data.closingAvailableBalance = new BigDecimal("254600.00");
data.closingAvailableDate    = LocalDate.of(2023, 9, 30);

data.senderBic   = "DEUTDEDBXXX";
data.receiverBic = "BMUNDE8BXXX";

SwiftMessage msg = generator.generateMT940(data);
```

**Note:** Field 86 (Information to Account Owner) is generated automatically from `StatementLine.additionalInfo`. It is omitted from MT950.

**Required fields for MT940:** `transactionReference`, `accountIdentification`, `statementNumber`, `currency`, `openingBalance`, `openingDate`, `openingBalanceIndicator`, `closingBalance`, `closingDate`, `closingBalanceIndicator`

---

### 7.5 MT950 – Bank Statement

MT950 is structurally identical to MT940 but intended for bank-to-bank exchange. Field 86 is never included.

```java
StatementData data = new StatementData();
data.transactionReference  = "NOSTROSTMT230930";
data.accountIdentification = "400-123456-USD";
data.statementNumber       = 3;
data.currency              = "USD";
data.openingBalanceIndicator = 'C';
data.openingDate             = LocalDate.of(2023, 9, 1);
data.openingBalance          = new BigDecimal("10000000.00");
// ... add lines ...
data.closingBalanceIndicator = 'C';
data.closingDate             = LocalDate.of(2023, 9, 30);
data.closingBalance          = new BigDecimal("9000000.00");

SwiftMessage msg = generator.generateMT950(data);
```

---

## 8. Parsing Wire Format

`SwiftParser` converts a raw SWIFT wire string back into a `SwiftMessage` object. All five blocks and all block-4 fields are extracted.

```java
SwiftParser parser = new SwiftParser();

String wire =
    "{1:F01CHASUS33XXX0000123456}" +
    "{2:I103HSBCGB2LXXXU}" +
    "{4:\r\n" +
    ":20:PAYREF001\r\n" +
    ":23B:CRED\r\n" +
    ":32A:230915USD125000,00\r\n" +
    ":50K:/US12345678901234567890\r\n" +
    "ACME CORPORATION\r\n" +
    ":59:/GB29NWBK60161331926819\r\n" +
    "BRITISH SUPPLIER LTD\r\n" +
    ":71A:SHA\r\n" +
    "-}";

try {
    SwiftMessage msg = parser.parse(wire);

    System.out.println(msg.getMessageType());        // "103"
    System.out.println(msg.getFieldValue("32A"));    // "230915USD125000,00"
    System.out.println(msg.getFieldValue("71A"));    // "SHA"

    // Access the full block 4
    for (SwiftField field : msg.getBlock4().getFields()) {
        System.out.println(field.getTag() + " → " + field.getValue());
    }

    // Get block 1 raw content
    System.out.println(msg.getBlock1().getRawContent());

} catch (SwiftParseException e) {
    System.err.println("Parse failed: " + e.getMessage());
}
```

### Round-trip test pattern

```java
// Generate
SwiftMessage original = generator.generateMT103(data);
String wire = original.toSwiftString();

// Parse back
SwiftMessage parsed = parser.parse(wire);

// Verify
assert original.getFieldValue("32A").equals(parsed.getFieldValue("32A"));
assert original.getFieldValue("71A").equals(parsed.getFieldValue("71A"));
```

---

## 9. Validation

Validation runs automatically inside every builder. You can also call it manually before building.

### Manual validation

```java
MT103Validator   validator = new MT103Validator();
ValidationResult result    = validator.validate(data);

if (!result.isValid()) {
    System.out.println("Errors: " + result.errorCount());
    for (ValidationResult.Issue issue : result.getIssues()) {
        // issue.severity  → ERROR / WARNING / INFO
        // issue.field     → field tag e.g. "32A"
        // issue.message   → human-readable description
        System.out.println(issue);
    }
} else {
    SwiftMessage msg = generator.generateMT103(data);
}
```

### MT202 / MT940 / MT950 validation

```java
SwiftMessageValidator validator = new SwiftMessageValidator();

ValidationResult vr202  = validator.validateMT202(transferData);
ValidationResult vr202c = validator.validateMT202COV(transferData);
ValidationResult vr940  = validator.validateMT940(statementData);
ValidationResult vr950  = validator.validateMT950(statementData);
```

### What is validated

**MT103**

| Field | Check |
|-------|-------|
| 20 | Mandatory, max 16 chars, SWIFT character set |
| 23B | Mandatory, must be CRED / CRTS / SPAY / SSTD / SPRI |
| 32A | Value date mandatory, currency = 3-letter ISO 4217, amount > 0 |
| 50x | Ordering customer mandatory |
| 59x | Beneficiary customer mandatory |
| 71A | Mandatory, must be OUR / BEN / SHA |
| 33B | If present: currency required and amount > 0 |
| 52A/53A etc. | If BIC present: format must be 8 or 11 chars (4a+2a+2c[+3c]) |
| 70 | Warning if > 140 characters (4×35 SWIFT limit) |

**MT940 / MT950**

| Field | Check |
|-------|-------|
| 20 | Mandatory |
| 25 | Account identification mandatory |
| 60F | Opening balance, date, and indicator (C/D) mandatory |
| 62F | Closing balance, date, and indicator (C/D) mandatory |
| 61 | Each line: value date, positive amount, and reference mandatory |

---

## 10. SWIFT Format Reference

### Date format — YYMMDD

```java
LocalDate.of(2023, 9, 15)  →  "230915"
```

`SwiftFormatUtil.formatDate(date)` handles this automatically.

### Amount format — comma decimal, no thousands separator

```java
new BigDecimal("125000.00")  →  "125000,00"
new BigDecimal("1234567.89") →  "1234567,89"
new BigDecimal("0.50")       →  "0,50"
```

`SwiftFormatUtil.formatAmount(amount)` handles this automatically.

### BIC format

| Length | Example | Meaning |
|--------|---------|---------|
| 8 chars | `CHASUS33` | Auto-padded to `CHASUS33XXX` |
| 11 chars | `CHASUS33XXX` | Used as-is |

BIC structure: `4a` (institution code) + `2a` (country) + `2c` (location) + optional `3c` (branch)

`SwiftFormatUtil.bic11(bic)` pads 8-char BICs automatically.

### Name and address fields (50K, 52D, 59, etc.)

```
/ACCOUNT_NUMBER     ← up to 34 chars, prefixed with /
NAME                ← up to 35 chars
ADDRESS LINE 1      ← up to 35 chars
ADDRESS LINE 2      ← up to 35 chars
```

`SwiftFormatUtil.formatNameAddress(accountNo, name, address1, address2)` builds this automatically.

### Block structure

```
Block 1: {1:F01<11-char-sender-BIC><session><sequence>}
Block 2: {2:I<3-char-MT><11-char-receiver-BIC><priority>}  ← I = Input, O = Output
Block 3: {3:{108:<user-reference>}}
Block 4: {4:\r\n:TAG:VALUE\r\n:TAG:VALUE\r\n-}
Block 5: {5:{CHK:<12-char-checksum>}}
```

---

## 11. Field Catalogue

`SwiftFieldDefinitions.getAll()` returns all 50+ registered fields. Below are the most commonly used ones.

### Payment fields (MT103 / MT202)

| Tag | Name | Format | Notes |
|-----|------|--------|-------|
| 20 | Transaction Reference Number | 16x | Mandatory |
| 21 | Related Reference | 16x | Mandatory for MT202 |
| 23B | Bank Operation Code | 4!a | CRED / CRTS / SPAY / SSTD / SPRI |
| 32A | Value Date/Currency/Amount | 6!n 3!a 15d | YYMMDD + ISO currency + comma amount |
| 33B | Currency/Instructed Amount | 3!a 15d | Optional |
| 36 | Exchange Rate | 12d | Optional |
| 50A/K/F | Ordering Customer | varies | 50K = name+address, 50A = BIC |
| 52A/D | Ordering Institution | varies | 52A = BIC, 52D = name+address |
| 53A/B | Sender's Correspondent | varies | |
| 54A | Receiver's Correspondent | varies | |
| 56A | Intermediary Institution | varies | |
| 57A/D | Account With Institution | varies | |
| 58A/D | Beneficiary Institution | varies | MT202 / MT202COV only |
| 59/A/F | Beneficiary Customer | varies | 59 = no option, 59A = BIC |
| 70 | Remittance Information | 4×35x | Free text, 4 lines of 35 chars |
| 71A | Details of Charges | 3!a | OUR / BEN / SHA |
| 72 | Sender to Receiver Information | 6×35x | |
| 77B | Regulatory Reporting | 3×35x | |

### Statement fields (MT940 / MT950)

| Tag | Name | Format | Notes |
|-----|------|--------|-------|
| 25 | Account Identification | 35x | |
| 25P | Account Identification + BIC | 35x + BIC | MT940 variant |
| 28C | Statement/Sequence Number | 5n[/5n] | |
| 60F/M | Opening Balance | 1!a 6!n 3!a 15d | F = final, M = intermediate |
| 61 | Statement Line | complex | Value date + D/C + amount + type + ref |
| 62F/M | Closing Balance | 1!a 6!n 3!a 15d | |
| 64 | Closing Available Balance | 1!a 6!n 3!a 15d | MT940 only |
| 65 | Forward Available Balance | 1!a 6!n 3!a 15d | MT940 only |
| 86 | Information to Account Owner | 6×65x | MT940 only; follows Field 61 |

---

## 12. Running Tests

The test suite requires no JUnit or test framework — it is entirely self-contained.

```bash
# Shell
./build.sh test

# Maven
mvn test

# Direct
javac -d out $(find src -name "*.java")
java -cp out com.kazhuga.swift.SwiftLibraryTests
```

Expected output:

```
Running SWIFT Library Tests  [com.kazhuga.swift]

  -- SwiftFormatUtil
    PASS: formatAmount 1234.56
    PASS: formatDate
    PASS: bic11 pads 8-char
    ... (9 tests)

  -- MT103Validator
    PASS: Valid sample passes
    PASS: Null data fails
    ... (6 tests)

  -- MT103 Generation    (15 tests)
  -- MT202 Generation    (5 tests)
  -- MT202COV Generation (4 tests)
  -- MT940 Generation    (11 tests)
  -- MT950 Generation    (4 tests)
  -- SwiftParser         (4 tests)
  -- Round-trip MT940    (3 tests)

----------------------------------------------
Results: 61 passed, 0 failed
```

---

## 13. Extending the Library

### Adding a new message type (e.g. MT101)

**Step 1 — Add field tags** (if any are new) to `SwiftFieldDefinitions.java`:

```java
register("50C", "Instructing Party", "[/34x]\\n4!a2!a2!c[3!c]", "101");
```

**Step 2 — Add a data model** in `SwiftData.java` (or reuse `TransferData`):

```java
public static class MT101Data {
    public String  transactionReference;
    public String  relatedReference;
    public List<MT101Transaction> transactions = new ArrayList<>();
    // ...
}
```

**Step 3 — Create a builder**:

```java
package com.kazhuga.swift.messages;

public class MT101Builder extends AbstractMessageBuilder {

    private final MT101Data data;

    public MT101Builder(MT101Data data) {
        super("101");
        this.data = data;
    }

    @Override
    public SwiftMessage build() {
        // validate
        // buildHeaders(senderBic, receiverBic, "101", 'U');
        // addField("20", data.transactionReference);
        // ... populate all fields in SWIFT sequence order ...
        return message;
    }
}
```

**Step 4 — Expose through the facade**:

```java
// In SwiftGenerator.java
public SwiftMessage generateMT101(MT101Data data) {
    return new MT101Builder(data).build();
}
```

**Step 5 — Add a validator** following the same pattern as `MT103Validator`.

### Adding a custom field formatter

`SwiftFormatUtil` is a plain utility class — add static methods:

```java
public static String formatIBAN(String iban) {
    // remove spaces, validate checksum, return formatted string
}
```

---

## 14. FAQ

**Q: Can I use this in a production SWIFT integration?**

A: The library generates syntactically correct SWIFT FIN messages. Whether they are accepted by a live SWIFT network depends on your institution's SWIFT connectivity, approved message profiles, and regulatory requirements. Always test against a SWIFT test environment before going live.

**Q: Does the library support MT202 under the new CBPR+ / ISO 20022 regime?**

A: This library targets the legacy SWIFT FIN (MT) format. ISO 20022 / MX messages use XML and are a separate standard. CBPR+ migration timelines vary — check with your SWIFT service bureau.

**Q: How do I handle multi-currency or exotic currencies?**

A: Pass any valid ISO 4217 currency code to `data.currency`. The library validates that the code is exactly three uppercase letters but does not restrict to a specific currency list.

**Q: Can I parse messages from external systems?**

A: Yes. `SwiftParser.parse(wire)` accepts any conformant SWIFT FIN wire string. Multi-line field values and all five blocks are handled.

**Q: Field 61 has a complex format — do I need to format it manually?**

A: No. `MT940Builder.buildStatementLine(line)` constructs the Field 61 value from your `StatementLine` POJO automatically.

**Q: How do I handle very long remittance information?**

A: SWIFT limits Field 70 to 4 lines of 35 characters (140 total). The validator issues a warning if `data.remittanceInfo` exceeds 140 characters. Truncate or split the text before generating the message. For structured remittance information, consider using the `/RFB/`, `/INV/` or `/USTRD/` prefixes within the 140-character limit.

**Q: Can I generate batch payments?**

A: MT103 is a single-payment message. For batch customer credit transfers, consider MT101 (not yet implemented) or ISO 20022 `pain.001`. You can generate multiple MT103 instances in a loop using this library.

**Q: Why is the checksum in Block 5 random?**

A: The Block 5 `{CHK:...}` value in real SWIFT FIN messages is computed by the SWIFT interface device (SID) after transmission — it is not part of the message content you compose. This library generates a placeholder that identifies the message in logs. Replace it with the real checksum if your infrastructure requires it.

---

## 15. Disclaimer

This library generates syntactically correct SWIFT FIN messages for integration, testing, and educational purposes.

- Field values and BICs in the sample data are **fictional** and for demonstration only.
- Production use in live SWIFT networks requires approval from SWIFT and compliance with your institution's regulatory and connectivity obligations.
- The authors make no warranty that messages generated by this library will be accepted by any specific bank, clearing system, or correspondent.
- This is not financial or legal advice.

---

*Generated by the Kazhuga SWIFT Message Generator — `com.kazhuga.swift` — MIT License*
