package com.minispl.application.playbook;

import com.minispl.application.observer.IncidentEvent;
import com.minispl.application.observer.IncidentEventPublisher;
import com.minispl.domain.enums.IncidentStatus;
import com.minispl.domain.enums.PlaybookPhase;
import com.minispl.domain.model.Incident;
import com.minispl.persistence.dao.IncidentDAO;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Template Method Pattern: InvestigationEngine (Abstract Class).
 *
 * Defines the invariant DFIR investigation and remediation lifecycle:
 * Triage -> Containment -> Evidence Collection -> Eradication -> Post-Mortem.
 *
 * Subclasses (such as RansomwarePlaybookEngine and PhishingPlaybookEngine) provide
 * threat-specific implementations for each concrete phase without altering the
 * invariant workflow execution sequence.
 */
public abstract class InvestigationEngine {

    protected static final Logger LOGGER = Logger.getLogger(InvestigationEngine.class.getName());

    protected final IncidentDAO incidentDAO;

    public InvestigationEngine() {
        this(new IncidentDAO());
    }

    public InvestigationEngine(IncidentDAO incidentDAO) {
        this.incidentDAO = incidentDAO;
    }

    /**
     * Returns the threat type handled by this playbook engine (e.g. "RANSOMWARE", "PHISHING").
     */
    public abstract String getThreatType();

    /**
     * TEMPLATE METHOD: Enforces the invariant 5-phase investigation lifecycle.
     * Declared final so subclasses cannot override the execution order.
     */
    public final InvestigationReport executePlaybook(Incident incident) {
        if (incident == null) {
            throw new IllegalArgumentException("Incident entity cannot be null for playbook execution.");
        }

        InvestigationReport report = new InvestigationReport(incident.getId(), getThreatType());

        // Pre-investigation hook
        beforeInvestigation(incident, report);

        // Phase 1: TRIAGE
        updateIncidentPhase(incident, PlaybookPhase.TRIAGE);
        InvestigationStepResult triageResult = executeTriage(incident);
        report.addStepResult(triageResult);
        onPhaseCompleted(incident, PlaybookPhase.TRIAGE, triageResult);
        if (!triageResult.isSuccess()) {
            report.markCompleted();
            return report;
        }

        // Phase 2: CONTAINMENT
        updateIncidentPhase(incident, PlaybookPhase.CONTAINMENT);
        InvestigationStepResult containResult = executeContainment(incident);
        report.addStepResult(containResult);
        onPhaseCompleted(incident, PlaybookPhase.CONTAINMENT, containResult);
        if (!containResult.isSuccess()) {
            report.markCompleted();
            return report;
        }

        // Phase 3: EVIDENCE COLLECTION
        updateIncidentPhase(incident, PlaybookPhase.EVIDENCE_COLLECTION);
        InvestigationStepResult evidenceResult = executeEvidenceCollection(incident);
        report.addStepResult(evidenceResult);
        onPhaseCompleted(incident, PlaybookPhase.EVIDENCE_COLLECTION, evidenceResult);
        if (!evidenceResult.isSuccess()) {
            report.markCompleted();
            return report;
        }

        // Phase 4: ERADICATION
        updateIncidentPhase(incident, PlaybookPhase.ERADICATION);
        InvestigationStepResult eradicateResult = executeEradication(incident);
        report.addStepResult(eradicateResult);
        onPhaseCompleted(incident, PlaybookPhase.ERADICATION, eradicateResult);
        if (!eradicateResult.isSuccess()) {
            report.markCompleted();
            return report;
        }

        // Phase 5: POST-MORTEM
        updateIncidentPhase(incident, PlaybookPhase.POST_MORTEM);
        InvestigationStepResult postMortemResult = executePostMortem(incident);
        report.addStepResult(postMortemResult);
        onPhaseCompleted(incident, PlaybookPhase.POST_MORTEM, postMortemResult);

        // Finalize lifecycle
        report.markCompleted();
        if (report.isFullyCompleted()) {
            incident.setCurrentPhase(PlaybookPhase.CLOSED);
            incident.setStatus(IncidentStatus.CLOSED);
            if (incidentDAO != null && incident.getId() > 0) {
                try {
                    incidentDAO.closeIncident(incident.getId());
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Failed to close incident in DB: " + e.getMessage(), e);
                }
            }
        }

        // Post-investigation hook
        afterInvestigation(incident, report);

        // Publish observer event
        IncidentEventPublisher.getInstance().publish(new IncidentEvent(
                IncidentEvent.EventType.PLAYBOOK_EXECUTED,
                incident.getId(),
                PlaybookPhase.TRIAGE.name(),
                PlaybookPhase.CLOSED.name(),
                incident.getAssignedAnalystId(),
                String.format("Executed %s playbook: 5/5 phases completed", getThreatType())
        ));

        return report;
    }

    /**
     * Alias for executePlaybook to support either naming convention.
     */
    public final InvestigationReport executeInvestigation(Incident incident) {
        return executePlaybook(incident);
    }

    /**
     * Executes a single phase in isolation for interactive analyst step-through.
     */
    public InvestigationStepResult executePhase(PlaybookPhase phase, Incident incident) {
        if (phase == null || incident == null) {
            throw new IllegalArgumentException("Phase and incident cannot be null.");
        }
        updateIncidentPhase(incident, phase);
        InvestigationStepResult result = switch (phase) {
            case TRIAGE -> executeTriage(incident);
            case CONTAINMENT -> executeContainment(incident);
            case EVIDENCE_COLLECTION -> executeEvidenceCollection(incident);
            case ERADICATION -> executeEradication(incident);
            case POST_MORTEM -> executePostMortem(incident);
            case CLOSED -> new InvestigationStepResult(PlaybookPhase.CLOSED, "Case Closure", "Archive", true, "Investigation already concluded.");
        };
        onPhaseCompleted(incident, phase, result);
        return result;
    }

    // ==========================================
    // Primitive Operations (Mandatory Subclass Implementation)
    // ==========================================

    protected abstract InvestigationStepResult executeTriage(Incident incident);

    protected abstract InvestigationStepResult executeContainment(Incident incident);

    protected abstract InvestigationStepResult executeEvidenceCollection(Incident incident);

    protected abstract InvestigationStepResult executeEradication(Incident incident);

    protected abstract InvestigationStepResult executePostMortem(Incident incident);

    // ==========================================
    // Hook Methods (Optional Subclass Override)
    // ==========================================

    protected void beforeInvestigation(Incident incident, InvestigationReport report) {
        LOGGER.info(String.format("Starting %s investigation workflow for Incident #%d", getThreatType(), incident.getId()));
    }

    protected void onPhaseCompleted(Incident incident, PlaybookPhase phase, InvestigationStepResult result) {
        LOGGER.info(String.format("Phase [%s] completed for Incident #%d: %s", phase, incident.getId(), result.isSuccess() ? "PASS" : "FAIL"));
    }

    protected void afterInvestigation(Incident incident, InvestigationReport report) {
        LOGGER.info(String.format("Finished %s investigation workflow for Incident #%d (Success: %s)",
                getThreatType(), incident.getId(), report.isFullyCompleted()));
    }

    private void updateIncidentPhase(Incident incident, PlaybookPhase phase) {
        incident.setCurrentPhase(phase);
        if (incidentDAO != null && incident.getId() > 0) {
            try {
                incidentDAO.updatePhase(incident.getId(), phase);
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Could not update incident phase in DB: " + e.getMessage(), e);
            }
        }
    }
}
