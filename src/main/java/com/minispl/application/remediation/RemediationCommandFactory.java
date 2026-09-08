package com.minispl.application.remediation;

import com.minispl.persistence.dao.AssetDAO;

import java.util.Map;

/**
 * Factory Pattern for Remediation Commands.
 * Instantiates the appropriate RemediationCommand subclass at runtime
 * based on action type requested by the analyst or triggered by playbook steps.
 */
public class RemediationCommandFactory {

    private final AssetDAO assetDAO;

    public RemediationCommandFactory() {
        this.assetDAO = new AssetDAO();
    }

    public RemediationCommandFactory(AssetDAO assetDAO) {
        this.assetDAO = assetDAO != null ? assetDAO : new AssetDAO();
    }

    /**
     * Factory Method to create a concrete RemediationCommand.
     *
     * @param commandType Type of action (e.g. "ISOLATE_HOST", "BLOCK_IP", "REVOKE_CREDENTIALS")
     * @param incidentId Incident being remediated
     * @param executorId Analyst triggering the action
     * @param params Parameter map containing action-specific arguments
     * @return Concrete RemediationCommand instance
     */
    public RemediationCommand createCommand(String commandType, int incidentId, int executorId, Map<String, String> params) {
        if (commandType == null || commandType.isBlank()) {
            throw new IllegalArgumentException("Command type cannot be null or empty");
        }

        String normalized = commandType.trim().toUpperCase();
        return switch (normalized) {
            case IsolateHostCommand.COMMAND_TYPE, "ISOLATE" -> {
                int assetId = 1;
                String hostname = "TARGET-HOST";
                if (params != null) {
                    if (params.containsKey("asset_id")) {
                        try { assetId = Integer.parseInt(params.get("asset_id")); } catch (NumberFormatException ignored) {}
                    }
                    if (params.containsKey("hostname")) {
                        hostname = params.get("hostname");
                    }
                }
                yield new IsolateHostCommand(incidentId, executorId, assetId, hostname, assetDAO);
            }

            case BlockIPCommand.COMMAND_TYPE, "BLOCK" -> {
                String ip = "198.51.100.23";
                String direction = "EGRESS";
                String fw = "PaloAlto-Edge";
                if (params != null) {
                    if (params.containsKey("ip")) ip = params.get("ip");
                    if (params.containsKey("direction")) direction = params.get("direction");
                    if (params.containsKey("firewall")) fw = params.get("firewall");
                }
                yield new BlockIPCommand(incidentId, executorId, ip, direction, fw);
            }

            case RevokeCredentialsCommand.COMMAND_TYPE, "REVOKE" -> {
                String account = "victim.user@corp.org";
                String scope = "ALL_ACTIVE_SESSIONS";
                if (params != null) {
                    if (params.containsKey("account")) account = params.get("account");
                    if (params.containsKey("scope")) scope = params.get("scope");
                }
                yield new RevokeCredentialsCommand(incidentId, executorId, account, scope);
            }

            default -> throw new IllegalArgumentException("Unsupported command type: " + commandType);
        };
    }
}
