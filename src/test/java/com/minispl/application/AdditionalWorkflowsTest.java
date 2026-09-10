package com.minispl.application;

import com.minispl.application.observer.IncidentEvent;
import com.minispl.application.observer.IncidentEventListener;
import com.minispl.application.observer.IncidentEventPublisher;
import com.minispl.domain.enums.AssetStatus;
import com.minispl.domain.enums.CriticalityTier;
import com.minispl.domain.model.ActionAuditLog;
import com.minispl.domain.model.Asset;
import com.minispl.persistence.DatabaseManager;
import com.minispl.persistence.DatabaseSeeder;
import com.minispl.persistence.dao.AssetDAO;
import com.minispl.persistence.dao.AuditLogDAO;
import com.minispl.presentation.EvidenceViewController;
import com.minispl.presentation.ReportsViewController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AdditionalWorkflowsTest {

    private AssetDAO assetDAO;
    private AuditLogDAO auditLogDAO;

    @BeforeEach
    public void setUp() throws Exception {
        String testDbPath = "test_workflows_" + System.currentTimeMillis() + ".db";
        File testDb = new File(testDbPath);
        testDb.deleteOnExit();

        DatabaseManager.setDbUrl("jdbc:sqlite:" + testDbPath);
        DatabaseManager.getInstance().initializeDatabase();
        new DatabaseSeeder().seedIfEmpty();

        this.assetDAO = new AssetDAO();
        this.auditLogDAO = new AuditLogDAO();
        IncidentEventPublisher.getInstance().clearListeners();
    }

    @Test
    public void testAssetCrudWorkflow() throws Exception {
        // 1. CREATE
        Asset newAsset = new Asset(
                "srv-vault-01.corp.internal",
                "10.0.50.15",
                CriticalityTier.CRITICAL,
                AssetStatus.ONLINE,
                "Forensic Key Vault Server"
        );
        Asset created = assetDAO.create(newAsset);
        assertTrue(created.getId() > 0, "Created asset should receive generated ID");

        // 2. READ
        Optional<Asset> found = assetDAO.findById(created.getId());
        assertTrue(found.isPresent(), "Asset should be retrievable by ID");
        assertEquals("srv-vault-01.corp.internal", found.get().getHostname());
        assertEquals(CriticalityTier.CRITICAL, found.get().getCriticalityTier());

        // 3. UPDATE
        found.get().setStatus(AssetStatus.ISOLATED);
        found.get().setDescription("Quarantined during active forensic investigation");
        boolean updated = assetDAO.update(found.get());
        assertTrue(updated, "Asset update should return true");

        Optional<Asset> updatedAsset = assetDAO.findById(created.getId());
        assertTrue(updatedAsset.isPresent());
        assertEquals(AssetStatus.ISOLATED, updatedAsset.get().getStatus());
        assertEquals("Quarantined during active forensic investigation", updatedAsset.get().getDescription());

        // Count checks
        assertTrue(assetDAO.countByStatus(AssetStatus.ISOLATED) >= 1);
        assertTrue(assetDAO.countByCriticality(CriticalityTier.CRITICAL) >= 1);

        // 4. DELETE
        boolean deleted = assetDAO.delete(created.getId());
        assertTrue(deleted, "Asset delete should succeed");
        assertTrue(assetDAO.findById(created.getId()).isEmpty(), "Asset should no longer exist");
    }

    @Test
    public void testForensicDossierMarkdownGeneration() {
        ReportsViewController reportsController = new ReportsViewController();
        String markdown = reportsController.generateForensicDossierMarkdown();

        assertNotNull(markdown);
        assertTrue(markdown.contains("DFIR Investigation Dossier & Security Post-Mortem"));
        assertTrue(markdown.contains("Executive Summary & KPIs"));
        assertTrue(markdown.contains("Active & Historical Incident Register"));
        assertTrue(markdown.contains("Digital Forensics Chain of Custody"));
        assertTrue(markdown.contains("Remediation Action Audit Ledger"));
        assertTrue(markdown.contains("Mean Time to Contain (MTTC)"));
    }

    @Test
    public void testAuditLedgerCsvGeneration() throws Exception {
        List<ActionAuditLog> logs = auditLogDAO.findAll();
        String csv = ReportsViewController.generateAuditLedgerCsv(logs);

        assertNotNull(csv);
        assertTrue(csv.startsWith("Log_ID,Incident_ID,Command_Type,Parameters,Executed_By,Timestamp,Can_Undo,Status"));
        if (!logs.isEmpty()) {
            ActionAuditLog first = logs.get(0);
            assertTrue(csv.contains(first.getCommandType()));
        }
    }

    @Test
    public void testEvidenceSha256Verification() throws Exception {
        File tempEvidenceFile = File.createTempFile("evidence_artifact_", ".bin");
        tempEvidenceFile.deleteOnExit();

        String payload = "CONFIDENTIAL FORENSIC DUMP: memory_image_0x89ab12cd";
        try (FileWriter writer = new FileWriter(tempEvidenceFile)) {
            writer.write(payload);
        }

        // Expected hash
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder expectedHex = new StringBuilder();
        for (byte b : digest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) expectedHex.append('0');
            expectedHex.append(hex);
        }

        // Actual computed hash via EvidenceViewController helper
        String computedHash = EvidenceViewController.computeFileSHA256(tempEvidenceFile);

        assertEquals(expectedHex.toString(), computedHash, "Computed file hash should match expected SHA-256 digest");
    }

    @Test
    public void testAssetObserverNotifications() {
        List<IncidentEvent> receivedEvents = new ArrayList<>();
        IncidentEventListener testListener = receivedEvents::add;

        IncidentEventPublisher.getInstance().subscribe(testListener);

        IncidentEvent event1 = new IncidentEvent(
                IncidentEvent.EventType.ASSET_CREATED,
                0,
                null,
                "ONLINE",
                1,
                "Registered new endpoint"
        );
        IncidentEvent event2 = new IncidentEvent(
                IncidentEvent.EventType.EVIDENCE_INTEGRITY_VERIFIED,
                1,
                "UNVERIFIED",
                "VERIFIED_MATCH",
                1,
                "Artifact hash confirmed"
        );

        IncidentEventPublisher.getInstance().publish(event1);
        IncidentEventPublisher.getInstance().publish(event2);

        assertEquals(2, receivedEvents.size());
        assertEquals(IncidentEvent.EventType.ASSET_CREATED, receivedEvents.get(0).getType());
        assertEquals(IncidentEvent.EventType.EVIDENCE_INTEGRITY_VERIFIED, receivedEvents.get(1).getType());
    }
}
