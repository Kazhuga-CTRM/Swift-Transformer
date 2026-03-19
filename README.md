# SWIFT & ISO 20022 Message Generator

> **Package:** `com.kazhuga.swift` · **License:** MIT · **Java:** 11+ · **Dependencies:** Zero

A pure Java library for generating, validating, and parsing **SWIFT FIN (MT)** and **ISO 20022 (MX)** financial messages. Built for treasury, commodities, and capital markets teams — fully compatible with **OpenLink Findur and Endur**.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Supported Message Types](#2-supported-message-types)
3. [Project Structure](#3-project-structure)
4. [Getting Started](#4-getting-started)
5. [Core Concepts](#5-core-concepts)
6. [SWIFT FIN (MT) Messages](#6-swift-fin-mt-messages)
   - [MT103 – Single Customer Credit Transfer](#61-mt103--single-customer-credit-transfer)
   - [MT202 – Financial Institution Transfer](#62-mt202--financial-institution-transfer)
   - [MT202COV – Cover Payment](#63-mt202cov--cover-payment)
   - [MT940 – Customer Statement](#64-mt940--customer-statement)
   - [MT950 – Bank Statement](#65-mt950--bank-statement)
7. [ISO 20022 (MX) Messages](#7-iso-20022-mx-messages)
   - [pain.009 – Mandate Initiation Request](#71-pain009--mandate-initiation-request)
   - [pain.009 Data Model](#72-pain009-data-model)
   - [pain.009 Generation](#73-pain009-generation)
   - [pain.009 Scenarios](#74-pain009-scenarios)
   - [pain.009 Validation](#75-pain009-validation)
8. [Parsing Wire Format](#8-parsing-wire-format)
9. [Validation Reference](#9-validation-reference)
10. [SWIFT Format Reference](#10-swift-format-reference)
11. [Field Catalogue](#11-field-catalogue)
12. [OpenLink Findur and Endur Integration](#12-openlink-findur-and-endur-integration)
13. [Running Tests](#13-running-tests)
14. [Extending the Library](#14-extending-the-library)
15. [FAQ](#15-faq)
16. [Disclaimer](#16-disclaimer)

---

## 1. Overview

The **SWIFT and ISO 20022 Message Generator** is a production-ready Java library that handles two message families from a single codebase:

- **SWIFT FIN (MT)** — legacy wire-format messages used by correspondent banking, nostro management, and treasury settlement
- **ISO 20022 (MX / pain)** — XML-based messages used by SEPA, modern clearing schemes, and direct debit mandate management

### Design principles

| Principle | Implementation |
|---|---|
| **Zero runtime dependencies** | No Spring, Jackson, or third-party jars |
| **Validate before you build** | Every builder validates input before emitting output |
| **Consistent facade** | `SwiftGenerator` and `ISO20022Generator` share the same API pattern |
| **Standards-correct output** | YYMMDD dates, comma decimals, 11-char BICs, correct field sequences, schema-valid XML |
| **Extensible architecture** | Add new message types by subclassing `AbstractMessageBuilder` or following the `Pain009Builder` pattern |

---

## 2. Supported Message Types

### SWIFT FIN (MT)

| Type | Name | Category | Use Case |
|------|------|----------|----------|
| **MT103** | Single Customer Credit Transfer | Payments (Cat 1) | Cross-border payment from corporate to beneficiary |
| **MT202** | General Financial Institution Transfer | Payments (Cat 2) | Bank-to-bank fund movement, nostro funding |
| **MT202COV** | Cover Payment | Payments (Cat 2) | Cover leg accompanying an MT103 |
| **MT940** | Customer Statement Message | Cash Management (Cat 9) | Bank to corporate account statement |
| **MT950** | Statement Message | Cash Management (Cat 9) | Bank-to-bank nostro reconciliation |

### ISO 20022 (MX)

| Type | Name | Schema Version | Use Case |
|------|------|----------------|----------|
| **pain.009** | Mandate Initiation Request | pain.009.001.08 | Establish a direct debit mandate — SEPA Core, B2B, or bilateral |

---

## 3. Project Structure

```
swift-message-generator/
├── src/
│   ├── main/java/com/kazhuga/swift/
│   │   │
│   │   ├── core/
│   │   │   ├── SwiftField.java            # A single :TAG:VALUE field
│   │   │   ├── SwiftBlock.java            # One of the five SWIFT blocks
│   │   │   └── SwiftMessage.java          # Full message (blocks 1–5)
│   │   │
│   │   ├── fields/
│   │   │   └── SwiftFieldDefinitions.java # 50+ field tag catalogue
│   │   │
│   │   ├── model/
│   │   │   └── SwiftData.java             # TransferData, StatementData,
│   │   │                                  # StatementLine, BankParty, Customer
│   │   │
│   │   ├── util/
│   │   │   └── SwiftFormatUtil.java       # Date / amount / BIC helpers
│   │   │
│   │   ├── validation/
│   │   │   ├── ValidationResult.java      # Holds errors / warnings / info
│   │   │   ├── MT103Validator.java
│   │   │   └── SwiftMessageValidator.java # MT202, MT940, MT950
│   │   │
│   │   ├── messages/
│   │   │   ├── AbstractMessageBuilder.java
│   │   │   ├── MT103Builder.java
│   │   │   ├── MT202Builder.java          # Handles MT202 and MT202COV
│   │   │   ├── MT940Builder.java          # Handles MT940 and MT950
│   │   │   └── MT950Builder.java
│   │   │
│   │   ├── parser/
│   │   │   ├── SwiftParser.java           # Wire string → SwiftMessage
│   │   │   └── SwiftParseException.java
│   │   │
│   │   ├── generator/
│   │   │   └── SwiftGenerator.java        # ← MT entry point
│   │   │
│   │   ├── iso20022/
│   │   │   ├── model/
│   │   │   │   └── Pain009Data.java       # All pain.009 POJOs
│   │   │   ├── pain009/
│   │   │   │   ├── Pain009Builder.java    # Generates pain.009 XML
│   │   │   │   ├── ISO20022Generator.java # ← MX entry point
│   │   │   │   └── Pain009SampleFactory.java
│   │   │   └── validation/
│   │   │       └── Pain009Validator.java
│   │   │
│   │   └── demo/
│   │       ├── SampleDataFactory.java
│   │       └── SwiftDemo.java
│   │
│   └── test/java/com/kazhuga/swift/
│       ├── SwiftLibraryTests.java         # 61 MT tests
│       └── iso20022/pain009/
│           └── Pain009Tests.java          # 58 pain.009 tests
│
├── build.sh
├── pom.xml
├── LICENSE
└── README.md
```

---

## 4. Getting Started

### Prerequisites

- Java 11 or later (`java -version`)
- No other tools required at runtime

### Clone and run

```bash
git clone https://github.com/YOUR_ORG/swift-message-generator.git
cd swift-message-generator
chmod +x build.sh
./build.sh          # compile and run MT demo
./build.sh test     # run all 119 tests
./build.sh clean    # remove compiled output
```

### Maven

```bash
mvn compile
mvn test
mvn exec:java -Dexec.mainClass=com.kazhuga.swift.demo.SwiftDemo
```

### Add to your own project

Copy the `src/main/java/com/kazhuga/swift/` tree into your source root. No configuration or dependency management is required — everything compiles against the standard Java 11 library.

---

## 5. Core Concepts

### SWIFT FIN message structure

Every FIN message has up to five blocks:

```
{1:F01CHASUS33XXX0000123456}        Block 1 – Basic Header
{2:I103HSBCGB2LXXXU}               Block 2 – Application Header
{3:{108:ABCDE12345678901}}          Block 3 – User Header (optional)
{4:                                 Block 4 – Text Block (your fields)
:20:PAYREF001
:23B:CRED
:32A:230915USD125000,00
...
-}
{5:{CHK:C2JKGQNJ6688}}             Block 5 – Trailer (optional)
```

You populate a data model. The builder handles field ordering, formatting, and block assembly.

### ISO 20022 message structure

pain.009 is an XML document conforming to the `pain.009.001.08` schema:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.009.001.08" ...>
  <MndtInitnReq>
    <GrpHdr>
      <MsgId>KAZHUGA-PAIN009-001</MsgId>
      ...
    </GrpHdr>
    <Mndt>
      <MndtId>MNDT-ENRG-2024-00145</MndtId>
      ...
    </Mndt>
  </MndtInitnReq>
</Document>
```

You populate `Pain009Data` POJOs. The builder handles XML construction, escaping, and indentation.

---

## 6. SWIFT FIN (MT) Messages

All MT generation goes through `SwiftGenerator`.

```java
SwiftGenerator generator = new SwiftGenerator();
```

### 6.1 MT103 – Single Customer Credit Transfer

```java
TransferData data = new TransferData();
data.transactionReference = "PAYREF20230915001";
data.bankOperationCode    = "CRED";
data.detailsOfCharges     = "SHA";
data.valueDate            = LocalDate.of(2023, 9, 15);
data.currency             = "USD";
data.amount               = new BigDecimal("125000.00");

Customer oc = new Customer("US12345678901234567890", "ACME CORPORATION");
oc.address1 = "100 MAIN STREET";
oc.city     = "NEW YORK";
data.orderingCustomer = oc;

Customer bc = new Customer("GB29NWBK60161331926819", "BRITISH SUPPLIER LTD");
bc.address1 = "14 CANNON STREET";
bc.city     = "LONDON";
data.beneficiaryCustomer = bc;

data.accountWithInstitution = new BankParty("HSBCGB2L");
data.remittanceInfo = "INV/2023/09/00456 PAYMENT FOR SERVICES";
data.senderBic      = "CHASUS33XXX";
data.receiverBic    = "HSBCGB2LXXX";

SwiftMessage msg  = generator.generateMT103(data);
String       wire = msg.toSwiftString();
System.out.println(msg.toPrettyString());
```

**Mandatory:** `transactionReference`, `bankOperationCode`, `valueDate`, `currency`, `amount`, `orderingCustomer`, `beneficiaryCustomer`, `detailsOfCharges`

**Optional:** `instructedCurrency/Amount` (33B), `exchangeRate` (36), `orderingInstitution` (52A), `senderCorrespondent` (53A), `receiverCorrespondent` (54A), `intermediaryBank` (56A), `accountWithInstitution` (57A), `remittanceInfo` (70), `senderToReceiverInfo` (72), `regulatoryReporting` (77B)

---

### 6.2 MT202 – Financial Institution Transfer

```java
TransferData data = new TransferData();
data.transactionReference = "NOSTRO20230915A";
data.relatedReference     = "PAYREF20230915001";   // mandatory for MT202
data.valueDate            = LocalDate.of(2023, 9, 15);
data.currency             = "USD";
data.amount               = new BigDecimal("5000000.00");
data.orderingInstitution  = new BankParty("CHASUS33");
data.accountWithInstitution = new BankParty("HSBCGB2L", "400-123456", "HSBC BANK PLC");
data.senderBic   = "CHASUS33XXX";
data.receiverBic = "CITIUS33XXX";

SwiftMessage msg = generator.generateMT202(data);
```

**Mandatory:** `transactionReference`, `relatedReference`, `valueDate`, `currency`, `amount`, beneficiary institution via `accountWithInstitution` or `beneficiaryCustomer.bic`

---

### 6.3 MT202COV – Cover Payment

```java
TransferData data = new TransferData();
data.transactionReference = "COVER20230915001";
data.relatedReference     = "PAYREF20230915001";
data.valueDate            = LocalDate.of(2023, 9, 15);
data.currency             = "USD";
data.amount               = new BigDecimal("125000.00");
data.accountWithInstitution = new BankParty("HSBCGB2L");

Customer oc = new Customer("US12345678901234567890", "ACME CORPORATION");
data.orderingCustomer = oc;

Customer bc = new Customer("GB29NWBK60161331926819", "BRITISH SUPPLIER LTD");
data.beneficiaryCustomer = bc;

data.remittanceInfo = "INV/2023/09/00456";
data.senderBic      = "CHASUS33XXX";
data.receiverBic    = "CITIUS33XXX";

SwiftMessage msg = generator.generateMT202COV(data);
```

---

### 6.4 MT940 – Customer Statement

```java
StatementData data = new StatementData();
data.transactionReference  = "STMT20230930EUR";
data.accountIdentification = "DE89370400440532013000";
data.accountBic            = "DEUTDEDB";
data.statementNumber       = 9;
data.sequenceNumber        = 1;
data.currency              = "EUR";
data.isIntermediate        = false;

data.openingBalanceIndicator = 'C';
data.openingDate             = LocalDate.of(2023, 9, 1);
data.openingBalance          = new BigDecimal("245300.00");

StatementLine credit = new StatementLine(
        LocalDate.of(2023, 9, 4), true, new BigDecimal("12500.00"), "TRF", "INV456RECEIPT");
credit.additionalInfo = "Customer payment for Invoice 456";
data.lines.add(credit);

data.closingBalanceIndicator = 'C';
data.closingDate             = LocalDate.of(2023, 9, 30);
data.closingBalance          = new BigDecimal("257800.00");
data.senderBic   = "DEUTDEDBXXX";
data.receiverBic = "BMUNDE8BXXX";

SwiftMessage msg = generator.generateMT940(data);
```

Field 86 (Information to Account Owner) is generated from `StatementLine.additionalInfo`. It is omitted from MT950.

---

### 6.5 MT950 – Bank Statement

```java
StatementData data = new StatementData();
data.transactionReference  = "NOSTROSTMT230930";
data.accountIdentification = "400-123456-USD";
data.statementNumber       = 3;
data.currency              = "USD";
data.openingBalanceIndicator = 'C';
data.openingDate             = LocalDate.of(2023, 9, 1);
data.openingBalance          = new BigDecimal("10000000.00");
// add lines
data.closingBalanceIndicator = 'C';
data.closingDate             = LocalDate.of(2023, 9, 30);
data.closingBalance          = new BigDecimal("9000000.00");

SwiftMessage msg = generator.generateMT950(data);
```

---

## 7. ISO 20022 (MX) Messages

### 7.1 pain.009 – Mandate Initiation Request

`pain.009.001.08` (MandateInitiationRequest) is sent by a creditor or its agent to a debtor's bank to establish a direct debit mandate. Once accepted, the mandate authorises future `pain.008` (CustomerDirectDebitInitiation) collections.

**Typical flow in treasury and commodities:**

```
Creditor              Creditor's Bank         Debtor's Bank
    |                      |                       |
    |── pain.009 ─────────►|── pain.009 ──────────►|
    |                      |                       | (mandate registered)
    |◄────────────────────|◄── pain.010/011 ───────|
    |  (accepted/amended)  |                       |
    |                      |                       |
    |── pain.008 ─────────►|── direct debit ───────►|
    |  (collect payment)   |                       |
```

All generation goes through `ISO20022Generator`.

```java
ISO20022Generator generator = new ISO20022Generator();
```

---

### 7.2 pain.009 Data Model

All input data lives in `com.kazhuga.swift.iso20022.model.Pain009Data` as static inner classes.

#### GroupHeader

```java
GroupHeader hdr = new GroupHeader();
hdr.messageId            = "KAZHUGA-PAIN009-001";  // max 35 chars, unique
hdr.creationDateTime     = LocalDateTime.now();
hdr.numberOfTransactions = 1;                        // must match mandates.size()

PartyId initiatingParty = new PartyId("KAZHUGA ENERGY TRADING LTD");
initiatingParty.lei = "5493001KJTIIGC8Y1R12";       // optional LEI
hdr.initiatingParty = initiatingParty;
```

#### MandateData — core fields

```java
MandateData m = new MandateData();

// Identification
m.mandateId        = "MNDT-ENRG-2024-00145";    // max 35 chars
m.mandateRequestId = "MREQ-ENRG-2024-00145";    // max 35 chars
m.creditorSchemeId = "DE98ZZZ09999999999";        // SEPA creditor identifier

// Mandate type
m.type = new MandateType();
m.type.sequenceType        = "RCUR";   // FRST / RCUR / FNAL / OOFF
m.type.localInstrumentCode = "CORE";   // CORE / B2B / COR1 / PRIV
m.type.categoryPurposeCode = "ENRG";   // optional

// Frequency and collection dates
m.frequency           = new Frequency("MNTH");  // DAIL/WEEK/TOWK/MNTH/QUTR/SEMI/YEAR/ADHO
m.firstCollectionDate = LocalDate.of(2024, 4, 1);
m.finalCollectionDate = LocalDate.of(2026, 3, 31);  // null = open-ended

// Maximum collection amount
m.maximumAmount = new BigDecimal("50000.00");
m.currency      = "EUR";

// Creditor
m.creditor        = new PartyId("KAZHUGA ENERGY TRADING LTD");
m.creditorAccount = new AccountId("DE89370400440532013000", "EUR");
m.creditorAgent   = new BankAgent("DEUTDEDB", "DEUTSCHE BANK AG");

// Debtor
m.debtor        = new PartyId("RHEIN INDUSTRIEWERKE GMBH");
m.debtorAccount = new AccountId("DE44500105175407324931", "EUR");
m.debtorAgent   = new BankAgent("COBADEFFXXX", "COMMERZBANK AG");

// Signature
m.signatureDate  = LocalDate.of(2024, 3, 10);
m.signaturePlace = "Frankfurt am Main";

// Purpose and remittance
m.purposeCode           = "ENRG";
m.remittanceInformation = "MONTHLY GAS SUPPLY CONTRACT REF/2024/ENRG/00145";
```

#### Supporting types

| Class | Key Fields | Notes |
|---|---|---|
| `PartyId` | `name`, `bic`, `lei`, `taxId`, `address` | Creditor or debtor party |
| `AccountId` | `iban`, `bban`, `currency`, `accountName` | IBAN preferred; BBAN as fallback |
| `BankAgent` | `bic`, `clearingSystemCode`, `memberIdentification`, `name`, `address` | BIC or clearing member ID required |
| `MandateType` | `sequenceType`, `localInstrumentCode`, `categoryPurposeCode` | Scheme and collection type |
| `Frequency` | `code`, `pointInTime` | How often collections occur |
| `AmendmentIndicator` | 8 boolean flags | Tracks which mandate fields have changed |
| `PostalAddress` | `streetName`, `buildingNumber`, `postCode`, `townName`, `country` | Used inside `PartyId` and `BankAgent` |

---

### 7.3 pain.009 Generation

```java
// 1. Build the group header
GroupHeader hdr = new GroupHeader();
hdr.messageId          = "KAZHUGA-PAIN009-001";
hdr.creationDateTime   = LocalDateTime.now();
hdr.numberOfTransactions = 1;
hdr.initiatingParty    = new PartyId("MY COMPANY NAME");

// 2. Create the message
Pain009Message message = new Pain009Message(hdr);

// 3. Build and add a mandate
MandateData m = new MandateData();
// ... populate fields ...
message.addMandate(m);  // automatically keeps NbOfTxs in sync

// 4. Generate XML
String xml = generator.generatePain009(message);
System.out.println(xml);
```

**Sample output (condensed):**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.009.001.08"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="urn:iso:std:iso:20022:tech:xsd:pain.009.001.08 pain.009.001.08.xsd">
  <MndtInitnReq>
    <GrpHdr>
      <MsgId>KAZHUGA-PAIN009-001</MsgId>
      <CreDtTm>2024-03-15T09:30:00</CreDtTm>
      <NbOfTxs>1</NbOfTxs>
      <InitgPty><Nm>KAZHUGA ENERGY TRADING LTD</Nm></InitgPty>
    </GrpHdr>
    <Mndt>
      <MndtId>MNDT-ENRG-2024-00145</MndtId>
      <MndtReqId>MREQ-ENRG-2024-00145</MndtReqId>
      <Tp>
        <SeqTp><Prtry>RCUR</Prtry></SeqTp>
        <LclInstrm><Prtry>CORE</Prtry></LclInstrm>
        <CtgyPurp><Cd>ENRG</Cd></CtgyPurp>
      </Tp>
      <Ocrncs>
        <Frqcy><Tp>MNTH</Tp></Frqcy>
        <FrstColltnDt>2024-04-01</FrstColltnDt>
        <FnlColltnDt>2026-03-31</FnlColltnDt>
      </Ocrncs>
      <MaxAmt Ccy="EUR">50000.00</MaxAmt>
      <CdtrAgt><FinInstnId><BICFI>DEUTDEDB</BICFI></FinInstnId></CdtrAgt>
      <Cdtr><Nm>KAZHUGA ENERGY TRADING LTD</Nm>...</Cdtr>
      <CdtrAcct><Id><IBAN>DE89370400440532013000</IBAN></Id><Ccy>EUR</Ccy></CdtrAcct>
      <DbtrAgt><FinInstnId><BICFI>COBADEFFXXX</BICFI></FinInstnId></DbtrAgt>
      <Dbtr><Nm>RHEIN INDUSTRIEWERKE GMBH</Nm>...</Dbtr>
      <DbtrAcct><Id><IBAN>DE44500105175407324931</IBAN></Id></DbtrAcct>
      <MndtRltdInf>
        <DtOfSgntr>2024-03-10</DtOfSgntr>
        <SgntrPlc>Frankfurt am Main</SgntrPlc>
      </MndtRltdInf>
      <RmtInf><Ustrd>MONTHLY GAS SUPPLY CONTRACT REF/2024/ENRG/00145</Ustrd></RmtInf>
    </Mndt>
  </MndtInitnReq>
</Document>
```

---

### 7.4 pain.009 Scenarios

`Pain009SampleFactory` provides four ready-to-run scenarios.

#### SEPA Core recurring — monthly energy collection

```java
Pain009Message msg = Pain009SampleFactory.sepaCoreMonthlySample();
// Energy supplier collecting monthly gas fees from an industrial customer
// Scheme: CORE   Sequence: RCUR   Frequency: MNTH   Max: EUR 50,000
String xml = new ISO20022Generator().generatePain009(msg);
```

#### B2B — weekly commodities margin calls

```java
Pain009Message msg = Pain009SampleFactory.commoditiesB2BSample();
// Exchange (LME Clear) collecting weekly variation margin from commodity firm
// Scheme: B2B   Sequence: RCUR   Frequency: WEEK   Advisory cap: USD 5,000,000
```

#### One-off — spot trade settlement

```java
Pain009Message msg = Pain009SampleFactory.oneOffSpotTradeSample();
// Single collection to settle a spot crude oil trade (50,000 bbl × USD 47.50)
// Scheme: CORE   Sequence: OOFF   Frequency: ADHO   Amount: USD 2,375,000
```

#### Amendment — debtor account change

```java
Pain009Message msg = Pain009SampleFactory.amendmentSample();
// Creditor updating debtor IBAN and bank after customer migrates to Postbank
// Flags: originalDebtorAccountChanged, originalDebtorAgentChanged
```

---

### 7.5 pain.009 Validation

Validation runs automatically inside `generatePain009()`. You can also run it manually before building.

```java
Pain009Validator validator = new Pain009Validator();
ValidationResult result    = validator.validate(message);

if (!result.isValid()) {
    for (ValidationResult.Issue issue : result.getIssues()) {
        System.out.println(issue.severity + " | " + issue.field + " | " + issue.message);
    }
}
```

#### Validation rules

| Field | Rule |
|---|---|
| `GrpHdr/MsgId` | Mandatory, max 35 characters |
| `GrpHdr/CreDtTm` | Mandatory |
| `GrpHdr/NbOfTxs` | Must equal the actual number of mandates |
| `GrpHdr/InitgPty/Nm` | Mandatory |
| `MndtId` | Mandatory, max 35 characters |
| `MndtReqId` | Mandatory, max 35 characters |
| `Tp/SeqTp` | Must be FRST, RCUR, FNAL, or OOFF |
| `Tp/LclInstrm` | If present: must be CORE, B2B, COR1, or PRIV |
| `Ocrncs/Frqcy` | If present: must be DAIL, WEEK, TOWK, MNTH, TOMN, QUTR, SEMI, YEAR, or ADHO |
| `FrstColltnDt` | Mandatory; warns if in the past |
| `FnlColltnDt` | If present: must be after `FrstColltnDt` |
| `MaxAmt` | If present: must be positive; ISO 4217 currency required |
| `Cdtr/Nm` | Mandatory, max 140 characters |
| `CdtrAcct/Id` | Either IBAN or BBAN required |
| `CdtrAgt/FinInstnId` | Either BIC or clearing system member ID required |
| `Dbtr/Nm` | Mandatory, max 140 characters |
| `DbtrAcct/Id` | Either IBAN or BBAN required |
| `DbtrAgt/FinInstnId` | Either BIC or clearing system member ID required |
| BIC fields | 8 or 11 chars: `4a+2a+2c[+3c]` |
| LEI | If present: exactly 20 characters |
| `Amdmnt/OrgnlMndtId` | Mandatory when any amendment flag is set |

---

## 8. Parsing Wire Format

`SwiftParser` converts a raw SWIFT FIN wire string back into a `SwiftMessage` object.

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
    System.out.println(msg.getMessageType());       // "103"
    System.out.println(msg.getFieldValue("32A"));   // "230915USD125000,00"
    System.out.println(msg.getFieldValue("71A"));   // "SHA"

    for (SwiftField field : msg.getBlock4().getFields()) {
        System.out.println(field.getTag() + " → " + field.getValue());
    }
} catch (SwiftParseException e) {
    System.err.println("Parse failed: " + e.getMessage());
}
```

---

## 9. Validation Reference

### MT103

| Field | Rule |
|---|---|
| 20 | Mandatory, max 16 chars, SWIFT character set |
| 23B | Mandatory: CRED / CRTS / SPAY / SSTD / SPRI |
| 32A | Value date, 3-letter ISO 4217 currency, positive amount — all mandatory |
| 50x | Ordering customer mandatory |
| 59x | Beneficiary customer mandatory |
| 71A | Mandatory: OUR / BEN / SHA |
| 33B | If present: currency required and amount > 0 |
| BIC fields | 8 or 11 chars: `4a+2a+2c[+3c]` |
| 70 | Warning if > 140 characters |

### MT202 / MT202COV

| Field | Rule |
|---|---|
| 20 | Mandatory, max 16 chars |
| 21 | Mandatory (related reference), max 16 chars |
| 32A | Value date, currency, amount all mandatory |
| 58A | Beneficiary institution mandatory |
| 50K / 59 | For MT202COV: underlying customer fields recommended |

### MT940 / MT950

| Field | Rule |
|---|---|
| 20 | Mandatory |
| 25 | Account identification mandatory |
| 60F/60M | Opening balance, date, and C/D indicator mandatory |
| 62F/62M | Closing balance, date, and C/D indicator mandatory |
| 61 | Each line: value date, positive amount, and reference mandatory |

---

## 10. SWIFT Format Reference

### Date — YYMMDD

```java
LocalDate.of(2023, 9, 15)  →  "230915"
```

### Amount — comma decimal, no thousands separator

```java
new BigDecimal("125000.00")  →  "125000,00"
new BigDecimal("0.50")       →  "0,50"
```

### BIC

| Length | Example | Notes |
|---|---|---|
| 8 chars | `CHASUS33` | Auto-padded to `CHASUS33XXX` |
| 11 chars | `CHASUS33XXX` | Used as-is |

### Name and address fields (50K, 52D, 59, etc.)

```
/ACCOUNT_NUMBER      up to 34 chars, prefixed with /
NAME                 up to 35 chars
ADDRESS LINE 1       up to 35 chars
ADDRESS LINE 2       up to 35 chars
```

### SWIFT character set (X)

Allowed: `A–Z  a–z  0–9  / - ? : ( ) . , ' +  space  newline`

---

## 11. Field Catalogue

`SwiftFieldDefinitions.getAll()` returns all 50+ registered field definitions.

### Payment fields (MT103 / MT202)

| Tag | Name | Format |
|---|---|---|
| 20 | Transaction Reference Number | 16x |
| 21 | Related Reference | 16x |
| 23B | Bank Operation Code | 4!a |
| 32A | Value Date / Currency / Amount | 6!n 3!a 15d |
| 33B | Currency / Instructed Amount | 3!a 15d |
| 36 | Exchange Rate | 12d |
| 50A/K/F | Ordering Customer | varies |
| 52A/D | Ordering Institution | varies |
| 53A/B | Sender's Correspondent | varies |
| 54A | Receiver's Correspondent | varies |
| 56A | Intermediary Institution | varies |
| 57A/D | Account With Institution | varies |
| 58A/D | Beneficiary Institution | varies |
| 59/A/F | Beneficiary Customer | varies |
| 70 | Remittance Information | 4×35x |
| 71A | Details of Charges | 3!a |
| 72 | Sender to Receiver Information | 6×35x |
| 77B | Regulatory Reporting | 3×35x |

### Statement fields (MT940 / MT950)

| Tag | Name | Format |
|---|---|---|
| 25 / 25P | Account Identification | 35x |
| 28C | Statement / Sequence Number | 5n[/5n] |
| 60F/M | Opening Balance | 1!a 6!n 3!a 15d |
| 61 | Statement Line | complex |
| 62F/M | Closing Balance | 1!a 6!n 3!a 15d |
| 64 | Closing Available Balance | 1!a 6!n 3!a 15d |
| 86 | Information to Account Owner | 6×65x |

---

## 12. OpenLink Findur and Endur Integration

The library maps directly from Findur and Endur deal objects without any middleware layer.

### MT103 from a Findur deal

```java
Deal deal = dealService.getDeal(dealId);
Counterparty cpty = deal.getCounterparty();

TransferData data = new TransferData();
data.transactionReference = "FND-" + deal.getDealId();
data.bankOperationCode    = "CRED";
data.detailsOfCharges     = "SHA";
data.valueDate            = deal.getSettlementDate();
data.currency             = deal.getCurrency();
data.amount               = deal.getSettlementAmount();

Customer oc = new Customer();
oc.accountNo = deal.getInternalAccount().getIban();
oc.name      = deal.getInternalAccount().getAccountName();
data.orderingCustomer = oc;

Customer bc = new Customer();
bc.accountNo = cpty.getAccount().getIban();
bc.name      = cpty.getLegalName();
bc.address1  = cpty.getAddress().getStreet();
bc.city      = cpty.getAddress().getCity();
data.beneficiaryCustomer = bc;

data.accountWithInstitution = new BankParty(cpty.getBank().getBic());
data.senderBic   = deal.getInternalBank().getBic11();
data.receiverBic = cpty.getBank().getBic11();
data.remittanceInfo = "TRD/" + deal.getDealReference();

String wire = new SwiftGenerator().generateMT103Wire(data);
swiftAdapter.send(wire);
```

### pain.009 mandate from an Endur direct debit agreement

```java
Agreement agreement = agreementService.get(agreementId);

GroupHeader hdr = new GroupHeader();
hdr.messageId          = "ENDUR-MNDT-" + agreement.getId();
hdr.creationDateTime   = LocalDateTime.now();
hdr.numberOfTransactions = 1;
hdr.initiatingParty    = new PartyId(agreement.getMyLegalEntity().getName());

MandateData m = new MandateData();
m.mandateId        = "MNDT-" + agreement.getId();
m.mandateRequestId = "MREQ-" + agreement.getId() + "-001";

m.type = new MandateType();
m.type.sequenceType        = "RCUR";
m.type.localInstrumentCode = "CORE";

m.frequency           = new Frequency("MNTH");
m.firstCollectionDate = agreement.getStartDate();
m.finalCollectionDate = agreement.getEndDate();
m.maximumAmount       = agreement.getMaximumCollectionAmount();
m.currency            = agreement.getCurrency();

m.creditor        = new PartyId(agreement.getMyLegalEntity().getName());
m.creditorAccount = new AccountId(agreement.getMyAccount().getIban(), agreement.getCurrency());
m.creditorAgent   = new BankAgent(agreement.getMyBank().getBic());

m.debtor        = new PartyId(agreement.getCounterparty().getLegalName());
m.debtorAccount = new AccountId(agreement.getCptyAccount().getIban(), agreement.getCurrency());
m.debtorAgent   = new BankAgent(agreement.getCptyBank().getBic());

m.signatureDate         = agreement.getSignatureDate();
m.remittanceInformation = agreement.getReference();

Pain009Message message = new Pain009Message(hdr);
message.addMandate(m);

String xml = new ISO20022Generator().generatePain009(message);
isoAdapter.submit(xml);
```

---

## 13. Running Tests

The test suites require no JUnit or external framework — they are entirely self-contained.

```bash
# All tests via shell script
./build.sh test

# All tests via Maven
mvn test

# MT suite only
java -cp out com.kazhuga.swift.SwiftLibraryTests

# pain.009 suite only
java -cp out com.kazhuga.swift.iso20022.pain009.Pain009Tests
```

Expected output:

```
MT suite  :  61 passed, 0 failed
pain.009  :  58 passed, 0 failed
Total     : 119 passed, 0 failed
```

---

## 14. Extending the Library

### Adding a new MT message type (e.g. MT101)

```java
// Step 1: create the builder
public class MT101Builder extends AbstractMessageBuilder {
    public MT101Builder(TransferData data) { super("101"); ... }

    @Override
    public SwiftMessage build() {
        buildHeaders(data.senderBic, data.receiverBic, "101", 'U');
        addField("20", data.transactionReference);
        addField("21", data.relatedReference);
        // add all remaining fields in SWIFT sequence order
        return message;
    }
}

// Step 2: expose through the facade
// In SwiftGenerator.java:
public SwiftMessage generateMT101(TransferData data) {
    return new MT101Builder(data).build();
}
```

Also add any new field tags to `SwiftFieldDefinitions.java` and create a validator following the `MT103Validator` pattern.

### Adding a new ISO 20022 message type (e.g. pain.008)

```java
// Step 1: create data POJOs in iso20022/model/Pain008Data.java
// Step 2: create the builder using the internal XmlWriter pattern
public class Pain008Builder {
    public String build() { ... }
}

// Step 3: expose through the facade
// In ISO20022Generator.java:
public String generatePain008(Pain008Data.Pain008Message message) {
    return new Pain008Builder(message).build();
}
```

---

## 15. FAQ

**Q: Can I use this in a live SWIFT production environment?**

The library generates syntactically and structurally correct messages. Acceptance by a live SWIFT network depends on your institution's SWIFT connectivity, approved message profiles, and regulatory requirements. Always test in a SWIFT test environment first.

**Q: Does pain.009 support SEPA B2B as well as SEPA Core?**

Yes. Set `m.type.localInstrumentCode = "B2B"` for Business-to-Business mandates. All other fields work identically.

**Q: How do I send multiple mandates in a single pain.009 message?**

Call `message.addMandate(mandate)` for each mandate. The library automatically keeps `NbOfTxs` in the group header in sync with the actual list size. There is no built-in limit on mandate count per message.

**Q: Can I use a BBAN instead of an IBAN in pain.009?**

Yes. Set `accountId.bban` instead of `accountId.iban`. The builder outputs `<Othr><Id>BBAN_VALUE</Id></Othr>` in place of `<IBAN>`.

**Q: How do I handle a mandate amendment?**

Populate `MandateData.amendment` with an `AmendmentIndicator` object and set the boolean flags for each field that has changed. Also set `m.originalMandateId` to the ID of the existing mandate. The validator rejects the message if amendment flags are set but `originalMandateId` is missing.

**Q: What is the relationship between pain.009 and pain.008?**

pain.009 registers the mandate (the authorisation). pain.008 (CustomerDirectDebitInitiation) is the subsequent collection instruction that references the registered mandate ID. pain.008 support is planned for a future release.

**Q: Field 61 in MT940 has a complex format — do I need to build it manually?**

No. `MT940Builder.buildStatementLine(line)` constructs the Field 61 value from your `StatementLine` POJO automatically, including value date, entry date, debit/credit indicator, amount, transaction type code, and references.

**Q: Does this library support ISO 20022 CBPR+ migration formats?**

The current release covers `pain.009.001.08`. CBPR+ migration for MT103 (`pacs.008`) and MT202 (`pacs.009`) is planned.

**Q: Why is the checksum in Block 5 a placeholder?**

The `{CHK:...}` value in real SWIFT FIN messages is computed by the SWIFT interface device after transmission and is not part of the message content you compose. The library generates a placeholder. Replace it with the real checksum if your infrastructure computes and verifies it.

---

## 16. Disclaimer

This library generates syntactically correct SWIFT FIN and ISO 20022 messages for integration, testing, and educational purposes.

Field values, IBANs, BICs, and amounts in the sample data are fictional and for demonstration only.

Production use in live SWIFT networks or SEPA clearing requires approval from the relevant network operators and compliance with your institution's regulatory and connectivity obligations.

The authors make no warranty that messages generated by this library will be accepted by any specific bank, clearing system, or correspondent.

This is not financial, legal, or regulatory advice.

---

*Kazhuga — `com.kazhuga.swift` — MIT License*
