package com.minispl.persistence;

import com.minispl.domain.enums.*;
import com.minispl.domain.model.*;
import com.minispl.persistence.dao.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseAndDAOTest {

    private static UserDAO userDAO;
    private static AssetDAO assetDAO;
    private static PlaybookDAO playbookDAO;
    private static IncidentDAO incidentDAO;
    private static EvidenceDAO evidenceDAO;
    private static AuditLogDAO auditLogDAO;

    @BeforeAll
    public static void setup() {
        // Use a clean temporary file database for tests
        String testDbPath = "test_dfir_" + System.currentTimeMillis() + ".db";
        File testDb = new File(testDbPath);
        testDb.deleteOnExit();

        DatabaseManager.setDbUrl("jdbc:sqlite:" + testDbPath);
        DatabaseManager dbManager = DatabaseManager.getInstance();
        dbManager.initializeDatabase();

        userDAO = new UserDAO(dbManager);
        assetDAO = new AssetDAO(dbManager);
        playbookDAO = new PlaybookDAO(dbManager);
        incidentDAO = new IncidentDAO(dbManager);
        evidenceDAO = new EvidenceDAO(dbManager);
        auditLogDAO = new AuditLogDAO(dbManager);

        DatabaseSeeder seeder = new DatabaseSeeder(userDAO, assetDAO, playbookDAO, incidentDAO, evidenceDAO, auditLogDAO);
        seeder.seedIfEmpty();
    }

    @Test
    public void testSeedDataLoaded() throws SQLException {
        assertTrue(userDAO.count() >= 3, "Should have at least 3 users");
        assertTrue(assetDAO.count() >= 4, "Should have at least 4 assets");
        assertTrue(playbookDAO.count() >= 2, "Should have at least 2 playbooks");
        assertTrue(incidentDAO.count() >= 3, "Should have at least 3 incidents");
        assertTrue(evidenceDAO.count() >= 3, "Should have at least 3 evidence items");
        assertTrue(auditLogDAO.count() >= 3, "Should have at least 3 audit logs");
    }

    @Test
    public void testUserQueries() throws SQLException {
        Optional<User> user = userDAO.findByEmail("alice.walker@soc.org");
        assertTrue(user.isPresent());
        assertEquals("Alice Walker", user.get().getFullName());
        assertEquals(UserRole.SENIOR_ANALYST, user.get().getRole());
    }

    @Test
    public void testAssetQueriesAndStatusUpdate() throws SQLException {
        List<Asset> assets = assetDAO.findAll();
        assertFalse(assets.isEmpty());
        Asset first = assets.get(0);

        boolean updated = assetDAO.updateStatus(first.getId(), AssetStatus.QUARANTINED);
        assertTrue(updated);

        Asset refreshed = assetDAO.findById(first.getId()).orElseThrow();
        assertEquals(AssetStatus.QUARANTINED, refreshed.getStatus());
    }

    @Test
    public void testPlaybookAndSteps() throws SQLException {
        Optional<Playbook> pb = playbookDAO.findByThreatType("RANSOMWARE");
        assertTrue(pb.isPresent());
        assertFalse(pb.get().getSteps().isEmpty(), "Playbook should have steps loaded");
        assertEquals(PlaybookPhase.TRIAGE, pb.get().getSteps().get(0).getPhase());
    }

    @Test
    public void testIncidentLifecycle() throws SQLException {
        List<Asset> assets = assetDAO.findAll();
        List<User> users = userDAO.findAll();
        List<Playbook> playbooks = playbookDAO.findAll();

        Incident newInc = new Incident(
                "Test Outbreak Incident",
                "RANSOMWARE",
                IncidentSeverity.CRITICAL,
                assets.get(0).getId(),
                users.get(0).getId(),
                playbooks.get(0).getId()
        );
        newInc.setRiskScore(90.0);
        Incident created = incidentDAO.create(newInc);
        assertNotNull(created);
        assertTrue(created.getId() > 0);
        assertEquals(IncidentStatus.NEW, created.getStatus());

        // Update status to TRIAGED
        incidentDAO.updateStatus(created.getId(), IncidentStatus.TRIAGED);
        Incident triaged = incidentDAO.findById(created.getId()).orElseThrow();
        assertEquals(IncidentStatus.TRIAGED, triaged.getStatus());

        // Update phase to CONTAINMENT
        incidentDAO.updatePhase(created.getId(), PlaybookPhase.CONTAINMENT);
        Incident containedPhase = incidentDAO.findById(created.getId()).orElseThrow();
        assertEquals(PlaybookPhase.CONTAINMENT, containedPhase.getCurrentPhase());

        // Close incident
        incidentDAO.closeIncident(created.getId());
        Incident closed = incidentDAO.findById(created.getId()).orElseThrow();
        assertEquals(IncidentStatus.CLOSED, closed.getStatus());
        assertNotNull(closed.getClosedAt());
    }

    @Test
    public void testEvidenceCustodyTransition() throws SQLException {
        List<Incident> incidents = incidentDAO.findAll();
        List<Asset> assets = assetDAO.findAll();
        List<User> users = userDAO.findAll();

        EvidenceItem item = new EvidenceItem(
                incidents.get(0).getId(),
                assets.get(0).getId(),
                "test_pcap.pcap",
                "NETWORK_PCAP",
                "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                users.get(0).getId()
        );
        EvidenceItem created = evidenceDAO.create(item);
        assertEquals(CustodyStatus.SEIZED, created.getCustodyStatus());

        // Update custody to IN_ANALYSIS with a new custodian
        evidenceDAO.updateCustody(created.getId(), CustodyStatus.IN_ANALYSIS, users.get(1).getId());
        EvidenceItem updated = evidenceDAO.findById(created.getId()).orElseThrow();
        assertEquals(CustodyStatus.IN_ANALYSIS, updated.getCustodyStatus());
        assertEquals(users.get(1).getId(), updated.getCurrentCustodianId());
    }

    @Test
    public void testAuditLogExecutionAndUndo() throws SQLException {
        List<Incident> incidents = incidentDAO.findAll();
        List<User> users = userDAO.findAll();

        ActionAuditLog log = new ActionAuditLog(
                incidents.get(0).getId(),
                "ISOLATE_HOST",
                "{\"target\":\"10.0.0.10\"}",
                users.get(0).getId(),
                true,
                AuditStatus.EXECUTED
        );
        ActionAuditLog created = auditLogDAO.create(log);
        assertTrue(created.getId() > 0);
        assertTrue(created.isCanUndo());

        // Simulate undo action
        auditLogDAO.updateStatus(created.getId(), AuditStatus.UNDONE, false);
        ActionAuditLog undone = auditLogDAO.findById(created.getId()).orElseThrow();
        assertEquals(AuditStatus.UNDONE, undone.getStatus());
        assertFalse(undone.isCanUndo());
    }
}
