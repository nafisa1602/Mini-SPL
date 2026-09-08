package com.minispl.application;

import com.minispl.application.evidence.*;
import com.minispl.domain.enums.CustodyStatus;
import com.minispl.domain.model.Asset;
import com.minispl.domain.model.EvidenceItem;
import com.minispl.domain.model.Incident;
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

public class EvidenceCustodyStateMachineTest {

    private EvidenceDAO evidenceDAO;
    private EvidenceItem item;
    private EvidenceCustodyStateMachine machine;
    private List<User> users;

    @BeforeEach
    public void setup() throws SQLException {
        String testDbPath = "test_custody_" + System.nanoTime() + ".db";
        File f = new File(testDbPath);
        f.deleteOnExit();

        DatabaseManager.setDbUrl("jdbc:sqlite:" + testDbPath);
        DatabaseManager dbManager = DatabaseManager.getInstance();
        dbManager.initializeDatabase();

        UserDAO userDAO = new UserDAO(dbManager);
        AssetDAO assetDAO = new AssetDAO(dbManager);
        PlaybookDAO playbookDAO = new PlaybookDAO(dbManager);
        IncidentDAO incidentDAO = new IncidentDAO(dbManager);
        evidenceDAO = new EvidenceDAO(dbManager);
        AuditLogDAO auditLogDAO = new AuditLogDAO(dbManager);

        DatabaseSeeder seeder = new DatabaseSeeder(userDAO, assetDAO, playbookDAO, incidentDAO, evidenceDAO, auditLogDAO);
        seeder.seedIfEmpty();

        users = userDAO.findAll();
        List<Asset> assets = assetDAO.findAll();
        List<Incident> incidents = incidentDAO.findAll();

        item = new EvidenceItem(
                incidents.get(0).getId(),
                assets.get(0).getId(),
                "test_ram.raw",
                "MEMORY_DUMP",
                "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                users.get(0).getId()
        );
        item.setCustodyStatus(CustodyStatus.SEIZED);
        item = evidenceDAO.create(item);

        machine = new EvidenceCustodyStateMachine(item, evidenceDAO);
    }

    @Test
    public void testInitialSeizedState() {
        assertEquals(CustodyStatus.SEIZED, machine.getStatus());
        assertTrue(machine.getCurrentState() instanceof SeizedCustodyState);

        List<CustodyStatus> valid = machine.getValidTransitions();
        assertEquals(1, valid.size());
        assertEquals(CustodyStatus.IN_ANALYSIS, valid.get(0));
    }

    @Test
    public void testIllegalCourtHoldDirectlyFromSeized() {
        assertThrows(IllegalStateException.class, () -> machine.placeOnCourtHold(users.get(1).getId()),
                "Seized evidence cannot bypass analysis and jump to court hold");
    }

    @Test
    public void testFullCustodyLifecycle() throws SQLException {
        int u1 = users.get(0).getId();
        int u2 = users.get(1).getId();
        int u3 = users.get(2).getId();

        // 1. SEIZED -> IN_ANALYSIS
        machine.beginAnalysis(u2);
        assertEquals(CustodyStatus.IN_ANALYSIS, machine.getStatus());
        assertTrue(machine.getCurrentState() instanceof InAnalysisCustodyState);

        EvidenceItem dbItem1 = evidenceDAO.findById(item.getId()).orElseThrow();
        assertEquals(CustodyStatus.IN_ANALYSIS, dbItem1.getCustodyStatus());
        assertEquals(u2, dbItem1.getCurrentCustodianId());

        // 2. IN_ANALYSIS -> COURT_HOLD
        machine.placeOnCourtHold(u3);
        assertEquals(CustodyStatus.COURT_HOLD, machine.getStatus());
        assertTrue(machine.getCurrentState() instanceof CourtHoldCustodyState);

        EvidenceItem dbItem2 = evidenceDAO.findById(item.getId()).orElseThrow();
        assertEquals(CustodyStatus.COURT_HOLD, dbItem2.getCustodyStatus());

        // 3. COURT_HOLD -> release to IN_ANALYSIS
        machine.releaseFromCourtHold(u2);
        assertEquals(CustodyStatus.IN_ANALYSIS, machine.getStatus());

        // 4. IN_ANALYSIS -> ARCHIVED
        machine.archive(u1);
        assertEquals(CustodyStatus.ARCHIVED, machine.getStatus());
        assertTrue(machine.getCurrentState() instanceof ArchivedCustodyState);

        EvidenceItem dbItem3 = evidenceDAO.findById(item.getId()).orElseThrow();
        assertEquals(CustodyStatus.ARCHIVED, dbItem3.getCustodyStatus());

        // 5. Terminal check
        assertTrue(machine.getValidTransitions().isEmpty());
        assertThrows(IllegalStateException.class, () -> machine.beginAnalysis(u1));
        assertThrows(IllegalStateException.class, () -> machine.placeOnCourtHold(u1));
        assertThrows(IllegalStateException.class, () -> machine.archive(u1));
    }
}
