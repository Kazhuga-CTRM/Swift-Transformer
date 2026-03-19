package com.kazhuga.swift.core;

/**
 * Represents a complete SWIFT FIN message, composed of up to 5 blocks.
 *
 * Structure:
 *   {1:...}{2:...}{3:...}{4:\n...\n-}{5:...}
 */
public class SwiftMessage {

    private SwiftBlock block1;  // Basic Header
    private SwiftBlock block2;  // Application Header
    private SwiftBlock block3;  // User Header      (optional)
    private SwiftBlock block4;  // Text Block       (mandatory)
    private SwiftBlock block5;  // Trailer          (optional)

    private final String messageType;  // e.g. "103", "202", "940"

    public SwiftMessage(String messageType) {
        if (messageType == null || messageType.isBlank()) {
            throw new IllegalArgumentException("Message type must not be blank");
        }
        this.messageType = messageType.trim();
        this.block1 = new SwiftBlock(SwiftBlock.BlockType.BLOCK1);
        this.block2 = new SwiftBlock(SwiftBlock.BlockType.BLOCK2);
        this.block3 = new SwiftBlock(SwiftBlock.BlockType.BLOCK3);
        this.block4 = new SwiftBlock(SwiftBlock.BlockType.BLOCK4);
        this.block5 = new SwiftBlock(SwiftBlock.BlockType.BLOCK5);
    }

    // -------------------------------------------------------------------------
    // Getters / setters
    // -------------------------------------------------------------------------

    public String     getMessageType()              { return messageType; }
    public SwiftBlock getBlock1()                   { return block1; }
    public void       setBlock1(SwiftBlock block1)  { this.block1 = block1; }
    public SwiftBlock getBlock2()                   { return block2; }
    public void       setBlock2(SwiftBlock block2)  { this.block2 = block2; }
    public SwiftBlock getBlock3()                   { return block3; }
    public void       setBlock3(SwiftBlock block3)  { this.block3 = block3; }
    public SwiftBlock getBlock4()                   { return block4; }
    public void       setBlock4(SwiftBlock block4)  { this.block4 = block4; }
    public SwiftBlock getBlock5()                   { return block5; }
    public void       setBlock5(SwiftBlock block5)  { this.block5 = block5; }

    // -------------------------------------------------------------------------
    // Convenience helpers
    // -------------------------------------------------------------------------

    /** Returns the value of a field in block 4, or null if not found */
    public String getFieldValue(String tag) {
        SwiftField f = block4.getField(tag);
        return f == null ? null : f.getValue();
    }

    /** Adds a field to block 4 */
    public void addField(SwiftField field) {
        block4.addField(field);
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    /** Produces the full SWIFT FIN wire-format string */
    public String toSwiftString() {
        StringBuilder sb = new StringBuilder();
        if (block1 != null && !block1.getRawContent().isBlank()) sb.append(block1.toSwiftString());
        if (block2 != null && !block2.getRawContent().isBlank()) sb.append(block2.toSwiftString());
        if (block3 != null && !block3.getFields().isEmpty())      sb.append(block3.toSwiftString());
        if (block4 != null)                                        sb.append(block4.toSwiftString());
        if (block5 != null && !block5.getRawContent().isBlank())  sb.append(block5.toSwiftString());
        return sb.toString();
    }

    /** Produces a human-readable representation for debugging / display */
    public String toPrettyString() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════════════════════╗\n");
        sb.append(String.format("║  SWIFT MT%s Message%n", messageType));
        sb.append("╠══════════════════════════════════════════════════════════╣\n");
        appendBlockPretty(sb, block1, "Block 1 – Basic Header");
        appendBlockPretty(sb, block2, "Block 2 – Application Header");
        appendBlockPretty(sb, block3, "Block 3 – User Header");
        sb.append("║\n");
        sb.append("║  Block 4 – Text Block\n");
        sb.append("║  " + "─".repeat(56) + "\n");
        for (SwiftField f : block4.getFields()) {
            String line = String.format("  :%s: %s", f.getTag(), f.getValue());
            for (String part : wrapLine(line, 54)) {
                sb.append("║  ").append(part).append("\n");
            }
            if (!f.getDescription().isBlank()) {
                sb.append("║      » ").append(f.getDescription()).append("\n");
            }
        }
        appendBlockPretty(sb, block5, "Block 5 – Trailer");
        sb.append("╚══════════════════════════════════════════════════════════╝\n");
        return sb.toString();
    }

    private void appendBlockPretty(StringBuilder sb, SwiftBlock block, String title) {
        if (block == null) return;
        boolean hasContent = !block.getRawContent().isBlank() || !block.getFields().isEmpty();
        if (!hasContent) return;
        sb.append("║\n");
        sb.append("║  ").append(title).append("\n");
        sb.append("║  " + "─".repeat(56) + "\n");
        if (!block.getRawContent().isBlank()) {
            sb.append("║  ").append(block.getRawContent()).append("\n");
        }
        for (SwiftField f : block.getFields()) {
            sb.append("║  {").append(f.getTag()).append(":").append(f.getValue()).append("}\n");
        }
    }

    private String[] wrapLine(String line, int width) {
        if (line.length() <= width) return new String[]{line};
        java.util.List<String> parts = new java.util.ArrayList<>();
        while (line.length() > width) {
            parts.add(line.substring(0, width));
            line = "    " + line.substring(width);
        }
        parts.add(line);
        return parts.toArray(new String[0]);
    }

    @Override
    public String toString() {
        return "SwiftMessage{type=MT" + messageType + "}";
    }
}
