package com.minispl.application;

import com.minispl.application.strategy.*;
import com.minispl.domain.enums.CriticalityTier;
import com.minispl.domain.enums.IncidentSeverity;
import com.minispl.domain.model.Asset;
import com.minispl.domain.model.Incident;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RiskScoringStrategyTest {

    @Test
    public void testNistRiskScoringStrategy() {
        NistRiskScoringStrategy strategy = new NistRiskScoringStrategy();
        assertEquals("NIST SP 800-61", strategy.getName());

        Asset criticalAsset = new Asset(1, "DC-CORP-01", "10.0.0.1", CriticalityTier.CRITICAL, null, "Domain Controller");
        Incident criticalIncident = new Incident("Ransomware", "RANSOMWARE", IncidentSeverity.CRITICAL, 1, null, null);

        double score = strategy.calculateRiskScore(criticalIncident, criticalAsset);
        // (50 + 40) * 1.15 = 103.5 -> clamped to 100.0
        assertEquals(100.0, score);

        Asset lowAsset = new Asset(2, "DEV-VM", "10.0.0.2", CriticalityTier.LOW, null, "Dev VM");
        Incident lowIncident = new Incident("Phishing probe", "PHISHING", IncidentSeverity.LOW, 2, null, null);

        double lowScore = strategy.calculateRiskScore(lowIncident, lowAsset);
        // (10 + 10) * 1.05 = 21.0
        assertEquals(21.0, lowScore);
    }

    @Test
    public void testCvssRiskScoringStrategy() {
        CvssRiskScoringStrategy strategy = new CvssRiskScoringStrategy();
        assertEquals("CVSS v3.1 Model", strategy.getName());

        Asset asset = new Asset(1, "DB-PROD", "10.0.0.5", CriticalityTier.HIGH, null, "DB");
        Incident incident = new Incident("Data breach", "DATA_EXFILTRATION", IncidentSeverity.HIGH, 1, null, null);

        double score = strategy.calculateRiskScore(incident, asset);
        // impact=0.85*0.6 = 0.51, exploitability=0.85*0.4 = 0.34. sum = 0.85 * 100 = 85.0
        assertEquals(85.0, score);
    }

    @Test
    public void testRiskScoringContextSwitching() {
        RiskScoringContext context = new RiskScoringContext();
        assertTrue(context.getStrategy() instanceof NistRiskScoringStrategy);

        Asset asset = new Asset(1, "WEB-01", "10.0.0.8", CriticalityTier.MEDIUM, null, null);
        Incident inc = new Incident("Alert", "PHISHING", IncidentSeverity.MEDIUM, 1, null, null);

        double nistScore = context.calculateRisk(inc, asset);
        assertTrue(nistScore > 0);

        // Switch strategy dynamically at runtime
        context.setStrategy(new CvssRiskScoringStrategy());
        assertTrue(context.getStrategy() instanceof CvssRiskScoringStrategy);

        double cvssScore = context.calculateRisk(inc, asset);
        assertTrue(cvssScore > 0);
    }
}
