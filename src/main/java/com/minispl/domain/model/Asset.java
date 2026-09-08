package com.minispl.domain.model;

import com.minispl.domain.enums.AssetStatus;
import com.minispl.domain.enums.CriticalityTier;

public class Asset {
    private int id;
    private String hostname;
    private String ipAddress;
    private CriticalityTier criticalityTier;
    private AssetStatus status;
    private String description;

    public Asset() {}

    public Asset(int id, String hostname, String ipAddress, CriticalityTier criticalityTier, AssetStatus status, String description) {
        this.id = id;
        this.hostname = hostname;
        this.ipAddress = ipAddress;
        this.criticalityTier = criticalityTier;
        this.status = status;
        this.description = description;
    }

    public Asset(String hostname, String ipAddress, CriticalityTier criticalityTier, AssetStatus status, String description) {
        this.hostname = hostname;
        this.ipAddress = ipAddress;
        this.criticalityTier = criticalityTier;
        this.status = status;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public CriticalityTier getCriticalityTier() {
        return criticalityTier;
    }

    public void setCriticalityTier(CriticalityTier criticalityTier) {
        this.criticalityTier = criticalityTier;
    }

    public AssetStatus getStatus() {
        return status;
    }

    public void setStatus(AssetStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return hostname + " (" + ipAddress + " - " + criticalityTier + ")";
    }
}
