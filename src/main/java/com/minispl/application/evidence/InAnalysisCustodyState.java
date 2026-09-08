package com.minispl.application.evidence;

import com.minispl.domain.enums.CustodyStatus;
import com.minispl.domain.model.EvidenceItem;

import java.sql.SQLException;
import java.util.List;

/**
 * Concrete State: IN_ANALYSIS evidence.
 * Forensic examiner is actively examining image or extracting indicators.
 * Valid transitions: -> COURT_HOLD (legal subpoena freeze), -> ARCHIVED (investigation complete).
 */
public class InAnalysisCustodyState implements CustodyState {

    @Override
    public CustodyStatus getStatus() {
        return CustodyStatus.IN_ANALYSIS;
    }

    @Override
    public String getDisplayName() {
        return "In Analysis (Forensic Lab Processing)";
    }

    @Override
    public void beginAnalysis(EvidenceCustodyStateMachine context, int custodianId) {
        throw new IllegalStateException("Evidence is already in IN_ANALYSIS state.");
    }

    @Override
    public void placeOnCourtHold(EvidenceCustodyStateMachine context, int custodianId) throws SQLException {
        EvidenceItem item = context.getItem();
        item.setCustodyStatus(CustodyStatus.COURT_HOLD);
        item.setCurrentCustodianId(custodianId);

        context.getEvidenceDAO().updateCustody(item.getId(), CustodyStatus.COURT_HOLD, custodianId);
        context.setState(new CourtHoldCustodyState());
    }

    @Override
    public void releaseFromCourtHold(EvidenceCustodyStateMachine context, int custodianId) {
        throw new IllegalStateException("Evidence is not on Court Hold.");
    }

    @Override
    public void archive(EvidenceCustodyStateMachine context, int custodianId) throws SQLException {
        EvidenceItem item = context.getItem();
        item.setCustodyStatus(CustodyStatus.ARCHIVED);
        item.setCurrentCustodianId(custodianId);

        context.getEvidenceDAO().updateCustody(item.getId(), CustodyStatus.ARCHIVED, custodianId);
        context.setState(new ArchivedCustodyState());
    }

    @Override
    public List<CustodyStatus> getValidTransitions() {
        return List.of(CustodyStatus.COURT_HOLD, CustodyStatus.ARCHIVED);
    }
}
