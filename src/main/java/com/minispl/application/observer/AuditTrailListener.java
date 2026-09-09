package com.minispl.application.observer;

import com.minispl.domain.enums.AuditStatus;
import com.minispl.domain.model.ActionAuditLog;
import com.minispl.persistence.dao.AuditLogDAO;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Concrete Observer Pattern implementation: AuditTrailListener.
 * Subscribes to IncidentEventPublisher and persists incident lifecycle state transitions
 * and evidence custody transitions into the SQLite action_audit_logs table.
 * Closes the architectural gap where previously only Command executions were audited.
 */
public class AuditTrailListener implements IncidentEventListener {

    private static final Logger LOGGER = Logger.getLogger(AuditTrailListener.class.getName());

    private final AuditLogDAO auditLogDAO;

    public AuditTrailListener() {
        this(new AuditLogDAO());
    }

    public AuditTrailListener(AuditLogDAO auditLogDAO) {
        this.auditLogDAO = auditLogDAO != null ? auditLogDAO : new AuditLogDAO();
    }

    public static synchronized AuditTrailListener register() {
        for (IncidentEventListener l : IncidentEventPublisher.getInstance().getListeners()) {
            if (l instanceof AuditTrailListener atl) {
                return atl;
            }
        }
        AuditTrailListener listener = new AuditTrailListener();
        IncidentEventPublisher.getInstance().subscribe(listener);
        return listener;
    }

    public static synchronized AuditTrailListener register(AuditLogDAO auditLogDAO) {
        for (IncidentEventListener l : IncidentEventPublisher.getInstance().getListeners()) {
            if (l instanceof AuditTrailListener atl) {
                return atl;
            }
        }
        AuditTrailListener listener = new AuditTrailListener(auditLogDAO);
        IncidentEventPublisher.getInstance().subscribe(listener);
        return listener;
    }

    @Override
    public void onIncidentEvent(IncidentEvent event) {
        if (event == null || event.getIncidentId() <= 0) {
            return;
        }

        try {
            switch (event.getType()) {
                case INCIDENT_STATE_CHANGED -> {
                    String params = "{\"entity\":\"INCIDENT\",\"from\":\"" + sanitize(event.getPreviousState()) +
                            "\",\"to\":\"" + sanitize(event.getNewState()) +
                            "\",\"details\":\"" + sanitize(event.getDetails()) + "\"}";
                    int userId = (event.getUserId() != null && event.getUserId() > 0) ? event.getUserId() : 1;
                    ActionAuditLog log = new ActionAuditLog(
                            event.getIncidentId(),
                            "STATE_TRANSITION",
                            params,
                            userId,
                            false,
                            AuditStatus.EXECUTED
                    );
                    auditLogDAO.create(log);
                }

                case EVIDENCE_CUSTODY_CHANGED -> {
                    String params = "{\"entity\":\"EVIDENCE\",\"from\":\"" + sanitize(event.getPreviousState()) +
                            "\",\"to\":\"" + sanitize(event.getNewState()) +
                            "\",\"details\":\"" + sanitize(event.getDetails()) + "\"}";
                    int userId = (event.getUserId() != null && event.getUserId() > 0) ? event.getUserId() : 1;
                    ActionAuditLog log = new ActionAuditLog(
                            event.getIncidentId(),
                            "CUSTODY_TRANSITION",
                            params,
                            userId,
                            false,
                            AuditStatus.EXECUTED
                    );
                    auditLogDAO.create(log);
                }

                case PLAYBOOK_EXECUTED -> {
                    String params = "{\"entity\":\"PLAYBOOK\",\"details\":\"" + sanitize(event.getDetails()) + "\"}";
                    int userId = (event.getUserId() != null && event.getUserId() > 0) ? event.getUserId() : 1;
                    ActionAuditLog log = new ActionAuditLog(
                            event.getIncidentId(),
                            "PLAYBOOK_WORKFLOW",
                            params,
                            userId,
                            false,
                            AuditStatus.EXECUTED
                    );
                    auditLogDAO.create(log);
                }

                default -> {
                    // Command executions/rollbacks are already logged directly by CommandInvoker
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to persist state transition to audit trail: " + e.getMessage(), e);
        }
    }

    private String sanitize(String value) {
        if (value == null) return "";
        return value.replace("\"", "'").replace("\n", " ").replace("\r", "");
    }

    public AuditLogDAO getAuditLogDAO() {
        return auditLogDAO;
    }
}
