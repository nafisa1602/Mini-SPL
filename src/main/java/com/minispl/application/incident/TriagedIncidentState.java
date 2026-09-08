package com.minispl.application.incident;

import com.minispl.domain.enums.IncidentStatus;
import com.minispl.domain.enums.PlaybookPhase;
import com.minispl.domain.model.Incident;

import java.sql.SQLException;
import java.util.List;

/**
 * Concrete State: TRIAGED incident.
 * The incident scope, playbook, and lead handler have been assigned.
 * Active containment steps (e.g. host isolation, firewall rules) are executing.
 * Valid transition: -> CONTAINED.
 * Direct jump to CLOSED is prohibited to preserve SOC investigation integrity.
 */
public class TriagedIncidentState implements IncidentState {

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.TRIAGED;
    }

    @Override
    public String getDisplayName() {
        return "Triaged (Under Containment)";
    }

    @Override
    public void triage(IncidentStateMachine context, Integer playbookId, Integer analystId) {
        throw new IllegalStateException("Incident is already in TRIAGED state.");
    }

    @Override
    public void contain(IncidentStateMachine context) throws SQLException {
        Incident incident = context.getIncident();
        incident.setStatus(IncidentStatus.CONTAINED);
        incident.setCurrentPhase(PlaybookPhase.EVIDENCE_COLLECTION);

        // Update database
        context.getIncidentDAO().updateStatus(incident.getId(), IncidentStatus.CONTAINED);
        context.getIncidentDAO().updatePhase(incident.getId(), PlaybookPhase.EVIDENCE_COLLECTION);

        // Transition context state
        context.setState(new ContainedIncidentState());
    }

    @Override
    public void close(IncidentStateMachine context) {
        throw new IllegalStateException("Invalid Transition: Cannot close a TRIAGED incident. Containment verification is mandatory before closure.");
    }

    @Override
    public List<IncidentStatus> getValidTransitions() {
        return List.of(IncidentStatus.CONTAINED);
    }
}
