package com.minispl.persistence;

import com.minispl.domain.enums.*;
import com.minispl.domain.model.*;
import com.minispl.persistence.dao.*;

import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * DatabaseSeeder loads realistic initial data into SQLite on startup if tables are empty.
 */
public class DatabaseSeeder {

    private static final Logger LOGGER = Logger.getLogger(DatabaseSeeder.class.getName());

    private final UserDAO userDAO;
    private final AssetDAO assetDAO;
    private final PlaybookDAO playbookDAO;
    private final IncidentDAO incidentDAO;
    private final EvidenceDAO evidenceDAO;
    private final AuditLogDAO auditLogDAO;

    public DatabaseSeeder() {
        this.userDAO = new UserDAO();
        this.assetDAO = new AssetDAO();
        this.playbookDAO = new PlaybookDAO();
        this.incidentDAO = new IncidentDAO();
        this.evidenceDAO = new EvidenceDAO();
        this.auditLogDAO = new AuditLogDAO();
    }

    public DatabaseSeeder(UserDAO userDAO, AssetDAO assetDAO, PlaybookDAO playbookDAO,
                          IncidentDAO incidentDAO, EvidenceDAO evidenceDAO, AuditLogDAO auditLogDAO) {
        this.userDAO = userDAO;
        this.assetDAO = assetDAO;
        this.playbookDAO = playbookDAO;
        this.incidentDAO = incidentDAO;
        this.evidenceDAO = evidenceDAO;
        this.auditLogDAO = auditLogDAO;
    }

