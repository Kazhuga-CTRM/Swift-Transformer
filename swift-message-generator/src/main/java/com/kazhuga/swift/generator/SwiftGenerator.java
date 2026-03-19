package com.kazhuga.swift.generator;

import com.kazhuga.swift.core.SwiftMessage;
import com.kazhuga.swift.messages.MT103Builder;
import com.kazhuga.swift.messages.MT202Builder;
import com.kazhuga.swift.messages.MT940Builder;
import com.kazhuga.swift.messages.MT950Builder;
import com.kazhuga.swift.model.SwiftData.StatementData;
import com.kazhuga.swift.model.SwiftData.TransferData;

/**
 * High-level facade for generating SWIFT messages.
 *
 * This is the main entry point for client code.
 *
 * <pre>
 *   SwiftGenerator generator = new SwiftGenerator();
 *
 *   TransferData data = new TransferData();
 *   data.transactionReference = "PAYREF20230915001";
 *   // ... populate remaining fields ...
 *
 *   SwiftMessage msg  = generator.generateMT103(data);
 *   String wire       = msg.toSwiftString();
 *   System.out.println(msg.toPrettyString());
 * </pre>
 */
public class SwiftGenerator {

    // =========================================================================
    //  MT103 – Single Customer Credit Transfer
    // =========================================================================

    /**
     * Generates an MT103 message from the supplied transfer data.
     *
     * @param data  fully populated TransferData object
     * @return      built SwiftMessage
     * @throws IllegalStateException if data fails validation
     */
    public SwiftMessage generateMT103(TransferData data) {
        return new MT103Builder(data).build();
    }

    /** Generates an MT103 wire string directly */
    public String generateMT103Wire(TransferData data) {
        return generateMT103(data).toSwiftString();
    }

    // =========================================================================
    //  MT202 – General Financial Institution Transfer
    // =========================================================================

    /** Generates an MT202 message */
    public SwiftMessage generateMT202(TransferData data) {
        return new MT202Builder(data, false).build();
    }

    /** Generates an MT202 wire string directly */
    public String generateMT202Wire(TransferData data) {
        return generateMT202(data).toSwiftString();
    }

    // =========================================================================
    //  MT202COV – Cover Payment
    // =========================================================================

    /** Generates an MT202COV message (cover for an MT103 SWIFT payment) */
    public SwiftMessage generateMT202COV(TransferData data) {
        return new MT202Builder(data, true).build();
    }

    /** Generates an MT202COV wire string directly */
    public String generateMT202COVWire(TransferData data) {
        return generateMT202COV(data).toSwiftString();
    }

    // =========================================================================
    //  MT940 – Customer Statement
    // =========================================================================

    /** Generates an MT940 customer statement message */
    public SwiftMessage generateMT940(StatementData data) {
        return new MT940Builder(data).build();
    }

    /** Generates an MT940 wire string directly */
    public String generateMT940Wire(StatementData data) {
        return generateMT940(data).toSwiftString();
    }

    // =========================================================================
    //  MT950 – Statement Message (bank-to-bank)
    // =========================================================================

    /** Generates an MT950 bank statement message */
    public SwiftMessage generateMT950(StatementData data) {
        return new MT950Builder(data).build();
    }

    /** Generates an MT950 wire string directly */
    public String generateMT950Wire(StatementData data) {
        return generateMT950(data).toSwiftString();
    }
}
