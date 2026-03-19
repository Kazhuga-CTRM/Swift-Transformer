package com.kazhuga.swift.messages;

import com.kazhuga.swift.core.SwiftMessage;
import com.kazhuga.swift.model.SwiftData.BankParty;
import com.kazhuga.swift.model.SwiftData.Customer;
import com.kazhuga.swift.model.SwiftData.TransferData;
import com.kazhuga.swift.util.SwiftFormatUtil;
import com.kazhuga.swift.validation.MT103Validator;
import com.kazhuga.swift.validation.ValidationResult;

/**
 * Builds an MT103 – Single Customer Credit Transfer message.
 *
 * Mandatory fields : 20, 23B, 32A, 50K (or 50A/50F), 59 (or 59A/59F), 71A
 * Optional fields  : 13C, 23E, 26T, 33B, 36, 51A, 52A/D, 53A/B, 54A, 55A,
 *                    56A/C/D, 57A/B/C/D, 70, 71F, 71G, 72, 77B
 */
public class MT103Builder extends AbstractMessageBuilder {

    private final TransferData data;

    public MT103Builder(TransferData data) {
        super("103");
        if (data == null) throw new IllegalArgumentException("TransferData must not be null");
        this.data = data;
    }

    @Override
    public SwiftMessage build() {
        MT103Validator   validator = new MT103Validator();
        ValidationResult vr       = validator.validate(data);
        if (!vr.isValid()) {
            throw new IllegalStateException("MT103 validation failed:\n" + vr);
        }

        String sender   = data.senderBic   != null ? data.senderBic   : "BANKUS33XXX";
        String receiver = data.receiverBic != null ? data.receiverBic : "BANKGB2LXXX";
        buildHeaders(sender, receiver, "103", 'U');

        // 20 – Transaction Reference Number
        addField("20", SwiftFormatUtil.trunc(data.transactionReference, 16));

        // 23B – Bank Operation Code
        addField("23B", data.bankOperationCode.toUpperCase());

        // 23E – Instruction Code (optional)
        addFieldIfPresent("23E", data.instructionCode);

        // 32A – Value Date / Currency / Amount
        addField("32A",
                SwiftFormatUtil.formatDate(data.valueDate)
                + data.currency.toUpperCase()
                + SwiftFormatUtil.formatAmount(data.amount));

        // 33B – Currency / Instructed Amount (optional)
        if (data.instructedAmount != null && data.instructedCurrency != null) {
            addField("33B",
                    data.instructedCurrency.toUpperCase()
                    + SwiftFormatUtil.formatAmount(data.instructedAmount));
        }

        // 36 – Exchange Rate (optional)
        if (data.exchangeRate != null) {
            addField("36", SwiftFormatUtil.formatAmount(data.exchangeRate));
        }

        // 50K/50A – Ordering Customer
        buildOrderingCustomer();

        // 52A/52D – Ordering Institution (optional)
        buildOrderingInstitution();

        // 53A – Sender's Correspondent (optional)
        if (data.senderCorrespondent != null && data.senderCorrespondent.bic != null) {
            addField("53A", SwiftFormatUtil.formatBicParty(
                    data.senderCorrespondent.accountNo, data.senderCorrespondent.bic));
        }

        // 54A – Receiver's Correspondent (optional)
        if (data.receiverCorrespondent != null && data.receiverCorrespondent.bic != null) {
            addField("54A", SwiftFormatUtil.formatBicParty(
                    data.receiverCorrespondent.accountNo, data.receiverCorrespondent.bic));
        }

        // 56A – Intermediary Institution (optional)
        if (data.intermediaryBank != null && data.intermediaryBank.bic != null) {
            addField("56A", SwiftFormatUtil.formatBicParty(
                    data.intermediaryBank.accountNo, data.intermediaryBank.bic));
        }

        // 57A/57D – Account With Institution (optional)
        if (data.accountWithInstitution != null) {
            BankParty awi = data.accountWithInstitution;
            if (awi.bic != null && !awi.bic.isBlank()) {
                addField("57A", SwiftFormatUtil.formatBicParty(awi.accountNo, awi.bic));
            } else if (awi.name != null && !awi.name.isBlank()) {
                addField("57D", SwiftFormatUtil.formatNameAddress(
                        awi.accountNo, awi.name, awi.address1, awi.city));
            }
        }

        // 59/59A – Beneficiary Customer
        buildBeneficiaryCustomer();

        // 70 – Remittance Information (optional)
        addFieldIfPresent("70", data.remittanceInfo);

        // 71A – Details of Charges (mandatory)
        addField("71A", data.detailsOfCharges.toUpperCase());

        // 72 – Sender to Receiver Information (optional)
        addFieldIfPresent("72", data.senderToReceiverInfo);

        // 77B – Regulatory Reporting (optional)
        addFieldIfPresent("77B", data.regulatoryReporting);

        return message;
    }

    // -------------------------------------------------------------------------

    private void buildOrderingCustomer() {
        Customer oc = data.orderingCustomer;
        if (oc == null) return;
        if (oc.bic != null && !oc.bic.isBlank()) {
            addField("50A", SwiftFormatUtil.formatBicParty(oc.accountNo, oc.bic));
        } else {
            addField("50K", SwiftFormatUtil.formatNameAddress(
                    oc.accountNo, oc.name, oc.address1,
                    oc.address2 != null ? oc.address2 : oc.city));
        }
    }

    private void buildOrderingInstitution() {
        BankParty oi = data.orderingInstitution;
        if (oi == null) return;
        if (oi.bic != null && !oi.bic.isBlank()) {
            addField("52A", SwiftFormatUtil.formatBicParty(oi.accountNo, oi.bic));
        } else if (oi.name != null && !oi.name.isBlank()) {
            addField("52D", SwiftFormatUtil.formatNameAddress(
                    oi.accountNo, oi.name, oi.address1, oi.city));
        }
    }

    private void buildBeneficiaryCustomer() {
        Customer bc = data.beneficiaryCustomer;
        if (bc == null) return;
        if (bc.bic != null && !bc.bic.isBlank()) {
            addField("59A", SwiftFormatUtil.formatBicParty(bc.accountNo, bc.bic));
        } else {
            addField("59", SwiftFormatUtil.formatNameAddress(
                    bc.accountNo, bc.name, bc.address1,
                    bc.address2 != null ? bc.address2 : bc.city));
        }
    }
}
