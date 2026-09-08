package com.minispl.application;

import com.minispl.application.incident.*;
import com.minispl.domain.enums.IncidentSeverity;
import com.minispl.domain.enums.IncidentStatus;
import com.minispl.domain.enums.PlaybookPhase;
import com.minispl.domain.model.Asset;
import com.minispl.domain.model.Incident;
import com.minispl.domain.model.Playbook;
import com.minispl.domain.model.User;
import com.minispl.persistence.DatabaseManager;
import com.minispl.persistence.DatabaseSeeder;
import com.minispl.persistence.dao.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IncidentStateMachineTest {

    private IncidentDAO incidentDAO;
    private Incident incident;
    private IncidentStateMachine machine;
    private Playbook testPlaybook;
    private User testUser;

    @BeforeEach
    public void setup() throws SQLException {
        String testDbPath = "test_state_" + System.nanoTime() + ".db";
        File f = new File(testDbPath);
        f.deleteOnExit();

        DatabaseManager.setDbUrl("jdbc:sqlite:" + testDbPath);
        DatabaseManager dbManager = DatabaseManager.getInstance();
        dbManager.initializeDatabase();

        UserDAO userDAO = new UserDAO(dbManager);
        AssetDAO assetDAO = new AssetDAO(dbManager);
        PlaybookDAO playbookDAO = new PlaybookDAO(dbManager);
        incidentDAO = new IncidentDAO(dbManager);
        EvidenceDAO evidenceDAO = new EvidenceDAO(dbManager);
        AuditLogDAO auditLogDAO = new AuditLogDAO(dbManager);

        DatabaseSeeder seeder = new DatabaseSeeder(userDAO, assetDAO, playbookDAO, incidentDAO, evidenceDAO, auditLogDAO);
        seeder.seedIfEmpty();

        testUser = userDAO.findAll().get(0);
        Asset testAsset = assetDAO.findAll().get(0);
        testPlaybook = playbookDAO.findAll().get(0);

        incident = new Incident("Ransomware Outbreak on DC-01", "RANSOMWARE", IncidentSeverity.CRITICAL, testAsset.getId(), testUser.getId(), testPlaybook.getId());
        incident.setStatus(IncidentStatus.NEW);
        incident.setCurrentPhase(PlaybookPhase.TRIAGE);
        incident = incidentDAO.create(incident);

        machine = new IncidentStateMachine(incident, incidentDAO);
    }

    @Test
    public void testInitialNewState() {
        assertEquals(IncidentStatus.NEW, machine.getStatus());
        assertTrue(machine.getCurrentState() instanceof NewIncidentState);

        List<IncidentStatus> transitions = machine.getValidTransitions();
        assertEquals(1, transitions.size());
        assertEquals(IncidentStatus.TRIAGED, transitions.get(0));
        assertTrue(machine.canTransitionTo(IncidentStatus.TRIAGED));
        assertFalse(machine.canTransitionTo(IncidentStatus.CONTAINED));
        assertFalse(machine.canTransitionTo(IncidentStatus.CLOSED));
    }

    @Test
    public void testIllegalDirectContainmentFromNew() {
        assertThrows(IllegalStateException.class, () -> machine.contain(),
                "A NEW incident cannot jump directly to CONTAINED without triage");
    }

    @Test
    public void testIllegalDirectClosureFromNew() {
        assertThrows(IllegalStateException.class, () -> machine.close(),
                "A NEW incident cannot jump directly to CLOSED without triage and containment");
    }

    @Test
    public void testTransitionToTriaged() throws SQLException {
        machine.triage(testPlaybook.getId(), testUser.getId());

        assertEquals(IncidentStatus.TRIAGED, machine.getStatus());
        assertEquals(PlaybookPhase.CONTAINMENT, incident.getCurrentPhase());
        assertTrue(machine.getCurrentState() instanceof TriagedIncidentState);

        // Verify DB update
        Incident fromDb = incidentDAO.findById(incident.getId()).orElseThrow();
        assertEquals(IncidentStatus.TRIAGED, fromDb.getStatus());
        assertEquals(PlaybookPhase.CONTAINMENT, fromDb.getCurrentPhase());

        // Valid transitions check
        List<IncidentStatus> transitions = machine.getValidTransitions();
        assertEquals(1, transitions.size());
        assertEquals(IncidentStatus.CONTAINED, transitions.get(0));
    }

    @Test
    public void testIllegalClosureFromTriaged() throws SQLException {
        machine.triage(testPlaybook.getId(), testUser.getId());
        assertThrows(IllegalStateException.class, () -> machine.close(),
                "A TRIAGED incident cannot jump directly to CLOSED without containment verification");
    }

    @Test
    public void testTransitionToContainedAndClosed() throws SQLException {
        machine.triage(testPlaybook.getId(), testUser.getId());
        machine.contain();

        assertEquals(IncidentStatus.CONTAINED, machine.getStatus());
        assertEquals(PlaybookPhase.EVIDENCE_COLLECTION, incident.getCurrentPhase());
        assertTrue(machine.getCurrentState() instanceof ContainedIncidentState);

        // Verify DB update
        Incident fromDbContained = incidentDAO.findById(incident.getId()).orElseThrow();
        assertEquals(IncidentStatus.CONTAINED, fromDbContained.getStatus());

        // Now close
        machine.close();
        assertEquals(IncidentStatus.CLOSED, machine.getStatus());
        assertTrue(machine.getCurrentState() instanceof ClosedIncidentState);

        Incident fromDbClosed = incidentDAO.findById(incident.getId()).orElseThrow();
        assertEquals(IncidentStatus.CLOSED, fromDbClosed.getStatus());
        assertNotNull(fromDbClosed.getClosedAt());

        // Terminal state check
        assertTrue(machine.getValidTransitions().isEmpty());
        assertThrows(IllegalStateException.class, () -> machine.triage(testPlaybook.getId(), testUser.getId()));
        assertThrows(IllegalStateException.class, () -> machine.contain());
        assertThrows(IllegalStateException.class, () -> machine.close());
    }
}
