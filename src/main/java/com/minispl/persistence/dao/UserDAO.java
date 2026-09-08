package com.minispl.persistence.dao;

import com.minispl.domain.enums.UserRole;
import com.minispl.domain.model.User;
import com.minispl.persistence.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    private final DatabaseManager dbManager;

    public UserDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public UserDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public User create(User user) throws SQLException {
        String sql = "INSERT INTO users (full_name, role, email) VALUES (?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getRole().name());
            stmt.setString(3, user.getEmail());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
        }
        return user;
    }

    public Optional<User> findById(int id) throws SQLException {
        String sql = "SELECT id, full_name, role, email, created_at FROM users WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT id, full_name, role, email, created_at FROM users WHERE email = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<User> findAll() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, full_name, role, email, created_at FROM users ORDER BY id ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        }
        return list;
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("full_name");
        UserRole role = UserRole.valueOf(rs.getString("role"));
        String email = rs.getString("email");
        String ts = rs.getString("created_at");
        LocalDateTime createdAt = parseTimestamp(ts);
        return new User(id, name, role, email, createdAt);
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
