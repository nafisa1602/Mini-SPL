package com.minispl.persistence.dao;

import com.minispl.domain.enums.PlaybookPhase;
import com.minispl.domain.model.Playbook;
import com.minispl.domain.model.PlaybookStep;
import com.minispl.persistence.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlaybookDAO {

    private final DatabaseManager dbManager;

    public PlaybookDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public PlaybookDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public Playbook createPlaybook(Playbook playbook) throws SQLException {
        String sql = "INSERT INTO playbooks (threat_type, description) VALUES (?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, playbook.getThreatType());
            stmt.setString(2, playbook.getDescription());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    playbook.setId(rs.getInt(1));
                }
            }
        }
        return playbook;
    }

    public PlaybookStep createStep(PlaybookStep step) throws SQLException {
        String sql = "INSERT INTO playbook_steps (playbook_id, phase, sequence_order, step_name, action_type, pass_fail_criteria) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, step.getPlaybookId());
            stmt.setString(2, step.getPhase().name());
            stmt.setInt(3, step.getSequenceOrder());
            stmt.setString(4, step.getStepName());
            stmt.setString(5, step.getActionType());
            stmt.setString(6, step.getPassFailCriteria());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    step.setId(rs.getInt(1));
                }
            }
        }
        return step;
    }

    public Optional<Playbook> findById(int id) throws SQLException {
        String sql = "SELECT id, threat_type, description FROM playbooks WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Playbook pb = new Playbook(
                            rs.getInt("id"),
                            rs.getString("threat_type"),
                            rs.getString("description")
                    );
                    pb.setSteps(getStepsByPlaybookId(pb.getId()));
                    return Optional.of(pb);
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Playbook> findByThreatType(String threatType) throws SQLException {
        String sql = "SELECT id, threat_type, description FROM playbooks WHERE threat_type = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, threatType);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Playbook pb = new Playbook(
                            rs.getInt("id"),
                            rs.getString("threat_type"),
                            rs.getString("description")
                    );
                    pb.setSteps(getStepsByPlaybookId(pb.getId()));
                    return Optional.of(pb);
                }
            }
        }
        return Optional.empty();
    }

    public List<Playbook> findAll() throws SQLException {
        List<Playbook> list = new ArrayList<>();
        String sql = "SELECT id, threat_type, description FROM playbooks ORDER BY id ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Playbook pb = new Playbook(
                        rs.getInt("id"),
                        rs.getString("threat_type"),
                        rs.getString("description")
                );
                pb.setSteps(getStepsByPlaybookId(pb.getId()));
                list.add(pb);
            }
        }
        return list;
    }

    public List<PlaybookStep> getStepsByPlaybookId(int playbookId) throws SQLException {
        List<PlaybookStep> steps = new ArrayList<>();
        String sql = "SELECT id, playbook_id, phase, sequence_order, step_name, action_type, pass_fail_criteria " +
                     "FROM playbook_steps WHERE playbook_id = ? ORDER BY sequence_order ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, playbookId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    steps.add(new PlaybookStep(
                            rs.getInt("id"),
                            rs.getInt("playbook_id"),
                            PlaybookPhase.valueOf(rs.getString("phase")),
                            rs.getInt("sequence_order"),
                            rs.getString("step_name"),
                            rs.getString("action_type"),
                            rs.getString("pass_fail_criteria")
                    ));
                }
            }
        }
        return steps;
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM playbooks";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}
