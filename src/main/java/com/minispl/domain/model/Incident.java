package com.minispl.domain.model;

import com.minispl.domain.enums.IncidentSeverity;
import com.minispl.domain.enums.IncidentStatus;
import com.minispl.domain.enums.PlaybookPhase;
import java.time.LocalDateTime;

public class Incident {
    private int id;
    private String title;
    private String threatType;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private int assetId;
    private Integer assignedAnalystId;
    private Integer playbookId;
    private double riskScore;
    private PlaybookPhase currentPhase;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;

    // Joined / Presentation fields
    private String assetHostname;
    private String analystName;
    private String playbookName;

    public Incident() {}

    public Incident(int id, String title, String threatType, IncidentSeverity severity,
                    IncidentStatus status, int assetId, Integer assignedAnalystId,
                    Integer playbookId, double riskScore, PlaybookPhase currentPhase,
                    LocalDateTime createdAt, LocalDateTime closedAt) {
        this.id = id;
        this.title = title;
        this.threatType = threatType;
        this.severity = severity;
        this.status = status;
        this.assetId = assetId;
        this.assignedAnalystId = assignedAnalystId;
        this.playbookId = playbookId;
        this.riskScore = riskScore;
        this.currentPhase = currentPhase;
        this.createdAt = createdAt;
        this.closedAt = closedAt;
    }

    public Incident(String title, String threatType, IncidentSeverity severity,
                    int assetId, Integer assignedAnalystId, Integer playbookId) {
        this.title = title;
        this.threatType = threatType;
        this.severity = severity;
        this.status = IncidentStatus.NEW;
        this.assetId = assetId;
        this.assignedAnalystId = assignedAnalystId;
        this.playbookId = playbookId;
        this.riskScore = 0.0;
        this.currentPhase = PlaybookPhase.TRIAGE;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getThreatType() {
        return threatType;
    }

    public void setThreatType(String threatType) {
        this.threatType = threatType;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(IncidentSeverity severity) {
        this.severity = severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    public Integer getAssignedAnalystId() {
        return assignedAnalystId;
    }

    public void setAssignedAnalystId(Integer assignedAnalystId) {
        this.assignedAnalystId = assignedAnalystId;
    }

    public Integer getPlaybookId() {
        return playbookId;
    }

    public void setPlaybookId(Integer playbookId) {
        this.playbookId = playbookId;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public PlaybookPhase getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(PlaybookPhase currentPhase) {
        this.currentPhase = currentPhase;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getAssetHostname() {
        return assetHostname;
    }

    public void setAssetHostname(String assetHostname) {
        this.assetHostname = assetHostname;
    }

    public String getAnalystName() {
        return analystName;
    }

    public void setAnalystName(String analystName) {
        this.analystName = analystName;
    }

    public String getPlaybookName() {
        return playbookName;
    }

    public void setPlaybookName(String playbookName) {
        this.playbookName = playbookName;
    }

    @Override
    public String toString() {
        return "INC-" + id + ": " + title + " [" + status + "]";
    }
}
