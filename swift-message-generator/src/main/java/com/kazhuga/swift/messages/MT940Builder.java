package com.kazhuga.swift.messages;

import com.kazhuga.swift.core.SwiftMessage;
import com.kazhuga.swift.model.SwiftData.StatementData;
import com.kazhuga.swift.model.SwiftData.StatementLine;
import com.kazhuga.swift.util.SwiftFormatUtil;
import com.kazhuga.swift.validation.SwiftMessageValidator;
import com.kazhuga.swift.validation.ValidationResult;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Builds MT940 (Customer Statement) and MT950 (Statement Message) messages.
 *
 * MT940 – sent by a bank to its customer (account owner)
 * MT950 – sent bank-to-bank (e.g. nostro reconciliation); no field 86
 *
 * Mandatory fields: 20, 25, 28C, 60F/M, {61 [86]}, 62F/M
 */
public class MT940Builder extends AbstractMessageBuilder {

    protected final StatementData data;
    private   final boolean       isMT950;

    public MT940Builder(StatementData data) {
        this(data, false);
    }

    protected MT940Builder(StatementData data, boolean isMT950) {
        super(isMT950 ? "950" : "940");
        if (data == null) throw new IllegalArgumentException("StatementData must not be null");
        this.data   = data;
        this.isMT950 = isMT950;
    }

    @Override
    public SwiftMessage build() {
        SwiftMessageValidator validator = new SwiftMessageValidator();
        ValidationResult vr = isMT950
                ? validator.validateMT950(data)
                : validator.validateMT940(data);
        if (!vr.isValid()) {
            throw new IllegalStateException(
                    (isMT950 ? "MT950" : "MT940") + " validation failed:\n" + vr);
        }

        String sender   = data.senderBic   != null ? data.senderBic   : "BANKUS33XXX";
        String receiver = data.receiverBic != null ? data.receiverBic : "BANKGB2LXXX";
        buildHeaders(sender, receiver, isMT950 ? "950" : "940", 'N');

        // 20 – Transaction Reference
        addField("20", SwiftFormatUtil.trunc(data.transactionReference, 16));

        // 25 / 25P – Account Identification
        if (data.accountBic != null && !data.accountBic.isBlank()) {
            addField("25P", data.accountIdentification + "\n"
                    + SwiftFormatUtil.bic11(data.accountBic));
        } else {
            addField("25", data.accountIdentification);
        }

        // 28C – Statement / Sequence Number
        String field28 = SwiftFormatUtil.zeroPad(data.statementNumber, 5);
        if (data.sequenceNumber > 0) {
            field28 += "/" + SwiftFormatUtil.zeroPad(data.sequenceNumber, 5);
        }
        addField("28C", field28);

        // 60F/M – Opening Balance
        addField(data.isIntermediate ? "60M" : "60F",
                buildBalanceLine(data.openingBalanceIndicator, data.openingDate,
                        data.currency, data.openingBalance));

        // 61 + 86 – Statement Lines
        for (StatementLine line : data.lines) {
            addField("61", buildStatementLine(line));
            if (!isMT950 && line.additionalInfo != null && !line.additionalInfo.isBlank()) {
                addField("86", SwiftFormatUtil.trunc(line.additionalInfo, 390));
            }
        }

        // 62F/M – Closing Balance
        addField(data.isIntermediate ? "62M" : "62F",
                buildBalanceLine(data.closingBalanceIndicator, data.closingDate,
                        data.currency, data.closingBalance));

        // 64 – Closing Available Balance (MT940 only, optional)
        if (!isMT950 && data.closingAvailableBalance != null && data.closingAvailableDate != null) {
            addField("64", buildBalanceLine(data.closingBalanceIndicator,
                    data.closingAvailableDate, data.currency, data.closingAvailableBalance));
        }

        return message;
    }

    // -------------------------------------------------------------------------

    /** Builds a balance field value: indicator + date + currency + amount */
    protected String buildBalanceLine(char indicator, LocalDate date,
                                       String currency, BigDecimal amount) {
        return String.valueOf(indicator)
                + SwiftFormatUtil.formatDate(date)
                + currency.toUpperCase()
                + SwiftFormatUtil.formatAmount(amount);
    }

    /**
     * Builds a field 61 (Statement Line) value.
     *
     * Format: 6!n[4!n]2a[1!a]15d1!a3!c16x[//16x][\n34x]
     * Example: "2309150914C1250,00NTRFCORP-PAY-001"
     */
    protected String buildStatementLine(StatementLine line) {
        StringBuilder sb = new StringBuilder();

        sb.append(SwiftFormatUtil.formatDate(line.valueDate));

        if (line.entryDate != null) {
            sb.append(SwiftFormatUtil.formatMonthDay(line.entryDate));
        }

        sb.append(line.isCredit ? "C" : "D");
        if (line.isFunds) sb.append("F");

        sb.append(SwiftFormatUtil.formatAmount(line.amount));

        String txType = line.transactionType != null ? line.transactionType : "TRF";
        sb.append("N").append(SwiftFormatUtil.trunc(txType, 3).toUpperCase());

        sb.append(SwiftFormatUtil.trunc(
                line.referenceForAccountOwner != null ? line.referenceForAccountOwner : "NONREF", 16));

        if (line.referenceOfAccountServicingInstitution != null
                && !line.referenceOfAccountServicingInstitution.isBlank()) {
            sb.append("//").append(SwiftFormatUtil.trunc(
                    line.referenceOfAccountServicingInstitution, 16));
        }

        return sb.toString();
    }
}
