package com.minispl.application.strategy;

import com.minispl.domain.model.Asset;
import com.minispl.domain.model.Incident;

/**
 * Strategy Pattern Interface for Risk Scoring.
 * Allows interchangeable scoring algorithms (e.g. NIST SP 800-61 vs CVSS v3.1)
 * without altering incident triage and response workflows.
 */
public interface RiskScoringStrategy {

    /**
     * Unique identifier / display name for the strategy.
     */
    String getName();

    /**
     * Human-readable description of how the risk score is evaluated.
     */
    String getDescription();

    /**
     * Computes the normalized risk score (0.0 to 100.0).
     *
     * @param incident The incident requiring risk assessment
     * @param asset The target system/asset affected by the incident
     * @return Calculated risk score between 0.0 and 100.0
     */
    double calculateRiskScore(Incident incident, Asset asset);
}
