package com.minispl.application.incident;

import com.minispl.domain.enums.IncidentStatus;
import com.minispl.domain.enums.PlaybookPhase;
import com.minispl.domain.model.Incident;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Concrete State: CONTAINED incident.
 * Threat vectors are neutralized and evidence collection / eradication is underway.
 * Valid transition: -> CLOSED.
 */
public class ContainedIncidentState implements IncidentState {

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.CONTAINED;
    }

    @Override
    public String getDisplayName() {
        return "Contained (Ready for Post-Mortem & Closure)";
    }

    @Override
    public void triage(IncidentStateMachine context, Integer playbookId, Integer analystId) {
        throw new IllegalStateException("Incident is already contained and cannot transition back to TRIAGED.");
    }

    @Override
    public void contain(IncidentStateMachine context) {
        throw new IllegalStateException("Incident is already in CONTAINED state.");
    }

    @Override
    public void close(IncidentStateMachine context) throws SQLException {
        Incident incident = context.getIncident();
        incident.setStatus(IncidentStatus.CLOSED);
        incident.setCurrentPhase(PlaybookPhase.POST_MORTEM);
        incident.setClosedAt(LocalDateTime.now());

        // Update database
        context.getIncidentDAO().closeIncident(incident.getId());

        // Transition context state
        context.setState(new ClosedIncidentState());
    }

    @Override
    public List<IncidentStatus> getValidTransitions() {
        return List.of(IncidentStatus.CLOSED);
    }
}
