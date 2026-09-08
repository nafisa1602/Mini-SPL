package com.minispl.persistence.dao;

import com.minispl.domain.enums.AssetStatus;
import com.minispl.domain.enums.CriticalityTier;
import com.minispl.domain.model.Asset;
import com.minispl.persistence.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AssetDAO {

    private final DatabaseManager dbManager;

    public AssetDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public AssetDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public Asset create(Asset asset) throws SQLException {
        String sql = "INSERT INTO assets (hostname, ip_address, criticality_tier, status, description) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, asset.getHostname());
            stmt.setString(2, asset.getIpAddress());
            stmt.setString(3, asset.getCriticalityTier().name());
            stmt.setString(4, asset.getStatus().name());
            stmt.setString(5, asset.getDescription());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    asset.setId(rs.getInt(1));
                }
            }
        }
        return asset;
    }

    public Optional<Asset> findById(int id) throws SQLException {
        String sql = "SELECT id, hostname, ip_address, criticality_tier, status, description FROM assets WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAsset(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Asset> findByHostname(String hostname) throws SQLException {
        String sql = "SELECT id, hostname, ip_address, criticality_tier, status, description FROM assets WHERE hostname = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hostname);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAsset(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Asset> findAll() throws SQLException {
        List<Asset> list = new ArrayList<>();
        String sql = "SELECT id, hostname, ip_address, criticality_tier, status, description FROM assets ORDER BY id ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToAsset(rs));
            }
        }
        return list;
    }

    public boolean updateStatus(int assetId, AssetStatus newStatus) throws SQLException {
        String sql = "UPDATE assets SET status = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus.name());
            stmt.setInt(2, assetId);
            return stmt.executeUpdate() > 0;
        }
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM assets";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private Asset mapResultSetToAsset(ResultSet rs) throws SQLException {
        return new Asset(
                rs.getInt("id"),
                rs.getString("hostname"),
                rs.getString("ip_address"),
                CriticalityTier.valueOf(rs.getString("criticality_tier")),
                AssetStatus.valueOf(rs.getString("status")),
                rs.getString("description")
        );
    }
}
