package com.minispl.application;

import com.minispl.application.observer.IncidentEvent;
import com.minispl.application.observer.IncidentEventPublisher;
import com.minispl.application.playbook.*;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TemplateMethodPlaybookTest {

    private DatabaseManager dbManager;
    private IncidentDAO incidentDAO;
    private User testUser;
    private Asset testAsset;
    private Playbook testPlaybook;

    @BeforeEach
    public void setup() throws SQLException {
        String testDbPath = "test_playbook_" + System.nanoTime() + ".db";
        File f = new File(testDbPath);
        f.deleteOnExit();

        DatabaseManager.setDbUrl("jdbc:sqlite:" + testDbPath);
        dbManager = DatabaseManager.getInstance();
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
        testAsset = assetDAO.findAll().get(0);
        testPlaybook = playbookDAO.findAll().get(0);

        IncidentEventPublisher.getInstance().clearListeners();
    }

    @Test
    public void testRansomwarePlaybookTemplateMethodLifecycle() throws SQLException {
        Incident incident = new Incident("Active LockBit Infection", "RANSOMWARE", IncidentSeverity.CRITICAL,
                testAsset.getId(), testUser.getId(), testPlaybook.getId());
        incident = incidentDAO.create(incident);

        List<IncidentEvent> publishedEvents = new ArrayList<>();
        IncidentEventPublisher.getInstance().subscribe(publishedEvents::add);

        InvestigationEngine engine = new RansomwarePlaybookEngine(incidentDAO);
        assertEquals("RANSOMWARE", engine.getThreatType());

        // Execute invariant Template Method
        InvestigationReport report = engine.executePlaybook(incident);

        assertNotNull(report);
        assertEquals(incident.getId(), report.getIncidentId());
        assertEquals("RANSOMWARE", report.getThreatType());
        assertTrue(report.isFullyCompleted());
        assertTrue(report.isAllSuccessful());
        assertEquals(5, report.getStepResults().size(), "Must execute exactly 5 invariant phases");

        // Verify invariant sequence: TRIAGE -> CONTAINMENT -> EVIDENCE_COLLECTION -> ERADICATION -> POST_MORTEM
        List<InvestigationStepResult> steps = report.getStepResults();
        assertEquals(PlaybookPhase.TRIAGE, steps.get(0).getPhase());
        assertEquals(PlaybookPhase.CONTAINMENT, steps.get(1).getPhase());
        assertEquals(PlaybookPhase.EVIDENCE_COLLECTION, steps.get(2).getPhase());
        assertEquals(PlaybookPhase.ERADICATION, steps.get(3).getPhase());
        assertEquals(PlaybookPhase.POST_MORTEM, steps.get(4).getPhase());

        // Verify incident state is updated to CLOSED
        assertEquals(IncidentStatus.CLOSED, incident.getStatus());
        assertEquals(PlaybookPhase.CLOSED, incident.getCurrentPhase());

        Incident dbInc = incidentDAO.findById(incident.getId()).orElseThrow();
        assertEquals(IncidentStatus.CLOSED, dbInc.getStatus());

        // Verify observer event published
        assertEquals(1, publishedEvents.size());
        assertEquals(IncidentEvent.EventType.PLAYBOOK_EXECUTED, publishedEvents.get(0).getType());
    }

    @Test
    public void testPhishingPlaybookTemplateMethodLifecycle() throws SQLException {
        Incident incident = new Incident("Targeted Spear-Phishing Campaign", "PHISHING", IncidentSeverity.HIGH,
                testAsset.getId(), testUser.getId(), testPlaybook.getId());
        incident = incidentDAO.create(incident);

        InvestigationEngine engine = new PhishingPlaybookEngine(incidentDAO);
        assertEquals("PHISHING", engine.getThreatType());

        InvestigationReport report = engine.executePlaybook(incident);

        assertNotNull(report);
        assertTrue(report.isFullyCompleted());
        assertEquals(5, report.getStepResults().size());

        // Verify phishing-specific steps
        InvestigationStepResult containStep = report.getResultForPhase(PlaybookPhase.CONTAINMENT).orElseThrow();
        assertTrue(containStep.getStepName().contains("Credentials"));
        assertTrue(containStep.isSuccess());

        InvestigationStepResult evidenceStep = report.getResultForPhase(PlaybookPhase.EVIDENCE_COLLECTION).orElseThrow();
        assertTrue(evidenceStep.getActionTaken().contains("EML"));
    }

    @Test
    public void testPlaybookSubclassAliases() throws SQLException {
        Incident inc1 = new Incident("Ransomware Case", "RANSOMWARE", IncidentSeverity.HIGH, testAsset.getId(), testUser.getId(), testPlaybook.getId());
        inc1 = incidentDAO.create(inc1);

        RansomwarePlaybook rp = new RansomwarePlaybook(incidentDAO);
        InvestigationReport r1 = rp.executeInvestigation(inc1);
        assertTrue(r1.isFullyCompleted());

        Incident inc2 = new Incident("Phishing Case", "PHISHING", IncidentSeverity.HIGH, testAsset.getId(), testUser.getId(), testPlaybook.getId());
        inc2 = incidentDAO.create(inc2);

        PhishingPlaybook pp = new PhishingPlaybook(incidentDAO);
        InvestigationReport r2 = pp.executeInvestigation(inc2);
        assertTrue(r2.isFullyCompleted());
    }

    @Test
    public void testPlaybookEngineFactory() {
        InvestigationEngine rw = PlaybookEngineFactory.getEngine("RANSOMWARE", incidentDAO);
        assertTrue(rw instanceof RansomwarePlaybookEngine);

        InvestigationEngine ph = PlaybookEngineFactory.getEngine("PHISHING", incidentDAO);
        assertTrue(ph instanceof PhishingPlaybookEngine);

        InvestigationEngine def = PlaybookEngineFactory.getEngine("UNKNOWN_THREAT", incidentDAO);
        assertNotNull(def);
    }

    @Test
    public void testStepByStepPhaseExecution() throws SQLException {
        Incident incident = new Incident("Step by Step Test", "RANSOMWARE", IncidentSeverity.MEDIUM,
                testAsset.getId(), testUser.getId(), testPlaybook.getId());
        incident = incidentDAO.create(incident);

        InvestigationEngine engine = new RansomwarePlaybookEngine(incidentDAO);

        InvestigationStepResult triage = engine.executePhase(PlaybookPhase.TRIAGE, incident);
        assertTrue(triage.isSuccess());
        assertEquals(PlaybookPhase.TRIAGE, incident.getCurrentPhase());

        InvestigationStepResult contain = engine.executePhase(PlaybookPhase.CONTAINMENT, incident);
        assertTrue(contain.isSuccess());
        assertEquals(PlaybookPhase.CONTAINMENT, incident.getCurrentPhase());
    }
}
