# SWIFT Message Generator

[![Java](https://img.shields.io/badge/Java-11%2B-blue)](https://adoptium.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Zero Dependencies](https://img.shields.io/badge/dependencies-zero-brightgreen)](pom.xml)

A **pure Java** library for generating, parsing, and validating **SWIFT FIN messages** with zero external dependencies.

---

## Supported Message Types

| Type        | Name                                      | Category            |
|-------------|-------------------------------------------|---------------------|
| **MT103**   | Single Customer Credit Transfer           | Payments            |
| **MT202**   | General Financial Institution Transfer    | Payments            |
| **MT202COV**| Cover Payment (MT103 cover)               | Payments            |
| **MT940**   | Customer Statement Message                | Cash Management     |
| **MT950**   | Statement Message (bank-to-bank)          | Cash Management     |

---

## Project Structure

```
swift-message-generator/
├── src/
│   ├── main/java/com/swift/
│   │   ├── core/                  # SwiftField, SwiftBlock, SwiftMessage
│   │   ├── fields/                # Field tag catalogue & metadata
│   │   ├── model/                 # POJOs: TransferData, StatementData, …
│   │   ├── util/                  # SwiftFormatUtil (dates, amounts, BICs)
│   │   ├── validation/            # MT103Validator, SwiftMessageValidator
│   │   ├── messages/              # MT103Builder, MT202Builder, MT940Builder, MT950Builder
│   │   ├── parser/                # SwiftParser  (wire format → SwiftMessage)
│   │   ├── generator/             # SwiftGenerator (facade / entry point)
│   │   └── demo/                  # SampleDataFactory, SwiftDemo
│   └── test/java/com/swift/
│       └── SwiftLibraryTests.java # Self-contained test suite
├── build.sh                       # Shell build & run script
├── pom.xml                        # Maven build file
└── README.md
```

---

## Quick Start

### Prerequisites

- Java 11 or later
- No additional libraries needed at runtime

### Build & Run (Shell)

```bash
# Clone
git clone https://github.com/YOUR_ORG/swift-message-generator.git
cd swift-message-generator

# Compile and run the demo
chmod +x build.sh
./build.sh

# Run tests
./build.sh test
```

### Build & Run (Maven)

```bash
mvn compile exec:java -Dexec.mainClass=com.kazhuga.swift.demo.SwiftDemo
mvn test
```

---

## Usage Examples

### Generate an MT103

```java
import com.kazhuga.swift.generator.SwiftGenerator;
import com.kazhuga.swift.model.SwiftData.*;
import com.kazhuga.swift.core.SwiftMessage;
import java.math.BigDecimal;
import java.time.LocalDate;

SwiftGenerator generator = new SwiftGenerator();

// 1. Populate data
TransferData data = new TransferData();
data.transactionReference = "PAYREF20230915001";
data.bankOperationCode    = "CRED";
data.detailsOfCharges     = "SHA";
data.valueDate            = LocalDate.of(2023, 9, 15);
data.currency             = "USD";
data.amount               = new BigDecimal("125000.00");

data.orderingCustomer = new Customer("US12345678901234567890", "ACME CORPORATION");
data.orderingCustomer.address1 = "100 MAIN STREET";
data.orderingCustomer.city     = "NEW YORK";

data.beneficiaryCustomer = new Customer("GB29NWBK60161331926819", "BRITISH SUPPLIER LTD");
data.beneficiaryCustomer.address1 = "14 CANNON STREET";
data.beneficiaryCustomer.city     = "LONDON";

data.accountWithInstitution = new BankParty("HSBCGB2L");
data.remittanceInfo = "INV/2023/09/00456 PAYMENT FOR SERVICES";

data.senderBic   = "CHASUS33XXX";
data.receiverBic = "HSBCGB2LXXX";

// 2. Generate
SwiftMessage msg = generator.generateMT103(data);

// 3. Get wire format
String wire = msg.toSwiftString();
System.out.println(wire);

// 4. Pretty-print for debugging
System.out.println(msg.toPrettyString());
```

**Wire output:**
```
{1:F01CHASUS33XXX0000123456}{2:IHSBCGB2LXXXU}{3:{108:ABCDE12345678901}}{4:
:20:PAYREF20230915001
:23B:CRED
:32A:230915USD125000,00
:50K:/US12345678901234567890
ACME CORPORATION
100 MAIN STREET
NEW YORK
:57A:/GB29NWBK60161331926819
HSBCGB2LXXX
:59:/GB29NWBK60161331926819
BRITISH SUPPLIER LTD
14 CANNON STREET
LONDON
:70:INV/2023/09/00456 PAYMENT FOR SERVICES
:71A:SHA
-}{5:{CHK:ABCDEF123456}}
```

---

### Generate an MT202

```java
TransferData data = new TransferData();
data.transactionReference = "NOSTRO20230915A";
data.relatedReference     = "PAYREF20230915001";
data.valueDate            = LocalDate.of(2023, 9, 15);
data.currency             = "USD";
data.amount               = new BigDecimal("5000000.00");

data.accountWithInstitution = new BankParty("HSBCGB2L", "GB29NWBK60161331926819", "HSBC BANK PLC");

data.senderBic   = "CHASUS33XXX";
data.receiverBic = "CITIUS33XXX";

SwiftMessage msg = generator.generateMT202(data);
System.out.println(msg.toSwiftString());
```

---

### Generate an MT940 Statement

```java
import com.kazhuga.swift.model.SwiftData.*;
import java.math.BigDecimal;
import java.time.LocalDate;

StatementData data = new StatementData();
data.transactionReference  = "STMT20230930EUR";
data.accountIdentification = "DE89370400440532013000";
data.accountBic            = "DEUTDEDB";
data.statementNumber       = 9;
data.currency              = "EUR";

data.openingBalanceIndicator = 'C';
data.openingDate             = LocalDate.of(2023, 9, 1);
data.openingBalance          = new BigDecimal("245300.00");

// Add statement lines
StatementLine line = new StatementLine(
    LocalDate.of(2023, 9, 4), true,
    new BigDecimal("12500.00"), "TRF", "INV456RECEIPT"
);
line.additionalInfo = "Customer payment for Invoice 456";
data.lines.add(line);

data.closingBalanceIndicator = 'C';
data.closingDate             = LocalDate.of(2023, 9, 30);
data.closingBalance          = new BigDecimal("257800.00");

data.senderBic   = "DEUTDEDBXXX";
data.receiverBic = "BMUNDE8BXXX";

SwiftMessage msg = generator.generateMT940(data);
System.out.println(msg.toSwiftString());
```

---

### Parse a SWIFT Wire String

```java
import com.kazhuga.swift.parser.SwiftParser;

SwiftParser parser = new SwiftParser();

String wire = "{1:F01CHASUS33XXX0000123456}" +
              "{2:IHSBCGB2LXXXU}" +
              "{4:\r\n" +
              ":20:PAYREF001\r\n" +
              ":23B:CRED\r\n" +
              ":32A:230915USD50000,00\r\n" +
              "-}";

SwiftMessage msg = parser.parse(wire);
System.out.println("Field 32A: " + msg.getFieldValue("32A"));
// → 230915USD50000,00
```

---

### Validate Before Building

```java
import com.kazhuga.swift.validation.MT103Validator;
import com.kazhuga.swift.validation.ValidationResult;

MT103Validator validator = new MT103Validator();
ValidationResult result  = validator.validate(data);

if (result.isValid()) {
    SwiftMessage msg = generator.generateMT103(data);
    // ...
} else {
    for (ValidationResult.Issue issue : result.getIssues()) {
        System.out.println(issue);
    }
}
```

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `SwiftGenerator` | **Main entry point** – produces any message type |
| `SwiftMessage` | Full message (blocks 1–5); call `.toSwiftString()` or `.toPrettyString()` |
| `SwiftField` | A single tagged field (`:TAG:VALUE`) |
| `SwiftBlock` | One of the five message blocks |
| `TransferData` | Input POJO for MT103, MT202, MT202COV |
| `StatementData` | Input POJO for MT940, MT950 |
| `StatementLine` | Single statement entry for Field 61 |
| `BankParty` | Identifies a bank (BIC, account, name/address) |
| `Customer` | Identifies a corporate or individual |
| `SwiftParser` | Parses wire-format strings back to `SwiftMessage` |
| `MT103Validator` | Validates `TransferData` for MT103 compliance |
| `SwiftMessageValidator` | Validates MT202, MT202COV, MT940, MT950 |
| `SwiftFormatUtil` | Utility: dates, amounts, BIC formatting |
| `SwiftFieldDefinitions` | Catalogue of all standard field tags & formats |
| `SampleDataFactory` | Ready-to-use sample data for all message types |

---

## SWIFT Format Notes

| Convention | Detail |
|------------|--------|
| Date format | `YYMMDD` (e.g. `230915` for 15 Sep 2023) |
| Amount format | Comma as decimal separator, no thousands separator (e.g. `125000,00`) |
| BIC | 8 chars (auto-padded to 11 with `XXX`) or 11 chars |
| Account | Up to 34 chars, prefixed with `/` in name/address fields |
| Character set | SWIFT X: `A–Z a–z 0–9 / - ? : ( ) . , ' + space` |
| Field delimiter | `:TAG:VALUE` — each field on a new line inside block 4 |

---

## Running the Demo

```bash
./build.sh
```

Output includes all five message types, a round-trip parse test, and formatted wire strings ready to drop into any SWIFT test harness.

---

## Extending the Library

To add a new message type (e.g. MT101):
1. Add any new field tags to `SwiftFieldDefinitions`.
2. Add a `MT101Data` inner class to `SwiftData` (or reuse `TransferData`).
3. Create `MT101Builder extends AbstractMessageBuilder`, implement `build()`.
4. Add a `generateMT101()` method to `SwiftGenerator`.
5. Add a validator in `validation/` following the same pattern.

---

## License

MIT License – see [LICENSE](LICENSE). Free to use, modify, and distribute for commercial or non-commercial purposes.

---

## Disclaimer

This library generates syntactically correct SWIFT FIN messages for integration and testing purposes. Production use in live SWIFT networks requires approval from SWIFT and compliance with your institution's regulatory and connectivity requirements. Field values and amounts in the sample data are fictional.
