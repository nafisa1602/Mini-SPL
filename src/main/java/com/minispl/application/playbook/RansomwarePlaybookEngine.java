package com.minispl.application.playbook;

import com.minispl.domain.enums.PlaybookPhase;
import com.minispl.domain.model.Incident;
import com.minispl.persistence.dao.IncidentDAO;

/**
 * Concrete Template Method subclass: RansomwarePlaybookEngine.
 * Implements ransomware-specific containment, volatile artifact preservation,
 * and malicious process eradication procedures.
 */
public class RansomwarePlaybookEngine extends InvestigationEngine {

    public RansomwarePlaybookEngine() {
        super();
    }

    public RansomwarePlaybookEngine(IncidentDAO incidentDAO) {
        super(incidentDAO);
    }

    @Override
    public String getThreatType() {
        return "RANSOMWARE";
    }

    @Override
    protected InvestigationStepResult executeTriage(Incident incident) {
        return new InvestigationStepResult(
                PlaybookPhase.TRIAGE,
                "Verify Threat Indicator Hash & Ransom Note",
                "IoC Threat Intel Cross-Check",
                true,
                "Malware binary hash confirmed against Threat Intel feeds; encryptor strain identified with ransom note markers."
        );
    }

    @Override
    protected InvestigationStepResult executeContainment(Incident incident) {
        return new InvestigationStepResult(
                PlaybookPhase.CONTAINMENT,
                "Isolate Compromised Host & Sever C2 Channels",
                "VLAN Quarantine & Firewall Egress Drop",
                true,
                "Target host adapter isolated to containment VLAN; egress perimeter rules blocked active C2 destination IPs."
        );
    }

    @Override
    protected InvestigationStepResult executeEvidenceCollection(Incident incident) {
        return new InvestigationStepResult(
                PlaybookPhase.EVIDENCE_COLLECTION,
                "Acquire Volatile Memory & Shadow Copy State",
                "Extract RAM Dump & VSS Metadata",
                true,
                "Live RAM dump extracted with SHA-256 checksum recorded; Volume Shadow Copy status logged into custody."
        );
    }

    @Override
    protected InvestigationStepResult executeEradication(Incident incident) {
        return new InvestigationStepResult(
                PlaybookPhase.ERADICATION,
                "Terminate Encryptor Processes & Scrub Persistence",
                "Process Kill & Scheduled Task Deletion",
                true,
                "Malicious child processes terminated; persistence registry Run keys removed; golden image deployment scheduled."
        );
    }

    @Override
    protected InvestigationStepResult executePostMortem(Incident incident) {
        return new InvestigationStepResult(
                PlaybookPhase.POST_MORTEM,
                "Root Cause Analysis & Backup Integrity Audit",
                "Forensic Post-Mortem Sign-off",
                true,
                "Patient-zero vector isolated to exposed external service; immutable air-gapped backups verified for restore."
        );
    }
}
