package com.minispl.domain.enums;

public enum IncidentSeverity {
    LOW(10.0),
    MEDIUM(30.0),
    HIGH(60.0),
    CRITICAL(90.0);

    private final double baseScore;

    IncidentSeverity(double baseScore) {
        this.baseScore = baseScore;
    }

    public double getBaseScore() {
        return baseScore;
    }
}
