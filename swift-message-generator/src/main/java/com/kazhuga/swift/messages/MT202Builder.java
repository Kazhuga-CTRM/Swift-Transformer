package com.kazhuga.swift.messages;

import com.kazhuga.swift.core.SwiftMessage;
import com.kazhuga.swift.model.SwiftData.BankParty;
import com.kazhuga.swift.model.SwiftData.Customer;
import com.kazhuga.swift.model.SwiftData.TransferData;
import com.kazhuga.swift.util.SwiftFormatUtil;
import com.kazhuga.swift.validation.SwiftMessageValidator;
import com.kazhuga.swift.validation.ValidationResult;

/**
 * Builds an MT202 – General Financial Institution Transfer message.
 *
 * MT202 is used for bank-to-bank transfers (e.g. nostro account funding).
 * MT202COV is the cover variant used alongside MT103, carrying the
 * underlying customer credit information in field 50/59.
 *
 * Mandatory fields: 20, 21, 32A, 58A (Beneficiary Institution)
 */
public class MT202Builder extends AbstractMessageBuilder {

    private final TransferData data;
    private final boolean      isCov;

    /** Creates a standard MT202 builder */
    public MT202Builder(TransferData data) {
        this(data, false);
    }

    /** Creates an MT202 or MT202COV builder */
    public MT202Builder(TransferData data, boolean isCov) {
        super(isCov ? "202COV" : "202");
        if (data == null) throw new IllegalArgumentException("TransferData must not be null");
        this.data  = data;
        this.isCov = isCov;
    }

    @Override
    public SwiftMessage build() {
        SwiftMessageValidator validator = new SwiftMessageValidator();
        ValidationResult vr = isCov
                ? validator.validateMT202COV(data)
                : validator.validateMT202(data);
        if (!vr.isValid()) {
            throw new IllegalStateException(
                    (isCov ? "MT202COV" : "MT202") + " validation failed:\n" + vr);
        }

        String sender   = data.senderBic   != null ? data.senderBic   : "BANKUS33XXX";
        String receiver = data.receiverBic != null ? data.receiverBic : "BANKGB2LXXX";
        buildHeaders(sender, receiver, "202", 'U');

        // 20 – Transaction Reference Number
        addField("20", SwiftFormatUtil.trunc(data.transactionReference, 16));

        // 21 – Related Reference (mandatory)
        addField("21", SwiftFormatUtil.trunc(data.relatedReference, 16));

        // 32A – Value Date / Currency / Amount
        addField("32A",
                SwiftFormatUtil.formatDate(data.valueDate)
                + data.currency.toUpperCase()
                + SwiftFormatUtil.formatAmount(data.amount));

        // 52A – Ordering Institution (optional)
        if (data.orderingInstitution != null && data.orderingInstitution.bic != null) {
            addField("52A", SwiftFormatUtil.formatBicParty(
                    data.orderingInstitution.accountNo, data.orderingInstitution.bic));
        }

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

        // 57A – Account With Institution (optional)
        if (data.accountWithInstitution != null && data.accountWithInstitution.bic != null) {
            addField("57A", SwiftFormatUtil.formatBicParty(
                    data.accountWithInstitution.accountNo, data.accountWithInstitution.bic));
        }

        // 58A – Beneficiary Institution (mandatory)
        buildBeneficiaryInstitution();

        // 72 – Sender to Receiver Information (optional)
        addFieldIfPresent("72", data.senderToReceiverInfo);

        // MT202COV: underlying customer credit transfer fields
        if (isCov) {
            buildCovFields();
        }

        return message;
    }

    // -------------------------------------------------------------------------

    private void buildBeneficiaryInstitution() {
        BankParty awb = data.accountWithInstitution;
        if (awb != null && awb.bic != null && !awb.bic.isBlank()) {
            addField("58A", SwiftFormatUtil.formatBicParty(awb.accountNo, awb.bic));
            return;
        }
        Customer bc = data.beneficiaryCustomer;
        if (bc != null && bc.bic != null && !bc.bic.isBlank()) {
            addField("58A", SwiftFormatUtil.formatBicParty(bc.accountNo, bc.bic));
            return;
        }
        if (bc != null && bc.name != null) {
            addField("58D", SwiftFormatUtil.formatNameAddress(
                    bc.accountNo, bc.name, bc.address1, bc.city));
        }
    }

    private void buildCovFields() {
        Customer oc = data.orderingCustomer;
        if (oc != null && oc.name != null) {
            addField("50K", SwiftFormatUtil.formatNameAddress(
                    oc.accountNo, oc.name, oc.address1,
                    oc.address2 != null ? oc.address2 : oc.city));
        }
        Customer bc = data.beneficiaryCustomer;
        if (bc != null && bc.name != null) {
            addField("59", SwiftFormatUtil.formatNameAddress(
                    bc.accountNo, bc.name, bc.address1,
                    bc.address2 != null ? bc.address2 : bc.city));
        }
        addFieldIfPresent("70", data.remittanceInfo);
    }
}
