package com.minispl.application.observer;

import java.time.LocalDateTime;

/**
 * Event object representing state transitions, command executions, and lifecycle mutations
 * across the DFIR Orchestrator system. Dispatched via IncidentEventPublisher.
 */
public class IncidentEvent {

    public enum EventType {
        INCIDENT_STATE_CHANGED,
        EVIDENCE_CUSTODY_CHANGED,
        COMMAND_EXECUTED,
        COMMAND_UNDONE,
        INCIDENT_CREATED,
        INCIDENT_DELETED,
        EVIDENCE_CREATED,
        EVIDENCE_DELETED,
        PLAYBOOK_EXECUTED
    }

    private final EventType type;
    private final int incidentId;
    private final String previousState;
    private final String newState;
    private final Integer userId;
    private final String details;
    private final LocalDateTime timestamp;

    public IncidentEvent(EventType type, int incidentId, String previousState, String newState, Integer userId, String details) {
        this.type = type;
        this.incidentId = incidentId;
        this.previousState = previousState;
        this.newState = newState;
        this.userId = userId;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public IncidentEvent(EventType type, int incidentId, String previousState, String newState, Integer userId) {
        this(type, incidentId, previousState, newState, userId, null);
    }

    public IncidentEvent(EventType type, int incidentId, String details) {
        this(type, incidentId, null, null, null, details);
    }

    public EventType getType() {
        return type;
    }

    public int getIncidentId() {
        return incidentId;
    }

    public String getPreviousState() {
        return previousState;
    }

    public String getOldValue() {
        return previousState;
    }

    public String getNewState() {
        return newState;
    }

    public String getNewValue() {
        return newState;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getDetails() {
        return details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "IncidentEvent{" +
                "type=" + type +
                ", incidentId=" + incidentId +
                ", previousState='" + previousState + "'" +
                ", newState='" + newState + "'" +
                ", userId=" + userId +
                ", details='" + details + "'" +
                ", timestamp=" + timestamp +
                "}";
    }
}
