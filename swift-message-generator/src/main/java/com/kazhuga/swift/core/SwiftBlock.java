package com.kazhuga.swift.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents one of the five SWIFT message blocks.
 *
 * Block 1 – Basic Header Block          {1:F01BANKBEBBAXXX0000000000}
 * Block 2 – Application Header Block    {2:I103BANKDEFFXXXXU3003} or {2:O103...}
 * Block 3 – User Header Block           {3:{108:REFXXXXXXX}}
 * Block 4 – Text Block                  {4:\n:20:...\n-}
 * Block 5 – Trailer Block               {5:{CHK:ABCDEF123456}}
 */
public class SwiftBlock {

    public enum BlockType {
        BLOCK1("Basic Header"),
        BLOCK2("Application Header"),
        BLOCK3("User Header"),
        BLOCK4("Text / Body"),
        BLOCK5("Trailer");

        private final String description;
        BlockType(String description) { this.description = description; }
        public String getDescription() { return description; }
        public int getNumber()         { return ordinal() + 1; }
    }

    private final BlockType       type;
    private       String          rawContent;   // blocks 1, 2, 5
    private final List<SwiftField> fields;      // blocks 3, 4

    public SwiftBlock(BlockType type) {
        this.type       = type;
        this.fields     = new ArrayList<>();
        this.rawContent = "";
    }

    public SwiftBlock(BlockType type, String rawContent) {
        this.type       = type;
        this.rawContent = rawContent == null ? "" : rawContent;
        this.fields     = new ArrayList<>();
    }

    public BlockType getType()             { return type; }
    public String    getRawContent()       { return rawContent; }
    public void      setRawContent(String rawContent) {
        this.rawContent = rawContent == null ? "" : rawContent;
    }

    public void addField(SwiftField field) {
        if (field != null) fields.add(field);
    }

    public List<SwiftField> getFields() {
        return Collections.unmodifiableList(fields);
    }

    /** Find the first field with the given tag */
    public SwiftField getField(String tag) {
        return fields.stream()
                .filter(f -> f.getTag().equalsIgnoreCase(tag))
                .findFirst()
                .orElse(null);
    }

    /** Find all fields with the given tag (some tags may repeat) */
    public List<SwiftField> getFields(String tag) {
        List<SwiftField> result = new ArrayList<>();
        for (SwiftField f : fields) {
            if (f.getTag().equalsIgnoreCase(tag)) result.add(f);
        }
        return Collections.unmodifiableList(result);
    }

    /** Renders the block in SWIFT wire format */
    public String toSwiftString() {
        StringBuilder sb  = new StringBuilder();
        int           num = type.getNumber();
        sb.append("{").append(num).append(":");

        switch (type) {
            case BLOCK1:
            case BLOCK2:
            case BLOCK5:
                sb.append(rawContent);
                break;

            case BLOCK3:
                for (SwiftField f : fields) {
                    sb.append("{").append(f.getTag()).append(":").append(f.getValue()).append("}");
                }
                break;

            case BLOCK4:
                sb.append("\r\n");
                for (SwiftField f : fields) {
                    sb.append(f.toSwiftString()).append("\r\n");
                }
                sb.append("-");
                break;
        }

        sb.append("}");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "SwiftBlock{type=" + type + ", fields=" + fields.size() + "}";
    }
}
