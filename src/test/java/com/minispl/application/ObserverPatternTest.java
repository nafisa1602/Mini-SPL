package com.minispl.application;

import com.minispl.application.evidence.EvidenceCustodyStateMachine;
import com.minispl.application.incident.IncidentStateMachine;
import com.minispl.application.observer.AuditTrailListener;
import com.minispl.application.observer.IncidentEvent;
import com.minispl.application.observer.IncidentEventListener;
import com.minispl.application.observer.IncidentEventPublisher;
import com.minispl.application.remediation.CommandInvoker;
import com.minispl.application.remediation.RemediationCommand;
import com.minispl.application.remediation.RemediationCommandFactory;
import com.minispl.domain.enums.*;
import com.minispl.domain.model.*;
import com.minispl.persistence.DatabaseManager;
import com.minispl.persistence.DatabaseSeeder;
import com.minispl.persistence.dao.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ObserverPatternTest {

    private DatabaseManager dbManager;
    private IncidentDAO incidentDAO;
    private EvidenceDAO evidenceDAO;
    private AuditLogDAO auditLogDAO;
    private AssetDAO assetDAO;
    private RemediationCommandFactory factory;
    private CommandInvoker invoker;

    private User testUser;
    private Asset testAsset;
    private Incident testIncident;
    private Playbook testPlaybook;

    @BeforeEach
    public void setup() throws SQLException {
        String testDbPath = "test_obs_" + System.nanoTime() + ".db";
        File f = new File(testDbPath);
        f.deleteOnExit();

        DatabaseManager.setDbUrl("jdbc:sqlite:" + testDbPath);
        dbManager = DatabaseManager.getInstance();
        dbManager.initializeDatabase();

        UserDAO userDAO = new UserDAO(dbManager);
        assetDAO = new AssetDAO(dbManager);
        PlaybookDAO playbookDAO = new PlaybookDAO(dbManager);
        incidentDAO = new IncidentDAO(dbManager);
        evidenceDAO = new EvidenceDAO(dbManager);
        auditLogDAO = new AuditLogDAO(dbManager);

        DatabaseSeeder seeder = new DatabaseSeeder(userDAO, assetDAO, playbookDAO, incidentDAO, evidenceDAO, auditLogDAO);
        seeder.seedIfEmpty();

        testUser = userDAO.findAll().get(0);
        testAsset = assetDAO.findAll().get(0);
        testPlaybook = playbookDAO.findAll().get(0);

        testIncident = new Incident("Phishing Credential Harvest", "PHISHING", IncidentSeverity.HIGH,
                testAsset.getId(), testUser.getId(), testPlaybook.getId());
        testIncident = incidentDAO.create(testIncident);

        factory = new RemediationCommandFactory(assetDAO);
        invoker = new CommandInvoker(auditLogDAO, factory);

        IncidentEventPublisher.getInstance().clearListeners();
    }

    @Test
    public void testEventPublisherSubscriptionAndNotification() {
        IncidentEventPublisher publisher = IncidentEventPublisher.getInstance();
        List<IncidentEvent> receivedEvents = new ArrayList<>();

        IncidentEventListener listener = receivedEvents::add;
        publisher.subscribe(listener);

        IncidentEvent testEvent = new IncidentEvent(
                IncidentEvent.EventType.INCIDENT_STATE_CHANGED,
                testIncident.getId(),
                "NEW",
                "TRIAGED",
                testUser.getId(),
                "Scoping triage complete"
        );

        publisher.publish(testEvent);

        assertEquals(1, receivedEvents.size());
        assertEquals(testEvent, receivedEvents.get(0));

        publisher.unsubscribe(listener);
        publisher.publish(testEvent);

        assertEquals(1, receivedEvents.size(), "Unsubscribed listener should not receive subsequent events");
    }

    @Test
    public void testIncidentStateMachinePublishesEvents() throws SQLException {
        IncidentEventPublisher publisher = IncidentEventPublisher.getInstance();
        List<IncidentEvent> events = new ArrayList<>();
        publisher.subscribe(events::add);

        IncidentStateMachine sm = new IncidentStateMachine(testIncident, incidentDAO);

        // 1. Triage: NEW -> TRIAGED
        sm.triage(testPlaybook.getId(), testUser.getId());
        assertEquals(1, events.size());
        IncidentEvent triageEvent = events.get(0);
        assertEquals(IncidentEvent.EventType.INCIDENT_STATE_CHANGED, triageEvent.getType());
        assertEquals(testIncident.getId(), triageEvent.getIncidentId());
        assertEquals("NEW", triageEvent.getPreviousState());
        assertEquals("TRIAGED", triageEvent.getNewState());

        // 2. Contain: TRIAGED -> CONTAINED
        sm.contain();
        assertEquals(2, events.size());
        IncidentEvent containEvent = events.get(1);
        assertEquals(IncidentEvent.EventType.INCIDENT_STATE_CHANGED, containEvent.getType());
        assertEquals("TRIAGED", containEvent.getPreviousState());
        assertEquals("CONTAINED", containEvent.getNewState());

        // 3. Close: CONTAINED -> CLOSED
        sm.close();
        assertEquals(3, events.size());
        IncidentEvent closeEvent = events.get(2);
        assertEquals(IncidentEvent.EventType.INCIDENT_STATE_CHANGED, closeEvent.getType());
        assertEquals("CONTAINED", closeEvent.getPreviousState());
        assertEquals("CLOSED", closeEvent.getNewState());
    }

    @Test
    public void testEvidenceCustodyStateMachinePublishesEvents() throws SQLException {
        EvidenceItem item = new EvidenceItem(
                testIncident.getId(),
                testAsset.getId(),
                "test_memory.raw",
                "MEMORY_DUMP",
                "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                testUser.getId()
        );
        item = evidenceDAO.create(item);

        IncidentEventPublisher publisher = IncidentEventPublisher.getInstance();
        List<IncidentEvent> events = new ArrayList<>();
        publisher.subscribe(events::add);

        EvidenceCustodyStateMachine csm = new EvidenceCustodyStateMachine(item, evidenceDAO);

        // SEIZED -> IN_ANALYSIS
        csm.beginAnalysis(testUser.getId());
        assertEquals(1, events.size());
        assertEquals(IncidentEvent.EventType.EVIDENCE_CUSTODY_CHANGED, events.get(0).getType());
        assertEquals("SEIZED", events.get(0).getPreviousState());
        assertEquals("IN_ANALYSIS", events.get(0).getNewState());

        // IN_ANALYSIS -> COURT_HOLD
        csm.placeOnCourtHold(testUser.getId());
        assertEquals(2, events.size());
        assertEquals("IN_ANALYSIS", events.get(1).getPreviousState());
        assertEquals("COURT_HOLD", events.get(1).getNewState());

        // COURT_HOLD -> IN_ANALYSIS
        csm.releaseFromCourtHold(testUser.getId());
        assertEquals(3, events.size());
        assertEquals("COURT_HOLD", events.get(2).getPreviousState());
        assertEquals("IN_ANALYSIS", events.get(2).getNewState());

        // IN_ANALYSIS -> ARCHIVED
        csm.archive(testUser.getId());
        assertEquals(4, events.size());
        assertEquals("IN_ANALYSIS", events.get(3).getPreviousState());
        assertEquals("ARCHIVED", events.get(3).getNewState());
    }

    @Test
    public void testCommandInvokerPublishesExecutionAndUndoneEvents() throws Exception {
        IncidentEventPublisher publisher = IncidentEventPublisher.getInstance();
        List<IncidentEvent> events = new ArrayList<>();
        publisher.subscribe(events::add);

        RemediationCommand blockCmd = factory.createCommand("BLOCK_IP", testIncident.getId(), testUser.getId(),
                Map.of("ip", "192.0.2.1", "direction", "INGRESS"));

        // Execute command
        invoker.execute(blockCmd);
        assertEquals(1, events.size());
        assertEquals(IncidentEvent.EventType.COMMAND_EXECUTED, events.get(0).getType());
        assertEquals("BLOCK_IP", events.get(0).getNewState());

        // Undo command
        invoker.undoLast();
        assertEquals(2, events.size());
        assertEquals(IncidentEvent.EventType.COMMAND_UNDONE, events.get(1).getType());
        assertEquals("BLOCK_IP", events.get(1).getPreviousState());
        assertEquals("UNDONE", events.get(1).getNewState());
    }

    @Test
    public void testAuditTrailListenerLogsStateTransitionsToDatabase() throws SQLException {
        // Concrete Observer that closes the gap: logging state transitions into action_audit_logs
        AuditTrailListener listener = new AuditTrailListener(auditLogDAO);
        IncidentEventPublisher.getInstance().subscribe(listener);

        int initialCount = auditLogDAO.findByIncidentId(testIncident.getId()).size();

        IncidentStateMachine sm = new IncidentStateMachine(testIncident, incidentDAO);
        sm.triage(testPlaybook.getId(), testUser.getId());
        sm.contain();

        List<ActionAuditLog> logs = auditLogDAO.findByIncidentId(testIncident.getId());
        assertEquals(initialCount + 2, logs.size(), "Two state transitions should be logged in action_audit_logs");

        ActionAuditLog containLog = logs.get(0); // ordered by id DESC
        assertEquals("STATE_TRANSITION", containLog.getCommandType());
        assertFalse(containLog.isCanUndo(), "State transitions must not be undoable via command undo stack");
        assertEquals(AuditStatus.EXECUTED, containLog.getStatus());
        assertTrue(containLog.getParameters().contains("CONTAINED"));
    }
}
