package com.minispl.application;

import com.minispl.application.remediation.*;
import com.minispl.domain.enums.AssetStatus;
import com.minispl.domain.enums.AuditStatus;
import com.minispl.domain.enums.CriticalityTier;
import com.minispl.domain.model.ActionAuditLog;
import com.minispl.domain.model.Asset;
import com.minispl.domain.model.Incident;
import com.minispl.domain.model.User;
import com.minispl.persistence.DatabaseManager;
import com.minispl.persistence.DatabaseSeeder;
import com.minispl.persistence.dao.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RemediationCommandAndFactoryTest {

    private AssetDAO assetDAO;
    private AuditLogDAO auditLogDAO;
    private RemediationCommandFactory factory;
    private CommandInvoker invoker;
    private Asset testAsset;
    private Incident testIncident;
    private User testUser;

    @BeforeEach
    public void setup() throws SQLException {
        String testDbPath = "test_cmd_" + System.nanoTime() + ".db";
        File f = new File(testDbPath);
        f.deleteOnExit();

        DatabaseManager.setDbUrl("jdbc:sqlite:" + testDbPath);
        DatabaseManager dbManager = DatabaseManager.getInstance();
        dbManager.initializeDatabase();

        UserDAO userDAO = new UserDAO(dbManager);
        assetDAO = new AssetDAO(dbManager);
        PlaybookDAO playbookDAO = new PlaybookDAO(dbManager);
        IncidentDAO incidentDAO = new IncidentDAO(dbManager);
        EvidenceDAO evidenceDAO = new EvidenceDAO(dbManager);
        auditLogDAO = new AuditLogDAO(dbManager);

        DatabaseSeeder seeder = new DatabaseSeeder(userDAO, assetDAO, playbookDAO, incidentDAO, evidenceDAO, auditLogDAO);
        seeder.seedIfEmpty();

        testUser = userDAO.findAll().get(0);
        testIncident = incidentDAO.findAll().get(0);
        testAsset = assetDAO.create(new Asset("APP-SRV-01", "10.0.1.50", CriticalityTier.HIGH, AssetStatus.ONLINE, "Target Server"));
        factory = new RemediationCommandFactory(assetDAO);
        invoker = new CommandInvoker(auditLogDAO, factory);
    }

    @Test
    public void testFactoryCommandCreation() {
        RemediationCommand isolateCmd = factory.createCommand("ISOLATE_HOST", testIncident.getId(), testUser.getId(),
                Map.of("asset_id", String.valueOf(testAsset.getId()), "hostname", testAsset.getHostname()));
        assertTrue(isolateCmd instanceof IsolateHostCommand);
        assertEquals("ISOLATE_HOST", isolateCmd.getCommandType());

        RemediationCommand blockCmd = factory.createCommand("BLOCK_IP", testIncident.getId(), testUser.getId(),
                Map.of("ip", "203.0.113.5", "direction", "EGRESS"));
        assertTrue(blockCmd instanceof BlockIPCommand);
        assertEquals("BLOCK_IP", blockCmd.getCommandType());

        RemediationCommand revokeCmd = factory.createCommand("REVOKE_CREDENTIALS", testIncident.getId(), testUser.getId(),
                Map.of("account", "bad.actor@corp.org"));
        assertTrue(revokeCmd instanceof RevokeCredentialsCommand);
        assertEquals("REVOKE_CREDENTIALS", revokeCmd.getCommandType());
    }

    @Test
    public void testIsolateHostExecuteAndUndo() throws Exception {
        RemediationCommand isolateCmd = factory.createCommand("ISOLATE_HOST", testIncident.getId(), testUser.getId(),
                Map.of("asset_id", String.valueOf(testAsset.getId()), "hostname", testAsset.getHostname()));

        // Execute via Invoker
        ActionAuditLog log = invoker.execute(isolateCmd);
        assertNotNull(log);
        assertTrue(log.getId() > 0);
        assertEquals(AuditStatus.EXECUTED, log.getStatus());
        assertTrue(log.isCanUndo());

        // Asset in DB should now be ISOLATED
        Asset updated = assetDAO.findById(testAsset.getId()).orElseThrow();
        assertEquals(AssetStatus.ISOLATED, updated.getStatus());

        // Undo via Invoker
        RemediationCommand undone = invoker.undoLast();
        assertSame(isolateCmd, undone);

        // Asset in DB should now be restored to ONLINE
        Asset restored = assetDAO.findById(testAsset.getId()).orElseThrow();
        assertEquals(AssetStatus.ONLINE, restored.getStatus());

        // Audit log in DB should now reflect UNDONE
        ActionAuditLog dbLog = auditLogDAO.findById(log.getId()).orElseThrow();
        assertEquals(AuditStatus.UNDONE, dbLog.getStatus());
        assertFalse(dbLog.isCanUndo());
    }

    @Test
    public void testBlockIPCommandExecutionAndUndo() throws Exception {
        BlockIPCommand cmd = new BlockIPCommand(testIncident.getId(), testUser.getId(), "198.51.100.99", "EGRESS", "PaloAlto-Edge");
        ActionAuditLog log = invoker.execute(cmd);
        assertEquals(AuditStatus.EXECUTED, log.getStatus());

        invoker.undoLast();
        ActionAuditLog updatedLog = auditLogDAO.findById(log.getId()).orElseThrow();
        assertEquals(AuditStatus.UNDONE, updatedLog.getStatus());
    }

    @Test
    public void testRevokeCredentialsExecutionAndUndo() throws Exception {
        RevokeCredentialsCommand cmd = new RevokeCredentialsCommand(testIncident.getId(), testUser.getId(), "victim.exec@corp.org", "ALL_SESSIONS");
        ActionAuditLog log = invoker.execute(cmd);
        assertEquals(AuditStatus.EXECUTED, log.getStatus());

        invoker.undoLast();
        ActionAuditLog updatedLog = auditLogDAO.findById(log.getId()).orElseThrow();
        assertEquals(AuditStatus.UNDONE, updatedLog.getStatus());
    }

    @Test
    public void testUndoByAuditLogId() throws Exception {
        RemediationCommand isolateCmd = factory.createCommand("ISOLATE_HOST", testIncident.getId(), testUser.getId(),
                Map.of("asset_id", String.valueOf(testAsset.getId()), "hostname", testAsset.getHostname()));

        ActionAuditLog log = invoker.execute(isolateCmd);
        assertTrue(log.getId() > 0);

        boolean ok = invoker.undoByAuditLogId(log.getId());
        assertTrue(ok);

        Asset restored = assetDAO.findById(testAsset.getId()).orElseThrow();
        assertEquals(AssetStatus.ONLINE, restored.getStatus());
    }
}
