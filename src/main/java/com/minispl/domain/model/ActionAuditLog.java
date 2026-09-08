package com.minispl.domain.model;

import com.minispl.domain.enums.AuditStatus;
import java.time.LocalDateTime;

public class ActionAuditLog {
    private int id;
    private int incidentId;
    private String commandType;
    private String parameters;
    private int executedById;
    private LocalDateTime timestamp;
    private boolean canUndo;
    private AuditStatus status;

    // Presentation helper
    private String executorName;

    public ActionAuditLog() {}

    public ActionAuditLog(int id, int incidentId, String commandType, String parameters,
                          int executedById, LocalDateTime timestamp, boolean canUndo, AuditStatus status) {
        this.id = id;
        this.incidentId = incidentId;
        this.commandType = commandType;
        this.parameters = parameters;
        this.executedById = executedById;
        this.timestamp = timestamp;
        this.canUndo = canUndo;
        this.status = status;
    }

    public ActionAuditLog(int incidentId, String commandType, String parameters,
                          int executedById, boolean canUndo, AuditStatus status) {
        this.incidentId = incidentId;
        this.commandType = commandType;
        this.parameters = parameters;
        this.executedById = executedById;
        this.timestamp = LocalDateTime.now();
        this.canUndo = canUndo;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(int incidentId) {
        this.incidentId = incidentId;
    }

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public int getExecutedById() {
        return executedById;
    }

    public void setExecutedById(int executedById) {
        this.executedById = executedById;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isCanUndo() {
        return canUndo;
    }

    public void setCanUndo(boolean canUndo) {
        this.canUndo = canUndo;
    }

    public AuditStatus getStatus() {
        return status;
    }

    public void setStatus(AuditStatus status) {
        this.status = status;
    }

    public String getExecutorName() {
        return executorName;
    }

    public void setExecutorName(String executorName) {
        this.executorName = executorName;
    }

    @Override
    public String toString() {
        return "Log #" + id + ": " + commandType + " [" + status + "]";
    }
}
