package com.minispl.application.playbook;

import com.minispl.persistence.dao.IncidentDAO;

/**
 * Concrete Ransomware Playbook implementing the Template Method pattern lifecycle.
 * Alias subclass conforming to proposal naming table.
 */
public class RansomwarePlaybook extends RansomwarePlaybookEngine {

    public RansomwarePlaybook() {
        super();
    }

    public RansomwarePlaybook(IncidentDAO incidentDAO) {
        super(incidentDAO);
    }
}
