package com.minispl.application.remediation;

import com.minispl.domain.enums.AuditStatus;
import com.minispl.domain.model.ActionAuditLog;
import com.minispl.persistence.dao.AuditLogDAO;

import java.sql.SQLException;
import java.util.*;

/**
 * Command Pattern Invoker: CommandInvoker.
 * Orchestrates execution, rollback/undo, in-memory command history,
 * and persistent logging into SQLite via AuditLogDAO.
 */
public class CommandInvoker {

    private final AuditLogDAO auditLogDAO;
    private final RemediationCommandFactory commandFactory;
    private final Deque<RemediationCommand> undoStack = new ArrayDeque<>();
    private final Map<Integer, RemediationCommand> activeCommandsByAuditId = new HashMap<>();

    public CommandInvoker() {
        this(new AuditLogDAO(), new RemediationCommandFactory());
    }

    public CommandInvoker(AuditLogDAO auditLogDAO, RemediationCommandFactory commandFactory) {
        this.auditLogDAO = auditLogDAO != null ? auditLogDAO : new AuditLogDAO();
        this.commandFactory = commandFactory != null ? commandFactory : new RemediationCommandFactory();
    }

    /**
     * Executes a command and persists an audit entry to action_audit_logs.
     */
    public ActionAuditLog execute(RemediationCommand command) throws Exception {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }

        try {
            // 1. Execute the remediation logic
            command.execute();

            // 2. Persist audit log
            ActionAuditLog auditLog = new ActionAuditLog(
                    command.getIncidentId(),
                    command.getCommandType(),
                    command.getParameters(),
                    command.getExecutedById(),
                    command.canUndo(),
                    AuditStatus.EXECUTED
            );

            ActionAuditLog createdLog = auditLogDAO.create(auditLog);
            command.setAuditLogId(createdLog.getId());

            // 3. Track in undo stack if eligible
            if (command.canUndo()) {
                undoStack.push(command);
                activeCommandsByAuditId.put(createdLog.getId(), command);
            }

            return createdLog;
        } catch (Exception e) {
            // Log failure to database
            try {
                ActionAuditLog failedLog = new ActionAuditLog(
                        command.getIncidentId(),
                        command.getCommandType(),
                        command.getParameters(),
                        command.getExecutedById(),
                        false,
                        AuditStatus.FAILED
                );
                auditLogDAO.create(failedLog);
            } catch (SQLException ignored) {}
            throw e;
        }
    }

    /**
     * Undoes / rolls back the most recent reversible command.
     */
    public RemediationCommand undoLast() throws Exception {
        if (undoStack.isEmpty()) {
            throw new IllegalStateException("No reversible actions in undo stack.");
        }

        RemediationCommand cmd = undoStack.pop();
        rollbackCommand(cmd);
        return cmd;
    }

    /**
     * Undoes a specific command associated with an ActionAuditLog ID.
     */
    public boolean undoByAuditLogId(int auditLogId) throws Exception {
        RemediationCommand command = activeCommandsByAuditId.get(auditLogId);
        if (command != null) {
            rollbackCommand(command);
            undoStack.remove(command);
            activeCommandsByAuditId.remove(auditLogId);
            return true;
        }

        // If not found in memory (e.g. from previous app run), reconstruct from database record
        Optional<ActionAuditLog> opt = auditLogDAO.findById(auditLogId);
        if (opt.isPresent()) {
            ActionAuditLog log = opt.get();
            if (!log.isCanUndo() || log.getStatus() != AuditStatus.EXECUTED) {
                throw new IllegalStateException("Action is not eligible for undo (status: " + log.getStatus() + ")");
            }

            // Parse parameters into map and reconstruct command
            Map<String, String> paramMap = parseJsonParams(log.getParameters());
            RemediationCommand reconstructed = commandFactory.createCommand(
                    log.getCommandType(),
                    log.getIncidentId(),
                    log.getExecutedById(),
                    paramMap
            );
            reconstructed.setAuditLogId(log.getId());
            // Mark as executed so undo is allowed
            if (reconstructed instanceof AbstractRemediationCommand arc) {
                arc.executed = true;
            }

            rollbackCommand(reconstructed);
            return true;
        }

        return false;
    }

    private void rollbackCommand(RemediationCommand cmd) throws Exception {
        cmd.undo();

        if (cmd.getAuditLogId() != null) {
            auditLogDAO.updateStatus(cmd.getAuditLogId(), AuditStatus.UNDONE, false);
        }
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public List<RemediationCommand> getUndoableCommands() {
        return new ArrayList<>(undoStack);
    }

    private Map<String, String> parseJsonParams(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.isBlank()) return map;

        String clean = json.replace("{", "").replace("}", "").trim();
        String[] pairs = clean.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();
                map.put(key, val);
            }
        }
        return map;
    }
}
