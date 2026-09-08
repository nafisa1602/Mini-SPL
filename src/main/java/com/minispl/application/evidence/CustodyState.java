package com.minispl.application.evidence;

import com.minispl.domain.enums.CustodyStatus;

import java.sql.SQLException;
import java.util.List;

/**
 * State Pattern Interface for Evidence Chain of Custody.
 * Governs legal evidence lifecycle transitions:
 * SEIZED -> IN_ANALYSIS -> COURT_HOLD -> ARCHIVED
 */
public interface CustodyState {

    /**
     * Enum value corresponding to this state.
     */
    CustodyStatus getStatus();

    /**
     * Human-friendly label for display.
     */
    String getDisplayName();

    /**
     * Transfers evidence from SEIZED to IN_ANALYSIS.
     */
    void beginAnalysis(EvidenceCustodyStateMachine context, int custodianId) throws SQLException;

    /**
     * Places evidence on COURT_HOLD (subpoena/prosecution freeze).
     */
    void placeOnCourtHold(EvidenceCustodyStateMachine context, int custodianId) throws SQLException;

    /**
     * Releases evidence from COURT_HOLD back to IN_ANALYSIS.
     */
    void releaseFromCourtHold(EvidenceCustodyStateMachine context, int custodianId) throws SQLException;

    /**
     * Moves evidence to ARCHIVED (permanent retention vault).
     */
    void archive(EvidenceCustodyStateMachine context, int custodianId) throws SQLException;

    /**
     * Permitted next states from this state.
     */
    List<CustodyStatus> getValidTransitions();
}