    public void seedIfEmpty() {
        try {
            if (userDAO.count() > 0) {
                LOGGER.info("Database already contains data; skipping seeding.");
                return;
            }

            LOGGER.info("Seeding database with baseline DFIR data...");

            // 1. Seed Users
            User u1 = userDAO.create(new User("Alice Walker", UserRole.SENIOR_ANALYST, "alice.walker@soc.org"));
            User u2 = userDAO.create(new User("Bob Jenkins", UserRole.ANALYST, "bob.jenkins@soc.org"));
            User u3 = userDAO.create(new User("Dana Scully", UserRole.ADMIN, "dana.scully@soc.org"));

            // 2. Seed Assets
            Asset a1 = assetDAO.create(new Asset("DC-CORP-01", "10.0.0.10", CriticalityTier.CRITICAL, AssetStatus.ONLINE, "Primary Active Directory Domain Controller"));
            Asset a2 = assetDAO.create(new Asset("PROD-WEB-APP", "10.0.1.5", CriticalityTier.HIGH, AssetStatus.ONLINE, "Customer-Facing Payment Portal"));
            Asset a3 = assetDAO.create(new Asset("FIN-WORKSTATION-07", "10.0.2.45", CriticalityTier.MEDIUM, AssetStatus.ISOLATED, "Finance Department Workstation"));
            Asset a4 = assetDAO.create(new Asset("DB-MASTER-01", "10.0.0.20", CriticalityTier.CRITICAL, AssetStatus.ONLINE, "Core PostgreSQL Customer Database"));

            // 3. Seed Playbooks & Steps
            Playbook pbRansomware = playbookDAO.createPlaybook(new Playbook(
                    "RANSOMWARE",
                    "Standard response procedure for active ransomware infections, credential dumping, and lateral movement."
            ));
            playbookDAO.createStep(new PlaybookStep(pbRansomware.getId(), PlaybookPhase.TRIAGE, 1, "Verify Threat Indicator Hash", "VERIFY_HASH", "Malware hash confirmed against Threat Intel feeds"));
            playbookDAO.createStep(new PlaybookStep(pbRansomware.getId(), PlaybookPhase.CONTAINMENT, 2, "Isolate Infected Host", "ISOLATE_HOST", "Host network adapter shifted to isolated VLAN"));
            playbookDAO.createStep(new PlaybookStep(pbRansomware.getId(), PlaybookPhase.CONTAINMENT, 3, "Block C2 IP on Perimeter", "BLOCK_IP", "Firewall egress rule dropped connections to C2"));
            playbookDAO.createStep(new PlaybookStep(pbRansomware.getId(), PlaybookPhase.EVIDENCE_COLLECTION, 4, "Acquire Live Volatile RAM", "ACQUIRE_ARTIFACT", "Memory dump extracted with cryptographic hash logged"));
            playbookDAO.createStep(new PlaybookStep(pbRansomware.getId(), PlaybookPhase.ERADICATION, 5, "Terminate Malicious Processes", "TERMINATE_PROCESS", "Identified encryptor processes killed and persistence wiped"));
            playbookDAO.createStep(new PlaybookStep(pbRansomware.getId(), PlaybookPhase.POST_MORTEM, 6, "Root Cause Analysis & Sign-off", "MANUAL_REVIEW", "Initial infection vector documented and case closed"));

            Playbook pbPhishing = playbookDAO.createPlaybook(new Playbook(
                    "PHISHING",
                    "Rapid response playbook for malicious email lures, credential harvesting, and compromised tokens."
            ));
            playbookDAO.createStep(new PlaybookStep(pbPhishing.getId(), PlaybookPhase.TRIAGE, 1, "Analyze Email Headers & DKIM", "MANUAL_REVIEW", "Header spoofing verified and reputation checked"));
            playbookDAO.createStep(new PlaybookStep(pbPhishing.getId(), PlaybookPhase.CONTAINMENT, 2, "Revoke Compromised Credentials", "REVOKE_CREDENTIALS", "Active Directory account reset and OAuth refresh tokens invalidated"));
            playbookDAO.createStep(new PlaybookStep(pbPhishing.getId(), PlaybookPhase.CONTAINMENT, 3, "Block Phishing Domain / IP", "BLOCK_IP", "Perimeter DNS sinkhole updated with harvest domain"));
            playbookDAO.createStep(new PlaybookStep(pbPhishing.getId(), PlaybookPhase.EVIDENCE_COLLECTION, 4, "Export RFC 822 EML File", "ACQUIRE_ARTIFACT", "Original raw email preserved in chain of custody"));
            playbookDAO.createStep(new PlaybookStep(pbPhishing.getId(), PlaybookPhase.POST_MORTEM, 5, "Deliver User Security Training", "MANUAL_REVIEW", "Simulated phishing coaching assigned to targeted user"));

            // 4. Seed Incidents
            Incident inc1 = new Incident(
                    "LockBit 3.0 Ransomware Activity on Finance Workstation",
                    "RANSOMWARE",
                    IncidentSeverity.CRITICAL,
                    a3.getId(),
                    u1.getId(),
                    pbRansomware.getId()
            );
            inc1.setStatus(IncidentStatus.TRIAGED);
            inc1.setCurrentPhase(PlaybookPhase.CONTAINMENT);
            inc1.setRiskScore(85.0);
            inc1 = incidentDAO.create(inc1);

            Incident inc2 = new Incident(
                    "Executive Spear-Phishing & Credential Harvest Campaign",
                    "PHISHING",
                    IncidentSeverity.HIGH,
                    a2.getId(),
                    u2.getId(),
                    pbPhishing.getId()
            );
            inc2.setStatus(IncidentStatus.NEW);
            inc2.setCurrentPhase(PlaybookPhase.TRIAGE);
            inc2.setRiskScore(60.0);
            inc2 = incidentDAO.create(inc2);

            Incident inc3 = new Incident(
                    "Suspicious Kerberoasting & Golden Ticket Attempt",
                    "RANSOMWARE",
                    IncidentSeverity.HIGH,
                    a1.getId(),
                    u1.getId(),
                    pbRansomware.getId()
            );
            inc3.setStatus(IncidentStatus.CONTAINED);
            inc3.setCurrentPhase(PlaybookPhase.EVIDENCE_COLLECTION);
            inc3.setRiskScore(78.0);
            inc3 = incidentDAO.create(inc3);

            // 5. Seed Evidence Items
            evidenceDAO.create(new EvidenceItem(
                    inc1.getId(),
                    a3.getId(),
                    "RAM_Dump_FIN-07_20260907.raw",
                    "MEMORY_DUMP",
                    "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                    u1.getId()
            ));

            EvidenceItem ev2 = new EvidenceItem(
                    inc1.getId(),
                    a3.getId(),
                    "Disk_C_Triage_Forensic.E01",
                    "DISK_IMAGE",
                    "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                    u2.getId()
            );
            ev2.setCustodyStatus(CustodyStatus.IN_ANALYSIS);
            evidenceDAO.create(ev2);

            EvidenceItem ev3 = new EvidenceItem(
                    inc2.getId(),
                    a2.getId(),
                    "Suspicious_Invoice_Phish.eml",
                    "LOG_FILE",
                    "2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae",
                    u3.getId()
            );
            ev3.setCustodyStatus(CustodyStatus.COURT_HOLD);
            evidenceDAO.create(ev3);

            // 6. Seed Action Audit Logs
            auditLogDAO.create(new ActionAuditLog(
                    inc1.getId(),
                    "ISOLATE_HOST",
                    "{\"hostname\":\"FIN-WORKSTATION-07\",\"ip\":\"10.0.2.45\",\"status\":\"ISOLATED\"}",
                    u1.getId(),
                    true,
                    AuditStatus.EXECUTED
            ));

            auditLogDAO.create(new ActionAuditLog(
                    inc1.getId(),
                    "BLOCK_IP",
                    "{\"ip\":\"198.51.100.23\",\"direction\":\"EGRESS\",\"firewall\":\"PaloAlto-Edge\"}",
                    u1.getId(),
                    true,
                    AuditStatus.EXECUTED
            ));

            auditLogDAO.create(new ActionAuditLog(
                    inc2.getId(),
                    "REVOKE_CREDENTIALS",
                    "{\"user\":\"ceo.office@corp.domain\",\"scope\":\"ALL_ACTIVE_SESSIONS\"}",
                    u2.getId(),
                    false,
                    AuditStatus.EXECUTED
            ));

            LOGGER.info("Database seeding completed successfully!");
        } catch (SQLException e) {
            LOGGER.severe("Error while seeding database: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
