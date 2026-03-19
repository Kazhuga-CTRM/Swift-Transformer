package com.kazhuga.swift.core;

/**
 * Represents a single field in a SWIFT message block 4.
 * A field has a tag (e.g., "32A"), a name, and a value.
 *
 * SWIFT field tags follow the pattern: 2 digits + optional letter suffix
 * e.g., 20, 23B, 32A, 50K, 59F
 */
public class SwiftField {

    private final String tag;
    private final String value;
    private final String description;

    public SwiftField(String tag, String value) {
        this(tag, value, "");
    }

    public SwiftField(String tag, String value, String description) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("Field tag must not be null or blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("Field value must not be null (use empty string)");
        }
        this.tag         = tag.trim();
        this.value       = value;
        this.description = description == null ? "" : description;
    }

    /** Tag identifier e.g. "32A", "50K" */
    public String getTag()         { return tag; }

    /** Raw value of the field */
    public String getValue()       { return value; }

    /** Human-readable description of the field */
    public String getDescription() { return description; }

    /**
     * Renders the field in standard SWIFT format:
     *   :TAG:VALUE
     */
    public String toSwiftString() {
        return ":" + tag + ":" + value;
    }

    @Override
    public String toString() {
        return "SwiftField{tag='" + tag + "', value='" + value + "'}";
    }
}
