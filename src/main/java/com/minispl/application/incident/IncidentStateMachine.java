package com.minispl.application.incident;

import com.minispl.application.observer.IncidentEvent;
import com.minispl.application.observer.IncidentEventPublisher;
import com.minispl.domain.enums.IncidentStatus;
import com.minispl.domain.model.Incident;
import com.minispl.persistence.dao.IncidentDAO;

import java.sql.SQLException;
import java.util.List;

/**
 * State Pattern Context: IncidentStateMachine.
 * Wraps an Incident domain entity and delegates lifecycle operations to its current IncidentState.
 * Updates SQLite via IncidentDAO upon each valid transition and notifies observers.
 */
public class IncidentStateMachine {

    private final Incident incident;
    private final IncidentDAO incidentDAO;
    private IncidentState currentState;

    public IncidentStateMachine(Incident incident, IncidentDAO incidentDAO) {
        if (incident == null) {
            throw new IllegalArgumentException("Incident cannot be null");
        }
        this.incident = incident;
        this.incidentDAO = incidentDAO != null ? incidentDAO : new IncidentDAO();
        this.currentState = resolveInitialState(incident.getStatus());
    }

    private IncidentState resolveInitialState(IncidentStatus status) {
        if (status == null) return new NewIncidentState();
        return switch (status) {
            case NEW -> new NewIncidentState();
            case TRIAGED -> new TriagedIncidentState();
            case CONTAINED -> new ContainedIncidentState();
            case CLOSED -> new ClosedIncidentState();
        };
    }

    public Incident getIncident() {
        return incident;
    }

    public IncidentDAO getIncidentDAO() {
        return incidentDAO;
    }

    public IncidentState getCurrentState() {
        return currentState;
    }

    public void setState(IncidentState newState) {
        this.currentState = newState;
    }

    public IncidentStatus getStatus() {
        return currentState.getStatus();
    }

    public List<IncidentStatus> getValidTransitions() {
        return currentState.getValidTransitions();
    }

    public boolean canTransitionTo(IncidentStatus target) {
        return getValidTransitions().contains(target);
    }

    /**
     * Executes transition: NEW -> TRIAGED
     */
    public void triage(Integer playbookId, Integer analystId) throws SQLException {
        IncidentStatus oldStatus = getStatus();
        currentState.triage(this, playbookId, analystId);
        IncidentEventPublisher.getInstance().publish(new IncidentEvent(
                IncidentEvent.EventType.INCIDENT_STATE_CHANGED,
                incident.getId(),
                oldStatus.name(),
                getStatus().name(),
                analystId != null ? analystId : incident.getAssignedAnalystId(),
                "Incident triaged into phase " + incident.getCurrentPhase()
        ));
    }

    /**
     * Executes transition: TRIAGED -> CONTAINED
     */
    public void contain() throws SQLException {
        IncidentStatus oldStatus = getStatus();
        currentState.contain(this);
        IncidentEventPublisher.getInstance().publish(new IncidentEvent(
                IncidentEvent.EventType.INCIDENT_STATE_CHANGED,
                incident.getId(),
                oldStatus.name(),
                getStatus().name(),
                incident.getAssignedAnalystId(),
                "Incident contained into phase " + incident.getCurrentPhase()
        ));
    }

    /**
     * Executes transition: CONTAINED -> CLOSED
     */
    public void close() throws SQLException {
        IncidentStatus oldStatus = getStatus();
        currentState.close(this);
        IncidentEventPublisher.getInstance().publish(new IncidentEvent(
                IncidentEvent.EventType.INCIDENT_STATE_CHANGED,
                incident.getId(),
                oldStatus.name(),
                getStatus().name(),
                incident.getAssignedAnalystId(),
                "Incident closed"
        ));
    }
}
