package com.minispl.application.evidence;

import com.minispl.domain.enums.CustodyStatus;

import java.util.Collections;
import java.util.List;

/**
 * Concrete State: ARCHIVED evidence.
 * Permanently deposited in cold forensic evidence repository.
 * Terminal state; custody cannot be modified further.
 */
public class ArchivedCustodyState implements CustodyState {

    @Override
    public CustodyStatus getStatus() {
        return CustodyStatus.ARCHIVED;
    }

    @Override
    public String getDisplayName() {
        return "Archived (Cold Vault Storage)";
    }

    @Override
    public void beginAnalysis(EvidenceCustodyStateMachine context, int custodianId) {
        throw new IllegalStateException("Archived evidence cannot re-enter analysis. Chain-of-custody seal is permanent.");
    }

    @Override
    public void placeOnCourtHold(EvidenceCustodyStateMachine context, int custodianId) {
        throw new IllegalStateException("Cannot place ARCHIVED evidence on Court Hold. It is permanently vaulted.");
    }

    @Override
    public void releaseFromCourtHold(EvidenceCustodyStateMachine context, int custodianId) {
        throw new IllegalStateException("Archived evidence is not on Court Hold.");
    }

    @Override
    public void archive(EvidenceCustodyStateMachine context, int custodianId) {
        throw new IllegalStateException("Evidence is already in ARCHIVED status.");
    }

    @Override
    public List<CustodyStatus> getValidTransitions() {
        return Collections.emptyList();
    }
}
