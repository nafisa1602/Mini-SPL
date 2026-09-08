package com.minispl.domain.model;

import com.minispl.domain.enums.CustodyStatus;
import java.time.LocalDateTime;

public class EvidenceItem {
    private int id;
    private int incidentId;
    private int sourceAssetId;
    private String evidenceName;
    private String evidenceType;
    private String fileHash;
    private CustodyStatus custodyStatus;
    private int currentCustodianId;
    private LocalDateTime collectedAt;

    // Presentation helpers
    private String sourceAssetHostname;
    private String custodianName;

    public EvidenceItem() {}

    public EvidenceItem(int id, int incidentId, int sourceAssetId, String evidenceName,
                        String evidenceType, String fileHash, CustodyStatus custodyStatus,
                        int currentCustodianId, LocalDateTime collectedAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.sourceAssetId = sourceAssetId;
        this.evidenceName = evidenceName;
        this.evidenceType = evidenceType;
        this.fileHash = fileHash;
        this.custodyStatus = custodyStatus;
        this.currentCustodianId = currentCustodianId;
        this.collectedAt = collectedAt;
    }

    public EvidenceItem(int incidentId, int sourceAssetId, String evidenceName,
                        String evidenceType, String fileHash, int currentCustodianId) {
        this.incidentId = incidentId;
        this.sourceAssetId = sourceAssetId;
        this.evidenceName = evidenceName;
        this.evidenceType = evidenceType;
        this.fileHash = fileHash;
        this.custodyStatus = CustodyStatus.SEIZED;
        this.currentCustodianId = currentCustodianId;
        this.collectedAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(int incidentId) {
        this.incidentId = incidentId;
    }

    public int getSourceAssetId() {
        return sourceAssetId;
    }

    public void setSourceAssetId(int sourceAssetId) {
        this.sourceAssetId = sourceAssetId;
    }

    public String getEvidenceName() {
        return evidenceName;
    }

    public void setEvidenceName(String evidenceName) {
        this.evidenceName = evidenceName;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public CustodyStatus getCustodyStatus() {
        return custodyStatus;
    }

    public void setCustodyStatus(CustodyStatus custodyStatus) {
        this.custodyStatus = custodyStatus;
    }

    public int getCurrentCustodianId() {
        return currentCustodianId;
    }

    public void setCurrentCustodianId(int currentCustodianId) {
        this.currentCustodianId = currentCustodianId;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }

    public String getSourceAssetHostname() {
        return sourceAssetHostname;
    }

    public void setSourceAssetHostname(String sourceAssetHostname) {
        this.sourceAssetHostname = sourceAssetHostname;
    }

    public String getCustodianName() {
        return custodianName;
    }

    public void setCustodianName(String custodianName) {
        this.custodianName = custodianName;
    }

    @Override
    public String toString() {
        return evidenceName + " [" + custodyStatus + "]";
    }
}
