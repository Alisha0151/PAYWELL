package com.paywell.payment;

import java.util.List;

/** Result of a pre-payment risk check. {@code source} says who produced it so the UI never overstates certainty. */
public record RiskAssessment(int score, String category, String action, List<Factor> factors, String source, String disclaimer) {
    public record Factor(String code, int points, String explanation) {}

    public static final String DISCLAIMER = "Demonstration risk rules, not a validated fraud model. No guarantee of fraud prevention.";

    public boolean blocks() { return "BLOCK".equals(action); }

    public boolean needsStepUp() { return "CHALLENGE".equals(action) || "REVIEW".equals(action); }
}
