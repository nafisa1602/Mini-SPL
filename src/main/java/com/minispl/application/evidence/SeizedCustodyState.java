package com.minispl.application.evidence;

import com.minispl.domain.enums.CustodyStatus;
import com.minispl.domain.model.EvidenceItem;

import java.sql.SQLException;
import java.util.List;

/**
 * Concrete State: SEIZED evidence.
 * Raw artifact (disk image, RAM dump, pcap) acquired and cryptographically hashed.
 * Valid transition: -> IN_ANALYSIS.
 */
public class SeizedCustodyState implements CustodyState {

    @Override
    public CustodyStatus getStatus() {
        return CustodyStatus.SEIZED;
    }

    @Override
    public String getDisplayName() {
        return "Seized (Intake & Hash Verified)";
    }

    @Override
    public void beginAnalysis(EvidenceCustodyStateMachine context, int custodianId) throws SQLException {
        EvidenceItem item = context.getItem();
        item.setCustodyStatus(CustodyStatus.IN_ANALYSIS);
        item.setCurrentCustodianId(custodianId);

        context.getEvidenceDAO().updateCustody(item.getId(), CustodyStatus.IN_ANALYSIS, custodianId);
        context.setState(new InAnalysisCustodyState());
    }

    @Override
    public void placeOnCourtHold(EvidenceCustodyStateMachine context, int custodianId) {
        throw new IllegalStateException("Cannot place SEIZED evidence directly on Court Hold. It must first enter IN_ANALYSIS for intake forensic verification.");
    }

    @Override
    public void releaseFromCourtHold(EvidenceCustodyStateMachine context, int custodianId) {
        throw new IllegalStateException("Evidence is not currently on Court Hold.");
    }

    @Override
    public void archive(EvidenceCustodyStateMachine context, int custodianId) {
        throw new IllegalStateException("Cannot archive SEIZED evidence without examination and chain-of-custody logging.");
    }

    @Override
    public List<CustodyStatus> getValidTransitions() {
        return List.of(CustodyStatus.IN_ANALYSIS);
    }
}
