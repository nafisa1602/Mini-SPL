package com.minispl.persistence.dao;

import com.minispl.domain.enums.IncidentSeverity;
import com.minispl.domain.enums.IncidentStatus;
import com.minispl.domain.enums.PlaybookPhase;
import com.minispl.domain.model.Incident;
import com.minispl.persistence.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IncidentDAO {

    private final DatabaseManager dbManager;

    public IncidentDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public IncidentDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    private static final String BASE_SELECT =
            "SELECT i.id, i.title, i.threat_type, i.severity, i.status, i.asset_id, " +
            "       i.assigned_analyst_id, i.playbook_id, i.risk_score, i.current_phase, " +
            "       i.created_at, i.closed_at, " +
            "       a.hostname AS asset_hostname, " +
            "       u.full_name AS analyst_name, " +
            "       p.threat_type AS playbook_name " +
            "FROM incidents i " +
            "JOIN assets a ON i.asset_id = a.id " +
            "LEFT JOIN users u ON i.assigned_analyst_id = u.id " +
            "LEFT JOIN playbooks p ON i.playbook_id = p.id ";

    public Incident create(Incident inc) throws SQLException {
        String sql = "INSERT INTO incidents (title, threat_type, severity, status, asset_id, " +
                     "assigned_analyst_id, playbook_id, risk_score, current_phase) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, inc.getTitle());
            stmt.setString(2, inc.getThreatType());
            stmt.setString(3, inc.getSeverity().name());
            stmt.setString(4, inc.getStatus().name());
            stmt.setInt(5, inc.getAssetId());

            if (inc.getAssignedAnalystId() != null) {
                stmt.setInt(6, inc.getAssignedAnalystId());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }

            if (inc.getPlaybookId() != null) {
                stmt.setInt(7, inc.getPlaybookId());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }

            stmt.setDouble(8, inc.getRiskScore());
            stmt.setString(9, inc.getCurrentPhase().name());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    inc.setId(rs.getInt(1));
                }
            }
        }
        return findById(inc.getId()).orElse(inc);
    }

    public Optional<Incident> findById(int id) throws SQLException {
        String sql = BASE_SELECT + "WHERE i.id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToIncident(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Incident> findAll() throws SQLException {
        List<Incident> list = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY i.id DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToIncident(rs));
            }
        }
        return list;
    }

    public List<Incident> findByStatus(IncidentStatus status) throws SQLException {
        List<Incident> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE i.status = ? ORDER BY i.id DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToIncident(rs));
                }
            }
        }
        return list;
    }

    public boolean updateStatus(int id, IncidentStatus status) throws SQLException {
        String sql = "UPDATE incidents SET status = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updatePhase(int id, PlaybookPhase phase) throws SQLException {
        String sql = "UPDATE incidents SET current_phase = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phase.name());
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateRiskScore(int id, double riskScore) throws SQLException {
        String sql = "UPDATE incidents SET risk_score = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, riskScore);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean assignAnalyst(int id, int analystId) throws SQLException {
        String sql = "UPDATE incidents SET assigned_analyst_id = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, analystId);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean closeIncident(int id) throws SQLException {
        String sql = "UPDATE incidents SET status = 'CLOSED', current_phase = 'CLOSED', closed_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM incidents WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM incidents";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int countByStatus(IncidentStatus status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM incidents WHERE status = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private Incident mapResultSetToIncident(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String title = rs.getString("title");
        String threatType = rs.getString("threat_type");
        IncidentSeverity severity = IncidentSeverity.valueOf(rs.getString("severity"));
        IncidentStatus status = IncidentStatus.valueOf(rs.getString("status"));
        int assetId = rs.getInt("asset_id");

        int rawAnalystId = rs.getInt("assigned_analyst_id");
        Integer analystId = rs.wasNull() ? null : rawAnalystId;

        int rawPlaybookId = rs.getInt("playbook_id");
        Integer playbookId = rs.wasNull() ? null : rawPlaybookId;

        double riskScore = rs.getDouble("risk_score");
        PlaybookPhase phase = PlaybookPhase.valueOf(rs.getString("current_phase"));

        LocalDateTime createdAt = parseTimestamp(rs.getString("created_at"));
        LocalDateTime closedAt = parseTimestamp(rs.getString("closed_at"));

        Incident inc = new Incident(id, title, threatType, severity, status, assetId,
                                    analystId, playbookId, riskScore, phase, createdAt, closedAt);
        inc.setAssetHostname(rs.getString("asset_hostname"));
        inc.setAnalystName(rs.getString("analyst_name"));
        inc.setPlaybookName(rs.getString("playbook_name"));
        return inc;
    }

    private LocalDateTime parseTimestamp(String ts) {
        if (ts == null) return null;
        try {
            return LocalDateTime.parse(ts.replace(" ", "T"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
