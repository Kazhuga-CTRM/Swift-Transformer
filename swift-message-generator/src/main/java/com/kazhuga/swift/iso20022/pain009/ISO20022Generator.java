package com.kazhuga.swift.iso20022.pain009;

import com.kazhuga.swift.iso20022.model.Pain009Data.Pain009Message;

/**
 * High-level facade for generating ISO 20022 messages.
 *
 * Currently supports:
 *   - pain.009.001.08 – MandateInitiationRequest
 *
 * Mirrors the pattern of {@link com.kazhuga.swift.generator.SwiftGenerator}
 * so client code has a consistent API across both SWIFT FIN (MT) and
 * ISO 20022 (MX) message families.
 *
 * <pre>
 *   ISO20022Generator generator = new ISO20022Generator();
 *
 *   Pain009Message msg = new Pain009Message(header);
 *   msg.addMandate(mandate);
 *
 *   String xml = generator.generatePain009(msg);
 * </pre>
 *
 * OpenLink Findur / Endur:
 *   Build a Pain009Message from deal and counterparty objects,
 *   then write the returned XML string to your ISO 20022 adapter or
 *   SWIFT Alliance gateway file drop.
 */
public class ISO20022Generator {

    /**
     * Generates a pain.009.001.08 XML document from the supplied message object.
     *
     * @param message  fully populated Pain009Message
     * @return         validated, indented UTF-8 XML string
     * @throws IllegalStateException if validation errors are found
     */
    public String generatePain009(Pain009Message message) {
        return new Pain009Builder(message).build();
    }

    /**
     * Generates pain.009 XML without running validation.
     * Useful for testing partial or draft mandate data.
     */
    public String generatePain009Unchecked(Pain009Message message) {
        return new Pain009Builder(message).buildUnchecked();
    }
}
