package com.minispl.domain.enums;

public enum CriticalityTier {
    LOW(1.0),
    MEDIUM(1.5),
    HIGH(2.0),
    CRITICAL(3.0);

    private final double weight;

    CriticalityTier(double weight) {
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }
}
