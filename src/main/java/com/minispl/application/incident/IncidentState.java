package com.minispl.application.incident;

import com.minispl.domain.enums.IncidentStatus;

import java.sql.SQLException;
import java.util.List;

/**
 * State Pattern Interface for Incident Lifecycle.
 * Enforces valid state transitions (NEW -> TRIAGED -> CONTAINED -> CLOSED)
 * and eliminates fragile conditional logic.
 */
public interface IncidentState {

    /**
     * The associated enum status for this state.
     */
    IncidentStatus getStatus();

    /**
     * Human-friendly label for display in UI.
     */
    String getDisplayName();

    /**
     * Transitions incident from NEW to TRIAGED.
     */
    void triage(IncidentStateMachine context, Integer playbookId, Integer analystId) throws SQLException;

    /**
     * Transitions incident from TRIAGED to CONTAINED.
     */
    void contain(IncidentStateMachine context) throws SQLException;

    /**
     * Transitions incident from CONTAINED to CLOSED.
     */
    void close(IncidentStateMachine context) throws SQLException;

    /**
     * Returns the list of valid next statuses permitted from this state.
     */
    List<IncidentStatus> getValidTransitions();
}
