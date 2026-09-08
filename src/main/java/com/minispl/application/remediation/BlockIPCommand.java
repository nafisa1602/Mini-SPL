package com.minispl.application.remediation;

/**
 * Concrete Command: Block Attacker IP on Perimeter Firewall.
 * Injects a high-priority packet drop rule for C2 / threat actor IP address.
 * On undo, withdraws the firewall ACL drop rule.
 */
public class BlockIPCommand extends AbstractRemediationCommand {

    public static final String COMMAND_TYPE = "BLOCK_IP";

    private final String ipAddress;
    private final String direction;
    private final String firewall;
    private String ruleIdentifier;

    public BlockIPCommand(int incidentId, int executedById, String ipAddress, String direction, String firewall) {
        super(incidentId, executedById);
        this.ipAddress = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress.trim() : "198.51.100.23";
        this.direction = (direction != null && !direction.isBlank()) ? direction : "EGRESS";
        this.firewall = (firewall != null && !firewall.isBlank()) ? firewall : "PaloAlto-Edge";
    }

    @Override
    public void execute() {
        this.ruleIdentifier = "FW-DROP-" + ipAddress.replace(".", "-") + "-" + System.currentTimeMillis() % 10000;
        this.executed = true;
        this.undone = false;
        this.statusMessage = String.format("Blocked IP [%s] on firewall [%s] (%s rule: %s).",
                ipAddress, firewall, direction, ruleIdentifier);
    }

    @Override
    public void undo() {
        if (!executed) {
            throw new IllegalStateException("Cannot undo a command that has not been executed.");
        }
        if (undone) {
            throw new IllegalStateException("Command has already been rolled back.");
        }

        this.undone = true;
        this.statusMessage = String.format("Rule [%s] withdrawn from firewall [%s]. Traffic to [%s] unblocked.",
                ruleIdentifier, firewall, ipAddress);
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
        return String.format("{\"ip\":\"%s\",\"direction\":\"%s\",\"firewall\":\"%s\",\"rule\":\"%s\"}",
                ipAddress, direction, firewall, ruleIdentifier != null ? ruleIdentifier : "PENDING");
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getDirection() {
        return direction;
    }

    public String getFirewall() {
        return firewall;
    }

    public String getRuleIdentifier() {
        return ruleIdentifier;
    }
}
