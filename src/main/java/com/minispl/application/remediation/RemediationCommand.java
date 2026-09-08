package com.minispl.application.remediation;

import java.sql.SQLException;

/**
 * Command Pattern Interface for Remediation Actions.
 * Encapsulates containment and mitigation operations into executable,
 * undoable, and auditable objects.
 */
public interface RemediationCommand {

    /**
     * Executes the remediation action.
     */
    void execute() throws Exception;

    /**
     * Reverts / undoes the remediation action if rollback is possible.
     */
    void undo() throws Exception;

    /**
     * Indicates whether this command supports reversal/rollback.
     */
    boolean canUndo();

    /**
     * Identifying name of the command (e.g. ISOLATE_HOST, BLOCK_IP).
     */
    String getCommandType();

    /**
     * Target incident ID.
     */
    int getIncidentId();

    /**
     * User ID of the analyst executing this command.
     */
    int getExecutedById();

    /**
     * Serialized parameters in JSON-compatible or human-readable format.
     */
    String getParameters();

    /**
     * Description or outcome message of the execution/undo.
     */
    String getStatusMessage();

    /**
     * Optional ID of the created ActionAuditLog record in SQLite.
     */
    Integer getAuditLogId();
    void setAuditLogId(Integer auditLogId);
}
