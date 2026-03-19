package com.kazhuga.swift.messages;

import com.kazhuga.swift.model.SwiftData.StatementData;

/**
 * Builds an MT950 – Statement Message (bank-to-bank nostro reconciliation).
 *
 * MT950 is structurally identical to MT940 except:
 * - No field 86 (Information to Account Owner)
 * - No field 25P variant
 * - No field 64/65 (available balance)
 * - Exchanged between financial institutions, not with customers
 */
public class MT950Builder extends MT940Builder {

    public MT950Builder(StatementData data) {
        super(data, true);
    }
}
