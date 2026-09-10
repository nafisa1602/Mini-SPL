package com.minispl.application.playbook;

import com.minispl.domain.enums.PlaybookPhase;
import java.time.LocalDateTime;

/**
 * Result model representing the outcome of executing an individual phase within
 * an InvestigationEngine template method execution.
 */
public class InvestigationStepResult {

    private final PlaybookPhase phase;
    private final String stepName;
    private final String actionTaken;
    private final boolean success;
    private final String outputSummary;
    private final LocalDateTime timestamp;

    public InvestigationStepResult(PlaybookPhase phase, String stepName, String actionTaken,
                                   boolean success, String outputSummary) {
        this.phase = phase;
        this.stepName = stepName;
        this.actionTaken = actionTaken;
        this.success = success;
        this.outputSummary = outputSummary;
        this.timestamp = LocalDateTime.now();
    }

    public PlaybookPhase getPhase() {
        return phase;
    }

    public String getStepName() {
        return stepName;
    }

    public String getActionTaken() {
        return actionTaken;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getOutputSummary() {
        return outputSummary;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "[" + phase + "] " + stepName + ": " + (success ? "SUCCESS" : "FAILED") + " - " + outputSummary;
    }
}
