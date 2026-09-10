package com.minispl.application.playbook;

import com.minispl.persistence.dao.IncidentDAO;

/**
 * Concrete Phishing Playbook implementing the Template Method pattern lifecycle.
 * Alias subclass conforming to proposal naming table.
 */
public class PhishingPlaybook extends PhishingPlaybookEngine {

    public PhishingPlaybook() {
        super();
    }

    public PhishingPlaybook(IncidentDAO incidentDAO) {
        super(incidentDAO);
    }
}
