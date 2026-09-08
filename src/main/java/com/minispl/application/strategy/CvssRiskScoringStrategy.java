package com.minispl.application.strategy;

import com.minispl.domain.enums.CriticalityTier;
import com.minispl.domain.enums.IncidentSeverity;
import com.minispl.domain.model.Asset;
import com.minispl.domain.model.Incident;

/**
 * Concrete Strategy: CVSS v3.1 Inspired Quantitative Scoring.
 * Computes risk based on Base Metrics: Exploitability (Attack Vector, Complexity)
 * and Impact (Confidentiality, Integrity, Availability on affected Asset).
 */
public class CvssRiskScoringStrategy implements RiskScoringStrategy {

    @Override
    public String getName() {
        return "CVSS v3.1 Model";
    }

    @Override
    public String getDescription() {
        return "Common Vulnerability Scoring System v3.1: Risk = Exploitability Subscore × Impact Subscore scaled to 100.";
    }

    @Override
    public double calculateRiskScore(Incident incident, Asset asset) {
        if (incident == null) return 0.0;

        // Base Exploitability metric (0.0 to 1.0)
        double exploitability = switch (incident.getSeverity() != null ? incident.getSeverity() : IncidentSeverity.LOW) {
            case LOW -> 0.40;
            case MEDIUM -> 0.65;
            case HIGH -> 0.85;
            case CRITICAL -> 1.00;
        };

        // Impact subscore (0.0 to 1.0) based on Asset Criticality
        double impact = 0.5;
        if (asset != null && asset.getCriticalityTier() != null) {
            impact = switch (asset.getCriticalityTier()) {
                case LOW -> 0.35;
                case MEDIUM -> 0.60;
                case HIGH -> 0.85;
                case CRITICAL -> 1.00;
            };
        }

        // CVSS calculation scaled to 0-100:
        // Score = (Impact * 0.6 + Exploitability * 0.4) * 100
        double raw = (impact * 0.6 + exploitability * 0.4) * 100.0;
        double clamped = Math.max(0.0, Math.min(100.0, raw));
        return Math.round(clamped * 10.0) / 10.0;
    }
}
