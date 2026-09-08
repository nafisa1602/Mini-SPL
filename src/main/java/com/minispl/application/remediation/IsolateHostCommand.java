package com.minispl.application.remediation;

import com.minispl.domain.enums.AssetStatus;
import com.minispl.domain.model.Asset;
import com.minispl.persistence.dao.AssetDAO;

import java.sql.SQLException;

/**
 * Concrete Command: Isolate Host from Network.
 * Sets the affected asset status to ISOLATED in SQLite.
 * On undo, restores the asset back to its previous operational status (ONLINE).
 */
public class IsolateHostCommand extends AbstractRemediationCommand {

    public static final String COMMAND_TYPE = "ISOLATE_HOST";

    private final int assetId;
    private final String hostname;
    private final AssetDAO assetDAO;
    private AssetStatus previousStatus = AssetStatus.ONLINE;

    public IsolateHostCommand(int incidentId, int executedById, int assetId, String hostname, AssetDAO assetDAO) {
        super(incidentId, executedById);
        this.assetId = assetId;
        this.hostname = hostname;
        this.assetDAO = assetDAO != null ? assetDAO : new AssetDAO();
    }

    @Override
    public void execute() throws SQLException {
        // Record current status before isolating
        Asset asset = assetDAO.findById(assetId).orElse(null);
        if (asset != null) {
            this.previousStatus = asset.getStatus();
        }

        // Apply network isolation
        boolean ok = assetDAO.updateStatus(assetId, AssetStatus.ISOLATED);
        if (!ok) {
            throw new SQLException("Failed to isolate host: Asset ID " + assetId + " not found or update failed.");
        }

        this.executed = true;
        this.undone = false;
        this.statusMessage = String.format("Host [%s] (ID %d) isolated from production network VLAN. Status: ISOLATED.", hostname, assetId);
    }

    @Override
    public void undo() throws SQLException {
        if (!executed) {
            throw new IllegalStateException("Cannot undo a command that has not been executed.");
        }
        if (undone) {
            throw new IllegalStateException("Command has already been rolled back.");
        }

        // Restore network connectivity to prior status
        boolean ok = assetDAO.updateStatus(assetId, previousStatus);
        if (!ok) {
            throw new SQLException("Failed to rollback host isolation: Asset ID " + assetId + " update failed.");
        }

        this.undone = true;
        this.statusMessage = String.format("Host [%s] network isolation rolled back. Status restored to %s.", hostname, previousStatus);
    }

    @Override
    public boolean canUndo() {
        return true;
    }

    @Override
    public String getCommandType() {
        return COMMAND_TYPE;
    }

    @Override
    public String getParameters() {
        return String.format("{\"asset_id\":%d,\"hostname\":\"%s\",\"target_status\":\"ISOLATED\"}", assetId, hostname);
    }

    public int getAssetId() {
        return assetId;
    }

    public String getHostname() {
        return hostname;
    }
}
