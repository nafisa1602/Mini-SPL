package com.minispl.persistence.dao;

import com.minispl.domain.enums.AuditStatus;
import com.minispl.domain.model.ActionAuditLog;
import com.minispl.persistence.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuditLogDAO {

    private final DatabaseManager dbManager;

    public AuditLogDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public AuditLogDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    private static final String BASE_SELECT =
            "SELECT l.id, l.incident_id, l.command_type, l.parameters, l.executed_by_id, " +
            "       l.timestamp, l.can_undo, l.status, " +
            "       u.full_name AS executor_name " +
            "FROM action_audit_logs l " +
            "JOIN users u ON l.executed_by_id = u.id ";

    public ActionAuditLog create(ActionAuditLog log) throws SQLException {
        String sql = "INSERT INTO action_audit_logs (incident_id, command_type, parameters, " +
                     "executed_by_id, can_undo, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, log.getIncidentId());
            stmt.setString(2, log.getCommandType());
            stmt.setString(3, log.getParameters());
            stmt.setInt(4, log.getExecutedById());
            stmt.setInt(5, log.isCanUndo() ? 1 : 0);
            stmt.setString(6, log.getStatus().name());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    log.setId(rs.getInt(1));
                }
            }
        }
        return findById(log.getId()).orElse(log);
    }

    public Optional<ActionAuditLog> findById(int id) throws SQLException {
        String sql = BASE_SELECT + "WHERE l.id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAuditLog(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<ActionAuditLog> findAll() throws SQLException {
        List<ActionAuditLog> list = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY l.id DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToAuditLog(rs));
            }
        }
        return list;
    }

    public List<ActionAuditLog> findByIncidentId(int incidentId) throws SQLException {
        List<ActionAuditLog> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE l.incident_id = ? ORDER BY l.id DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, incidentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAuditLog(rs));
                }
            }
        }
        return list;
    }

    public boolean updateStatus(int id, AuditStatus status, boolean canUndo) throws SQLException {
        String sql = "UPDATE action_audit_logs SET status = ?, can_undo = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setInt(2, canUndo ? 1 : 0);
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM action_audit_logs";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private ActionAuditLog mapResultSetToAuditLog(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int incidentId = rs.getInt("incident_id");
        String commandType = rs.getString("command_type");
        String parameters = rs.getString("parameters");
        int executedById = rs.getInt("executed_by_id");
        LocalDateTime timestamp = parseTimestamp(rs.getString("timestamp"));
        boolean canUndo = rs.getInt("can_undo") == 1;
        AuditStatus status = AuditStatus.valueOf(rs.getString("status"));

        ActionAuditLog log = new ActionAuditLog(id, incidentId, commandType, parameters, executedById, timestamp, canUndo, status);
        log.setExecutorName(rs.getString("executor_name"));
        return log;
    }

    private LocalDateTime parseTimestamp(String ts) {
        if (ts == null) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(ts.replace(" ", "T"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
