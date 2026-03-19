package com.kazhuga.swift.parser;

/**
 * Thrown when a SWIFT message string cannot be parsed.
 */
public class SwiftParseException extends Exception {

    public SwiftParseException(String message) {
        super(message);
    }

    public SwiftParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
