package com.minispl.application.strategy;

import com.minispl.domain.enums.CriticalityTier;
import com.minispl.domain.enums.IncidentSeverity;
import com.minispl.domain.model.Asset;
import com.minispl.domain.model.Incident;

/**
 * Concrete Strategy: NIST SP 800-61 Rev 2 Computer Security Incident Handling Guide.
 * Computes risk based on functional impact (Asset Criticality), information impact (Severity),
 * and threat vector exposure.
 */
public class NistRiskScoringStrategy implements RiskScoringStrategy {

    @Override
    public String getName() {
        return "NIST SP 800-61";
    }

    @Override
    public String getDescription() {
        return "NIST Special Publication 800-61 standard: Risk = (Asset Weight + Severity Weight) × Threat Multiplier.";
    }

    @Override
    public double calculateRiskScore(Incident incident, Asset asset) {
        if (incident == null) return 0.0;

        // 1. Asset Criticality Weight (0 - 50)
        double assetWeight = 10.0;
        if (asset != null && asset.getCriticalityTier() != null) {
            assetWeight = switch (asset.getCriticalityTier()) {
                case LOW -> 10.0;
                case MEDIUM -> 25.0;
                case HIGH -> 40.0;
                case CRITICAL -> 50.0;
            };
        }

        // 2. Incident Severity Weight (0 - 40)
        double severityWeight = switch (incident.getSeverity() != null ? incident.getSeverity() : IncidentSeverity.LOW) {
            case LOW -> 10.0;
            case MEDIUM -> 20.0;
            case HIGH -> 30.0;
            case CRITICAL -> 40.0;
        };

        // 3. Threat Vector Multiplier (0.9 - 1.25)
        double threatMultiplier = 1.0;
        String threat = incident.getThreatType() != null ? incident.getThreatType().toUpperCase() : "";
        if (threat.contains("RANSOMWARE") || threat.contains("ROOTKIT")) {
            threatMultiplier = 1.15;
        } else if (threat.contains("PHISHING") || threat.contains("HARVEST")) {
            threatMultiplier = 1.05;
        } else if (threat.contains("DOS") || threat.contains("DDOS")) {
            threatMultiplier = 0.95;
        }

        double rawScore = (assetWeight + severityWeight) * threatMultiplier;
        // Clamp between 0.0 and 100.0, rounded to 1 decimal place
        double clamped = Math.max(0.0, Math.min(100.0, rawScore));
        return Math.round(clamped * 10.0) / 10.0;
    }
}
