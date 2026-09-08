package com.minispl.application.remediation;

/**
 * Concrete Command: Revoke User Credentials and Active Sessions.
 * Invalidates kerberos / OAuth / VPN tokens for compromised accounts.
 * On undo, re-enables the account and sends a secure temporary reactivation token.
 */
public class RevokeCredentialsCommand extends AbstractRemediationCommand {

    public static final String COMMAND_TYPE = "REVOKE_CREDENTIALS";

    private final String targetAccount;
    private final String scope;

    public RevokeCredentialsCommand(int incidentId, int executedById, String targetAccount, String scope) {
        super(incidentId, executedById);
        this.targetAccount = (targetAccount != null && !targetAccount.isBlank()) ? targetAccount.trim() : "compromised.user@corp.org";
        this.scope = (scope != null && !scope.isBlank()) ? scope : "ALL_ACTIVE_SESSIONS";
    }

    @Override
    public void execute() {
        this.executed = true;
        this.undone = false;
        this.statusMessage = String.format("Revoked credentials for [%s]. Active tokens invalidated (Scope: %s).",
                targetAccount, scope);
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
        this.statusMessage = String.format("Account [%s] access unlocked and temporary recovery token generated.",
                targetAccount);
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
        return String.format("{\"account\":\"%s\",\"scope\":\"%s\"}", targetAccount, scope);
    }

    public String getTargetAccount() {
        return targetAccount;
    }

    public String getScope() {
        return scope;
    }
}
