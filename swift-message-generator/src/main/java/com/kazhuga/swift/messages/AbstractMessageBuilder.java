package com.kazhuga.swift.messages;

import com.kazhuga.swift.core.SwiftBlock;
import com.kazhuga.swift.core.SwiftField;
import com.kazhuga.swift.core.SwiftMessage;
import com.kazhuga.swift.fields.SwiftFieldDefinitions;
import com.kazhuga.swift.util.SwiftFormatUtil;

/**
 * Abstract base class for all MT message builders.
 *
 * Subclasses call the protected helper methods to populate block 4,
 * then call buildHeaders() to set blocks 1–3 and 5.
 */
public abstract class AbstractMessageBuilder {

    protected SwiftMessage message;

    protected AbstractMessageBuilder(String messageType) {
        this.message = new SwiftMessage(messageType);
    }

    /** Build and return the finished SwiftMessage */
    public abstract SwiftMessage build();

    // -------------------------------------------------------------------------
    // Header construction
    // -------------------------------------------------------------------------

    /**
     * Populates blocks 1, 2, 3, 5 with standard values.
     *
     * @param senderBic   11-char sender BIC
     * @param receiverBic 11-char receiver BIC
     * @param messageType e.g. "103", "202"
     * @param priority    'U' = urgent, 'N' = normal
     */
    protected void buildHeaders(String senderBic, String receiverBic,
                                 String messageType, char priority) {
        String s = SwiftFormatUtil.bic11(senderBic).toUpperCase();
        String r = SwiftFormatUtil.bic11(receiverBic).toUpperCase();

        // Block 1 – Basic Header
        SwiftBlock b1 = new SwiftBlock(SwiftBlock.BlockType.BLOCK1,
                "F01" + s + "0000" + SwiftFormatUtil.zeroPad((int)(Math.random() * 999999), 6));
        message.setBlock1(b1);

        // Block 2 – Application Header (Input)
        SwiftBlock b2 = new SwiftBlock(SwiftBlock.BlockType.BLOCK2,
                "I" + messageType + r + priority);
        message.setBlock2(b2);

        // Block 3 – User Header
        SwiftBlock b3 = new SwiftBlock(SwiftBlock.BlockType.BLOCK3);
        b3.addField(new SwiftField("108", SwiftFormatUtil.generateReference(16)));
        message.setBlock3(b3);

        // Block 5 – Trailer
        SwiftBlock b5 = new SwiftBlock(SwiftBlock.BlockType.BLOCK5,
                "{CHK:" + SwiftFormatUtil.generateReference(12) + "}");
        message.setBlock5(b5);
    }

    // -------------------------------------------------------------------------
    // Field helpers (delegate to block 4)
    // -------------------------------------------------------------------------

    protected void addField(String tag, String value) {
        String description = SwiftFieldDefinitions.getName(tag);
        message.addField(new SwiftField(tag, value, description));
    }

    /** Only adds the field when value is non-blank */
    protected void addFieldIfPresent(String tag, String value) {
        if (value != null && !value.isBlank()) {
            addField(tag, value);
        }
    }
}
