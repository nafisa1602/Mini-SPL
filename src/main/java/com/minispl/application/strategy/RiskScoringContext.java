package com.minispl.application.strategy;

import com.minispl.domain.model.Asset;
import com.minispl.domain.model.Incident;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategy Pattern Context: Holds a reference to the active RiskScoringStrategy
 * and delegates the calculation to the selected strategy.
 */
public class RiskScoringContext {

    private RiskScoringStrategy strategy;
    private final List<RiskScoringStrategy> availableStrategies;

    public RiskScoringContext() {
        this(new NistRiskScoringStrategy());
    }

    public RiskScoringContext(RiskScoringStrategy strategy) {
        this.strategy = strategy != null ? strategy : new NistRiskScoringStrategy();
        this.availableStrategies = new ArrayList<>();
        this.availableStrategies.add(new NistRiskScoringStrategy());
        this.availableStrategies.add(new CvssRiskScoringStrategy());
    }

    public void setStrategy(RiskScoringStrategy strategy) {
        if (strategy != null) {
            this.strategy = strategy;
        }
    }

    public RiskScoringStrategy getStrategy() {
        return this.strategy;
    }

    public List<RiskScoringStrategy> getAvailableStrategies() {
        return this.availableStrategies;
    }

    public double calculateRisk(Incident incident, Asset asset) {
        return strategy.calculateRiskScore(incident, asset);
    }
}
