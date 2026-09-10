package com.minispl.application.evidence;

import com.minispl.application.observer.IncidentEvent;
import com.minispl.application.observer.IncidentEventPublisher;
import com.minispl.domain.enums.CustodyStatus;
import com.minispl.domain.model.EvidenceItem;
import com.minispl.persistence.dao.EvidenceDAO;

import java.sql.SQLException;
import java.util.List;

/**
 * State Pattern Context: EvidenceCustodyStateMachine.
 * Wraps an EvidenceItem and delegates chain-of-custody lifecycle transitions
 * to the active CustodyState. Persists transitions to SQLite via EvidenceDAO and notifies observers.
 */
public class EvidenceCustodyStateMachine {

    private final EvidenceItem item;
    private final EvidenceDAO evidenceDAO;
    private CustodyState currentState;

    public EvidenceCustodyStateMachine(EvidenceItem item, EvidenceDAO evidenceDAO) {
        if (item == null) {
            throw new IllegalArgumentException("EvidenceItem cannot be null");
        }
        this.item = item;
        this.evidenceDAO = evidenceDAO != null ? evidenceDAO : new EvidenceDAO();
        this.currentState = resolveInitialState(item.getCustodyStatus());
    }

    private CustodyState resolveInitialState(CustodyStatus status) {
        if (status == null) return new SeizedCustodyState();
        return switch (status) {
            case SEIZED -> new SeizedCustodyState();
            case IN_ANALYSIS -> new InAnalysisCustodyState();
            case COURT_HOLD -> new CourtHoldCustodyState();
            case ARCHIVED -> new ArchivedCustodyState();
        };
    }

    public EvidenceItem getItem() {
        return item;
    }

    public EvidenceDAO getEvidenceDAO() {
        return evidenceDAO;
    }

    public CustodyState getCurrentState() {
        return currentState;
    }

    public void setState(CustodyState newState) {
        this.currentState = newState;
    }

    public CustodyStatus getStatus() {
        return currentState.getStatus();
    }

    public List<CustodyStatus> getValidTransitions() {
        return currentState.getValidTransitions();
    }

    public boolean canTransitionTo(CustodyStatus target) {
        return getValidTransitions().contains(target);
    }

    /**
     * Executes transition: SEIZED -> IN_ANALYSIS
     */
    public void beginAnalysis(int custodianId) throws SQLException {
        CustodyStatus oldStatus = getStatus();
        currentState.beginAnalysis(this, custodianId);
        IncidentEventPublisher.getInstance().publish(new IncidentEvent(
                IncidentEvent.EventType.EVIDENCE_CUSTODY_CHANGED,
                item.getIncidentId(),
                oldStatus.name(),
                getStatus().name(),
                custodianId,
                "Evidence #" + item.getId() + " (" + item.getEvidenceName() + ") moved to IN_ANALYSIS"
        ));
    }

    /**
     * Executes transition: IN_ANALYSIS -> COURT_HOLD
     */
    public void placeOnCourtHold(int custodianId) throws SQLException {
        CustodyStatus oldStatus = getStatus();
        currentState.placeOnCourtHold(this, custodianId);
        IncidentEventPublisher.getInstance().publish(new IncidentEvent(
                IncidentEvent.EventType.EVIDENCE_CUSTODY_CHANGED,
                item.getIncidentId(),
                oldStatus.name(),
                getStatus().name(),
                custodianId,
                "Evidence #" + item.getId() + " placed on COURT_HOLD (Judicial Freeze)"
        ));
    }

    /**
     * Executes transition: COURT_HOLD -> IN_ANALYSIS
     */
    public void releaseFromCourtHold(int custodianId) throws SQLException {
        CustodyStatus oldStatus = getStatus();
        currentState.releaseFromCourtHold(this, custodianId);
        IncidentEventPublisher.getInstance().publish(new IncidentEvent(
                IncidentEvent.EventType.EVIDENCE_CUSTODY_CHANGED,
                item.getIncidentId(),
                oldStatus.name(),
                getStatus().name(),
                custodianId,
                "Evidence #" + item.getId() + " released from COURT_HOLD to IN_ANALYSIS"
        ));
    }

    /**
     * Executes transition: IN_ANALYSIS or COURT_HOLD -> ARCHIVED
     */
    public void archive(int custodianId) throws SQLException {
        CustodyStatus oldStatus = getStatus();
        currentState.archive(this, custodianId);
        IncidentEventPublisher.getInstance().publish(new IncidentEvent(
                IncidentEvent.EventType.EVIDENCE_CUSTODY_CHANGED,
                item.getIncidentId(),
                oldStatus.name(),
                getStatus().name(),
                custodianId,
                "Evidence #" + item.getId() + " archived to cold storage vault"
        ));
    }
}
