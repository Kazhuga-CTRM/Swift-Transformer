package com.kazhuga.swift.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the result of validating a SWIFT message or input data object.
 */
public class ValidationResult {

    public enum Severity { ERROR, WARNING, INFO }

    public static class Issue {
        public final Severity severity;
        public final String   field;
        public final String   message;

        public Issue(Severity severity, String field, String message) {
            this.severity = severity;
            this.field    = field;
            this.message  = message;
        }

        @Override
        public String toString() {
            return String.format("[%s] Field %s: %s", severity, field, message);
        }
    }

    private final List<Issue> issues = new ArrayList<>();

    public void addError(String field, String message) {
        issues.add(new Issue(Severity.ERROR, field, message));
    }

    public void addWarning(String field, String message) {
        issues.add(new Issue(Severity.WARNING, field, message));
    }

    public void addInfo(String field, String message) {
        issues.add(new Issue(Severity.INFO, field, message));
    }

    public List<Issue> getIssues()   { return Collections.unmodifiableList(issues); }

    public boolean isValid()         { return issues.stream().noneMatch(i -> i.severity == Severity.ERROR); }

    public boolean hasWarnings()     { return issues.stream().anyMatch(i -> i.severity == Severity.WARNING); }

    public long errorCount()         { return issues.stream().filter(i -> i.severity == Severity.ERROR).count(); }

    public long warningCount()       { return issues.stream().filter(i -> i.severity == Severity.WARNING).count(); }

    @Override
    public String toString() {
        if (issues.isEmpty()) return "ValidationResult{PASS – no issues}";
        StringBuilder sb = new StringBuilder();
        sb.append("ValidationResult{").append(isValid() ? "PASS" : "FAIL");
        sb.append(", errors=").append(errorCount());
        sb.append(", warnings=").append(warningCount()).append("}\n");
        for (Issue i : issues) sb.append("  ").append(i).append("\n");
        return sb.toString().trim();
    }
}
