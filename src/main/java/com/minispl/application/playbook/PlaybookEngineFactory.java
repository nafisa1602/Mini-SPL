package com.minispl.application.playbook;

import com.minispl.persistence.dao.IncidentDAO;

/**
 * Factory for creating the appropriate InvestigationEngine subclass based on incident threat type.
 */
public class PlaybookEngineFactory {

    public static InvestigationEngine getEngine(String threatType) {
        return getEngine(threatType, new IncidentDAO());
    }

    public static InvestigationEngine getEngine(String threatType, IncidentDAO incidentDAO) {
        if (threatType == null) {
            return new RansomwarePlaybookEngine(incidentDAO);
        }

        return switch (threatType.trim().toUpperCase()) {
            case "RANSOMWARE" -> new RansomwarePlaybookEngine(incidentDAO);
            case "PHISHING" -> new PhishingPlaybookEngine(incidentDAO);
            default -> new RansomwarePlaybookEngine(incidentDAO);
        };
    }
}
