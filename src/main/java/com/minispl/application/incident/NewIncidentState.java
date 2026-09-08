package com.minispl.application.incident;

import com.minispl.domain.enums.IncidentStatus;
import com.minispl.domain.enums.PlaybookPhase;
import com.minispl.domain.model.Incident;

import java.sql.SQLException;
import java.util.List;

/**
 * Concrete State: NEW incident alert.
 * A newly created incident must undergo triage and initial scoping.
 * Valid transition: -> TRIAGED.
 * Direct jumps to CONTAINED or CLOSED are prohibited.
 */
public class NewIncidentState implements IncidentState {

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.NEW;
    }

    @Override
    public String getDisplayName() {
        return "New (Unprocessed Alert)";
    }

    @Override
    public void triage(IncidentStateMachine context, Integer playbookId, Integer analystId) throws SQLException {
        Incident incident = context.getIncident();
        incident.setStatus(IncidentStatus.TRIAGED);
        incident.setCurrentPhase(PlaybookPhase.CONTAINMENT);
        if (playbookId != null) incident.setPlaybookId(playbookId);
        if (analystId != null) incident.setAssignedAnalystId(analystId);

        // Update database
        context.getIncidentDAO().updateStatus(incident.getId(), IncidentStatus.TRIAGED);
        context.getIncidentDAO().updatePhase(incident.getId(), PlaybookPhase.CONTAINMENT);
        if (analystId != null) {
            context.getIncidentDAO().assignAnalyst(incident.getId(), analystId);
        }

        // Transition context state
        context.setState(new TriagedIncidentState());
    }

    @Override
    public void contain(IncidentStateMachine context) {
        throw new IllegalStateException("Invalid Transition: Cannot contain a NEW incident directly. The alert must be TRIAGED first.");
    }

    @Override
    public void close(IncidentStateMachine context) {
        throw new IllegalStateException("Invalid Transition: Cannot close a NEW incident directly. Incident must be Triaged and Contained before closure.");
    }

    @Override
    public List<IncidentStatus> getValidTransitions() {
        return List.of(IncidentStatus.TRIAGED);
    }
}
