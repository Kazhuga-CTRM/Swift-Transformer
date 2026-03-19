package com.kazhuga.swift.parser;

import com.kazhuga.swift.core.SwiftBlock;
import com.kazhuga.swift.core.SwiftField;
import com.kazhuga.swift.core.SwiftMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a SWIFT FIN wire-format string into a {@link SwiftMessage} object.
 *
 * Usage:
 * <pre>
 *   SwiftParser parser = new SwiftParser();
 *   SwiftMessage msg   = parser.parse(rawSwiftString);
 * </pre>
 */
public class SwiftParser {

    private static final Pattern BLOCK4_PATTERN =
            Pattern.compile("\\{4:\\r?\\n(.*?)\\r?\\n-\\}", Pattern.DOTALL);

    private static final Pattern SUBFIELD_PATTERN =
            Pattern.compile("\\{([^:}]+):([^}]*)\\}");

    /**
     * Parses a complete SWIFT FIN message string.
     *
     * @param raw  raw SWIFT wire format string
     * @return     parsed SwiftMessage
     * @throws SwiftParseException if the string cannot be parsed
     */
    public SwiftMessage parse(String raw) throws SwiftParseException {
        if (raw == null || raw.isBlank()) {
            throw new SwiftParseException("Input string is null or blank");
        }

        String       messageType = detectMessageType(raw);
        SwiftMessage msg         = new SwiftMessage(messageType);

        // Block 1
        String b1 = extractBlock(raw, 1);
        if (b1 != null) msg.setBlock1(new SwiftBlock(SwiftBlock.BlockType.BLOCK1, b1));

        // Block 2
        String b2 = extractBlock(raw, 2);
        if (b2 != null) msg.setBlock2(new SwiftBlock(SwiftBlock.BlockType.BLOCK2, b2));

        // Block 3
        String b3 = extractBlock(raw, 3);
        if (b3 != null) {
            SwiftBlock block3 = new SwiftBlock(SwiftBlock.BlockType.BLOCK3);
            Matcher sm = SUBFIELD_PATTERN.matcher(b3);
            while (sm.find()) {
                block3.addField(new SwiftField(sm.group(1), sm.group(2)));
            }
            msg.setBlock3(block3);
        }

        // Block 4
        parseBlock4(raw, msg);

        // Block 5
        String b5 = extractBlock(raw, 5);
        if (b5 != null) msg.setBlock5(new SwiftBlock(SwiftBlock.BlockType.BLOCK5, b5));

        return msg;
    }

    // -------------------------------------------------------------------------

    private String detectMessageType(String raw) {
        String b2 = extractBlock(raw, 2);
        if (b2 == null || b2.length() < 4) return "UNKNOWN";
        if (b2.charAt(0) == 'I' || b2.charAt(0) == 'O') {
            String type = b2.substring(1, Math.min(4, b2.length()));
            if (b2.contains("COV")) return type + "COV";
            return type;
        }
        return "UNKNOWN";
    }

    private String extractBlock(String raw, int blockNumber) {
        if (blockNumber == 4) {
            Matcher m = BLOCK4_PATTERN.matcher(raw);
            return m.find() ? m.group(1) : null;
        }
        Pattern p = Pattern.compile("\\{" + blockNumber + ":(.+?)\\}(?=\\{[12356]:|$)",
                Pattern.DOTALL);
        Matcher m = p.matcher(raw);
        return m.find() ? m.group(1) : null;
    }

    private void parseBlock4(String raw, SwiftMessage msg) throws SwiftParseException {
        Matcher b4m = BLOCK4_PATTERN.matcher(raw);
        if (!b4m.find()) {
            throw new SwiftParseException("Block 4 not found in message");
        }

        String   body       = b4m.group(1).trim();
        SwiftBlock block4   = new SwiftBlock(SwiftBlock.BlockType.BLOCK4);
        String[] lines      = body.split("\\r?\\n");
        StringBuilder currentValue = new StringBuilder();
        String currentTag   = null;

        for (String line : lines) {
            if (line.startsWith(":") && line.length() > 2) {
                int secondColon = line.indexOf(':', 1);
                if (secondColon > 1 && secondColon <= 4) {
                    if (currentTag != null) {
                        block4.addField(new SwiftField(currentTag,
                                currentValue.toString().stripTrailing()));
                    }
                    currentTag   = line.substring(1, secondColon);
                    currentValue = new StringBuilder(line.substring(secondColon + 1));
                    continue;
                }
            }
            if (currentTag != null) {
                currentValue.append("\n").append(line);
            }
        }
        if (currentTag != null) {
            block4.addField(new SwiftField(currentTag,
                    currentValue.toString().stripTrailing()));
        }

        msg.setBlock4(block4);
    }
}
