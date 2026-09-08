package com.minispl.application.remediation;

/**
 * Abstract base class providing common properties and plumbing
 * for concrete RemediationCommands.
 */
public abstract class AbstractRemediationCommand implements RemediationCommand {

    protected final int incidentId;
    protected final int executedById;
    protected Integer auditLogId;
    protected String statusMessage = "";
    protected boolean executed = false;
    protected boolean undone = false;

    public AbstractRemediationCommand(int incidentId, int executedById) {
        this.incidentId = incidentId;
        this.executedById = executedById;
    }

    @Override
    public int getIncidentId() {
        return incidentId;
    }

    @Override
    public int getExecutedById() {
        return executedById;
    }

    @Override
    public Integer getAuditLogId() {
        return auditLogId;
    }

    @Override
    public void setAuditLogId(Integer auditLogId) {
        this.auditLogId = auditLogId;
    }

    @Override
    public String getStatusMessage() {
        return statusMessage;
    }

    public boolean isExecuted() {
        return executed;
    }

    public boolean isUndone() {
        return undone;
    }
}
