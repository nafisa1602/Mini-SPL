package com.minispl.application.evidence;

import com.minispl.domain.enums.CustodyStatus;
import com.minispl.domain.model.EvidenceItem;

import java.sql.SQLException;
import java.util.List;

/**
 * Concrete State: COURT_HOLD evidence.
 * Subject to judicial protective order or trial disclosure freeze.
 * Valid transitions: -> IN_ANALYSIS (if hold is lifted), -> ARCHIVED (trial concluded).
 */
public class CourtHoldCustodyState implements CustodyState {

    @Override
    public CustodyStatus getStatus() {
        return CustodyStatus.COURT_HOLD;
    }

    @Override
    public String getDisplayName() {
        return "Court Hold (Judicial & Trial Freeze)";
    }

    @Override
    public void beginAnalysis(EvidenceCustodyStateMachine context, int custodianId) {
        throw new IllegalStateException("Evidence is under active Court Hold. You must release from Court Hold before returning to analysis.");
    }

    @Override
    public void placeOnCourtHold(EvidenceCustodyStateMachine context, int custodianId) {
        throw new IllegalStateException("Evidence is already under active Court Hold.");
    }

    @Override
    public void releaseFromCourtHold(EvidenceCustodyStateMachine context, int custodianId) throws SQLException {
        EvidenceItem item = context.getItem();
        item.setCustodyStatus(CustodyStatus.IN_ANALYSIS);
        item.setCurrentCustodianId(custodianId);

        context.getEvidenceDAO().updateCustody(item.getId(), CustodyStatus.IN_ANALYSIS, custodianId);
        context.setState(new InAnalysisCustodyState());
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
        return List.of(CustodyStatus.IN_ANALYSIS, CustodyStatus.ARCHIVED);
    }
}
