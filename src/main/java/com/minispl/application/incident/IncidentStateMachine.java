package com.minispl.application.incident;

import com.minispl.domain.enums.IncidentStatus;
import com.minispl.domain.model.Incident;
import com.minispl.persistence.dao.IncidentDAO;

import java.sql.SQLException;
import java.util.List;

/**
 * State Pattern Context: IncidentStateMachine.
 * Wraps an Incident domain entity and delegates lifecycle operations to its current IncidentState.
 * Updates SQLite via IncidentDAO upon each valid transition.
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
        currentState.triage(this, playbookId, analystId);
    }

    /**
     * Executes transition: TRIAGED -> CONTAINED
     */
    public void contain() throws SQLException {
        currentState.contain(this);
    }

    /**
     * Executes transition: CONTAINED -> CLOSED
     */
    public void close() throws SQLException {
        currentState.close(this);
    }
}
