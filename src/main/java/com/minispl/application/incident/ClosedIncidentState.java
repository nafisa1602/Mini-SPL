package com.minispl.application.incident;

import com.minispl.domain.enums.IncidentStatus;

import java.util.Collections;
import java.util.List;

/**
 * Concrete State: CLOSED incident.
 * Terminal state of the investigation lifecycle.
 * No further transitions permitted.
 */
public class ClosedIncidentState implements IncidentState {

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.CLOSED;
    }

    @Override
    public String getDisplayName() {
        return "Closed (Investigation Concluded)";
    }

    @Override
    public void triage(IncidentStateMachine context, Integer playbookId, Integer analystId) {
        throw new IllegalStateException("Incident is CLOSED. No transitions allowed on a closed incident.");
    }

    @Override
    public void contain(IncidentStateMachine context) {
        throw new IllegalStateException("Incident is CLOSED. No transitions allowed on a closed incident.");
    }

    @Override
    public void close(IncidentStateMachine context) {
        throw new IllegalStateException("Incident is already CLOSED.");
    }

    @Override
    public List<IncidentStatus> getValidTransitions() {
        return Collections.emptyList();
    }
}
