package com.minispl.persistence.dao;

import com.minispl.domain.enums.CustodyStatus;
import com.minispl.domain.model.EvidenceItem;
import com.minispl.persistence.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EvidenceDAO {

    private final DatabaseManager dbManager;

    public EvidenceDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public EvidenceDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    private static final String BASE_SELECT =
            "SELECT e.id, e.incident_id, e.source_asset_id, e.evidence_name, e.evidence_type, " +
            "       e.file_hash, e.custody_status, e.current_custodian_id, e.collected_at, " +
            "       a.hostname AS source_asset_hostname, " +
            "       u.full_name AS custodian_name " +
            "FROM evidence_items e " +
            "JOIN assets a ON e.source_asset_id = a.id " +
            "JOIN users u ON e.current_custodian_id = u.id ";

    public EvidenceItem create(EvidenceItem item) throws SQLException {
        String sql = "INSERT INTO evidence_items (incident_id, source_asset_id, evidence_name, " +
                     "evidence_type, file_hash, custody_status, current_custodian_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, item.getIncidentId());
            stmt.setInt(2, item.getSourceAssetId());
            stmt.setString(3, item.getEvidenceName());
            stmt.setString(4, item.getEvidenceType());
            stmt.setString(5, item.getFileHash());
            stmt.setString(6, item.getCustodyStatus().name());
            stmt.setInt(7, item.getCurrentCustodianId());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    item.setId(rs.getInt(1));
                }
            }
        }
        return findById(item.getId()).orElse(item);
    }

    public Optional<EvidenceItem> findById(int id) throws SQLException {
        String sql = BASE_SELECT + "WHERE e.id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEvidenceItem(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<EvidenceItem> findAll() throws SQLException {
        List<EvidenceItem> list = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY e.id DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToEvidenceItem(rs));
            }
        }
        return list;
    }

    public List<EvidenceItem> findByIncidentId(int incidentId) throws SQLException {
        List<EvidenceItem> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE e.incident_id = ? ORDER BY e.id ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, incidentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToEvidenceItem(rs));
                }
            }
        }
        return list;
    }

    public boolean updateCustody(int evidenceId, CustodyStatus status, int custodianId) throws SQLException {
        String sql = "UPDATE evidence_items SET custody_status = ?, current_custodian_id = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setInt(2, custodianId);
            stmt.setInt(3, evidenceId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM evidence_items WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM evidence_items";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private EvidenceItem mapResultSetToEvidenceItem(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int incidentId = rs.getInt("incident_id");
        int sourceAssetId = rs.getInt("source_asset_id");
        String name = rs.getString("evidence_name");
        String type = rs.getString("evidence_type");
        String hash = rs.getString("file_hash");
        CustodyStatus status = CustodyStatus.valueOf(rs.getString("custody_status"));
        int custodianId = rs.getInt("current_custodian_id");
        LocalDateTime collectedAt = parseTimestamp(rs.getString("collected_at"));

        EvidenceItem item = new EvidenceItem(id, incidentId, sourceAssetId, name, type, hash, status, custodianId, collectedAt);
        item.setSourceAssetHostname(rs.getString("source_asset_hostname"));
        item.setCustodianName(rs.getString("custodian_name"));
        return item;
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
