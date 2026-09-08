package com.minispl.domain.model;

import com.minispl.domain.enums.PlaybookPhase;

public class PlaybookStep {
    private int id;
    private int playbookId;
    private PlaybookPhase phase;
    private int sequenceOrder;
    private String stepName;
    private String actionType;
    private String passFailCriteria;

    public PlaybookStep() {}

    public PlaybookStep(int id, int playbookId, PlaybookPhase phase, int sequenceOrder, String stepName, String actionType, String passFailCriteria) {
        this.id = id;
        this.playbookId = playbookId;
        this.phase = phase;
        this.sequenceOrder = sequenceOrder;
        this.stepName = stepName;
        this.actionType = actionType;
        this.passFailCriteria = passFailCriteria;
    }

    public PlaybookStep(int playbookId, PlaybookPhase phase, int sequenceOrder, String stepName, String actionType, String passFailCriteria) {
        this.playbookId = playbookId;
        this.phase = phase;
        this.sequenceOrder = sequenceOrder;
        this.stepName = stepName;
        this.actionType = actionType;
        this.passFailCriteria = passFailCriteria;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPlaybookId() {
        return playbookId;
    }

    public void setPlaybookId(int playbookId) {
        this.playbookId = playbookId;
    }

    public PlaybookPhase getPhase() {
        return phase;
    }

    public void setPhase(PlaybookPhase phase) {
        this.phase = phase;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }

    public void setSequenceOrder(int sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getPassFailCriteria() {
        return passFailCriteria;
    }

    public void setPassFailCriteria(String passFailCriteria) {
        this.passFailCriteria = passFailCriteria;
    }

    @Override
    public String toString() {
        return sequenceOrder + ". [" + phase + "] " + stepName;
    }
}
