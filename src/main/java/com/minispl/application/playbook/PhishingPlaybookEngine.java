package com.minispl.application.playbook;

import com.minispl.domain.enums.PlaybookPhase;
import com.minispl.domain.model.Incident;
import com.minispl.persistence.dao.IncidentDAO;

/**
 * Concrete Template Method subclass: PhishingPlaybookEngine.
 * Implements phishing-specific credential revocation, mailbox eradication,
 * and email artifact evidence preservation procedures.
 */
public class PhishingPlaybookEngine extends InvestigationEngine {

    public PhishingPlaybookEngine() {
        super();
    }

    public PhishingPlaybookEngine(IncidentDAO incidentDAO) {
        super(incidentDAO);
    }

    @Override
    public String getThreatType() {
        return "PHISHING";
    }

    @Override
    protected InvestigationStepResult executeTriage(Incident incident) {
        return new InvestigationStepResult(
                PlaybookPhase.TRIAGE,
                "Analyze Email Headers & DKIM/SPF Authentication",
                "Header Forensics & Sender Reputation Check",
                true,
                "Email authentication failed (SPF spoofing, invalid DKIM signature); sender IP flagged on threat feeds."
        );
    }

    @Override
    protected InvestigationStepResult executeContainment(Incident incident) {
        return new InvestigationStepResult(
                PlaybookPhase.CONTAINMENT,
                "Revoke Compromised Credentials & Sinkhole Phishing Domain",
                "Account Reset & DNS Perimeter Sinkhole",
                true,
                "Compromised user credentials revoked; active session tokens invalidated; phishing lure domain sinkholed."
        );
    }

    @Override
    protected InvestigationStepResult executeEvidenceCollection(Incident incident) {
        return new InvestigationStepResult(
                PlaybookPhase.EVIDENCE_COLLECTION,
                "Export Raw RFC 822 Message & Extract Hashes",
                "Catalog EML Artifact into Evidence Vault",
                true,
                "Original .eml message artifact extracted; cryptographic SHA-256 hash registered in chain of custody."
        );
    }

    @Override
    protected InvestigationStepResult executeEradication(Incident incident) {
        return new InvestigationStepResult(
                PlaybookPhase.ERADICATION,
                "Purge Tenant Inboxes & Invalidate Third-Party Tokens",
                "Mailbox Sweep & OAuth Consent Removal",
                true,
                "Malicious campaign lures purged across all enterprise inboxes; rogue application permissions revoked."
        );
    }

    @Override
    protected InvestigationStepResult executePostMortem(Incident incident) {
        return new InvestigationStepResult(
                PlaybookPhase.POST_MORTEM,
                "User Awareness Coaching & Gateway Rule Tuning",
                "Post-Mortem Review & Security Coaching",
                true,
                "Targeted anti-phishing simulation coaching assigned; gateway mail filters hardened against lookalike domains."
        );
    }
}
