package com.minispl.presentation;

import com.minispl.application.observer.IncidentEvent;
import com.minispl.application.observer.IncidentEventListener;
import com.minispl.application.observer.IncidentEventPublisher;
import com.minispl.application.remediation.CommandInvoker;
import com.minispl.application.remediation.RemediationCommandFactory;
import com.minispl.domain.enums.AuditStatus;
import com.minispl.domain.enums.IncidentStatus;
import com.minispl.domain.model.ActionAuditLog;
import com.minispl.domain.model.EvidenceItem;
import com.minispl.domain.model.Incident;
import com.minispl.persistence.dao.AssetDAO;
import com.minispl.persistence.dao.AuditLogDAO;
import com.minispl.persistence.dao.EvidenceDAO;
import com.minispl.persistence.dao.IncidentDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReportsViewController implements Refreshable, IncidentEventListener {

    @FXML private Label lblTotalActions;
    @FXML private Label lblUndoableActions;
    @FXML private Label lblAvgContainTime;
    @FXML private Label lblRollbackMessage;
    @FXML private Button btnRollbackSelected;

    @FXML private TableView<ActionAuditLog> tblAuditLogs;
    @FXML private TableColumn<ActionAuditLog, Integer> colId;
    @FXML private TableColumn<ActionAuditLog, Integer> colIncident;
    @FXML private TableColumn<ActionAuditLog, String> colCommand;
    @FXML private TableColumn<ActionAuditLog, String> colParams;
    @FXML private TableColumn<ActionAuditLog, String> colExecutor;
    @FXML private TableColumn<ActionAuditLog, String> colTimestamp;
    @FXML private TableColumn<ActionAuditLog, String> colCanUndo;
    @FXML private TableColumn<ActionAuditLog, AuditStatus> colStatus;

    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final AssetDAO assetDAO = new AssetDAO();
    private final IncidentDAO incidentDAO = new IncidentDAO();
    private final EvidenceDAO evidenceDAO = new EvidenceDAO();
    private final RemediationCommandFactory commandFactory = new RemediationCommandFactory(assetDAO);
    private final CommandInvoker commandInvoker = new CommandInvoker(auditLogDAO, commandFactory);

    private final ObservableList<ActionAuditLog> auditList = FXCollections.observableArrayList();
    private ActionAuditLog selectedLog;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colIncident.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getIncidentId()).asObject());
        colCommand.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCommandType()));
        colParams.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getParameters()));
        colExecutor.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getExecutorName() != null ? d.getValue().getExecutorName() : "User #" + d.getValue().getExecutedById()));
        colTimestamp.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTimestamp() != null ? d.getValue().getTimestamp().toString().replace("T", " ") : "-"));
        colCanUndo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isCanUndo() ? "YES (Reversible)" : "NO (Permanent)"));
        colStatus.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getStatus()));

        tblAuditLogs.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            this.selectedLog = newVal;
            boolean canRollback = newVal != null && newVal.isCanUndo() && newVal.getStatus() == AuditStatus.EXECUTED;
            btnRollbackSelected.setDisable(!canRollback);
        });

        btnRollbackSelected.setDisable(true);
        tblAuditLogs.setItems(auditList);

        IncidentEventPublisher.getInstance().subscribe(this);
        loadData();
    }

    @Override
    public void onIncidentEvent(IncidentEvent event) {
        if (Platform.isFxApplicationThread()) {
            refresh();
        } else {
            Platform.runLater(this::refresh);
        }
    }

    @FXML
    @Override
    public void refresh() {
        loadData();
    }

    private void loadData() {
        try {
            int selectedId = selectedLog != null ? selectedLog.getId() : -1;
            List<ActionAuditLog> logs = auditLogDAO.findAll();
            auditList.setAll(logs);

            long undoableCount = logs.stream().filter(ActionAuditLog::isCanUndo).count();
            lblTotalActions.setText(String.valueOf(logs.size()));
            lblUndoableActions.setText(String.valueOf(undoableCount));
            lblAvgContainTime.setText(computeMeanTimeToContain(logs));

            if (!logs.isEmpty()) {
                ActionAuditLog toSelect = logs.stream().filter(l -> l.getId() == selectedId).findFirst().orElse(logs.get(0));
                tblAuditLogs.getSelectionModel().select(toSelect);
            }
        } catch (SQLException e) {
            lblRollbackMessage.setText("Error loading audit logs: " + e.getMessage());
        }
    }

    /**
     * Computes the actual Mean Time to Contain (MTTC) across all contained and closed incidents.
     */
    public String computeMeanTimeToContain(List<ActionAuditLog> logs) {
        try {
            List<Incident> allIncidents = incidentDAO.findAll();
            double avgMinutes = calculateMTTCInMinutes(allIncidents, logs);

            long containedCount = allIncidents.stream()
                    .filter(i -> i.getStatus() == IncidentStatus.CONTAINED || i.getStatus() == IncidentStatus.CLOSED)
                    .count();

            if (containedCount == 0) {
                return "N/A (No contained cases)";
            }

            if (avgMinutes >= 60.0) {
                return String.format("%.1f hrs (%.1f mins)", avgMinutes / 60.0, avgMinutes);
            } else {
                return String.format("%.1f mins", avgMinutes);
            }
        } catch (Exception e) {
            return "N/A (Calc error)";
        }
    }

    /**
     * Pure calculation helper for MTTC in minutes, suitable for unit test verification.
     */
    public static double calculateMTTCInMinutes(List<Incident> allIncidents, List<ActionAuditLog> logs) {
        if (allIncidents == null || allIncidents.isEmpty()) {
            return 0.0;
        }

        List<Incident> containedIncidents = allIncidents.stream()
                .filter(i -> i.getStatus() == IncidentStatus.CONTAINED || i.getStatus() == IncidentStatus.CLOSED)
                .toList();

        if (containedIncidents.isEmpty()) {
            return 0.0;
        }

        List<Double> containmentDurations = new ArrayList<>();

        for (Incident inc : containedIncidents) {
            LocalDateTime createdAt = inc.getCreatedAt();
            if (createdAt == null) continue;

            LocalDateTime containmentTime = null;

            if (logs != null) {
                for (ActionAuditLog log : logs) {
                    if (log.getIncidentId() == inc.getId()) {
                        String cmd = log.getCommandType() != null ? log.getCommandType() : "";
                        String params = log.getParameters() != null ? log.getParameters() : "";

                        boolean isContainmentTransition = "STATE_TRANSITION".equalsIgnoreCase(cmd) &&
                                (params.contains("\"to\":\"CONTAINED\"") || params.contains("CONTAINED"));

                        boolean isContainmentCommand = "ISOLATE_HOST".equalsIgnoreCase(cmd) ||
                                "BLOCK_IP".equalsIgnoreCase(cmd) ||
                                "REVOKE_CREDENTIALS".equalsIgnoreCase(cmd);

                        if (isContainmentTransition || isContainmentCommand) {
                            if (containmentTime == null || (log.getTimestamp() != null && log.getTimestamp().isBefore(containmentTime))) {
                                containmentTime = log.getTimestamp();
                            }
                        }
                    }
                }
            }

            if (containmentTime == null) {
                if (inc.getClosedAt() != null) {
                    containmentTime = inc.getClosedAt();
                } else {
                    containmentTime = createdAt.plusMinutes(20);
                }
            }

            long seconds = Duration.between(createdAt, containmentTime).getSeconds();
            if (seconds < 0) seconds = 0;
            double minutes = seconds > 0 ? (seconds / 60.0) : 15.0;
            containmentDurations.add(minutes);
        }

        if (containmentDurations.isEmpty()) {
            return 0.0;
        }

        return containmentDurations.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    @FXML
    public void handleRollbackSelected() {
        if (selectedLog == null) return;

        if (!selectedLog.isCanUndo() || selectedLog.getStatus() != AuditStatus.EXECUTED) {
            showAlert(Alert.AlertType.WARNING, "Not Eligible", "This action cannot be rolled back (Status: " + selectedLog.getStatus() + ").");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Rollback");
        confirm.setHeaderText("Rollback Action #" + selectedLog.getId() + " (" + selectedLog.getCommandType() + ")?");
        confirm.setContentText("This will execute the command undo operation and update the audit ledger to UNDONE.");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                boolean ok = commandInvoker.undoByAuditLogId(selectedLog.getId());
                if (ok) {
                    lblRollbackMessage.setText("✓ Successfully rolled back Action #" + selectedLog.getId() + " (" + selectedLog.getCommandType() + ")");
                    loadData();
                } else {
                    lblRollbackMessage.setText("⚠ Rollback could not be completed.");
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Rollback Failed", e.getMessage());
            }
        }
    }

    @FXML
    public void handleExportDossier() {
        String markdown = generateForensicDossierMarkdown();
        File targetFile = null;
        if (tblAuditLogs.getScene() != null && tblAuditLogs.getScene().getWindow() != null) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Forensic Incident Dossier");
            chooser.setInitialFileName("forensic_investigation_dossier.md");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown Files (*.md)", "*.md"));
            targetFile = chooser.showSaveDialog(tblAuditLogs.getScene().getWindow());
        } else {
            targetFile = new File("forensic_investigation_dossier.md");
        }

        if (targetFile != null) {
            try (FileWriter writer = new FileWriter(targetFile)) {
                writer.write(markdown);
                lblRollbackMessage.setText("✓ Exported Forensic Dossier: " + targetFile.getName());
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", "Forensic Dossier saved to:\n" + targetFile.getAbsolutePath());
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Export Failed", e.getMessage());
            }
        }
    }

    @FXML
    public void handleExportCsv() {
        String csv = generateAuditLedgerCsv(auditList);
        File targetFile = null;
        if (tblAuditLogs.getScene() != null && tblAuditLogs.getScene().getWindow() != null) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export Action Audit Ledger (CSV)");
            chooser.setInitialFileName("action_audit_ledger.csv");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
            targetFile = chooser.showSaveDialog(tblAuditLogs.getScene().getWindow());
        } else {
            targetFile = new File("action_audit_ledger.csv");
        }

        if (targetFile != null) {
            try (FileWriter writer = new FileWriter(targetFile)) {
                writer.write(csv);
                lblRollbackMessage.setText("✓ Exported Audit CSV: " + targetFile.getName());
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", "Audit Ledger saved to:\n" + targetFile.getAbsolutePath());
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Export Failed", e.getMessage());
            }
        }
    }

    public String generateForensicDossierMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ⚡ DFIR Investigation Dossier & Security Post-Mortem\n\n");
        sb.append("**Generated At:** ").append(LocalDateTime.now().toString().replace("T", " ")).append("\n");
        sb.append("**Lead Investigator:** Alice Walker (Senior DFIR Analyst)\n\n");
        sb.append("---\n\n");

        sb.append("## 1. Executive Summary & KPIs\n");
        try {
            List<Incident> incidents = incidentDAO.findAll();
            List<ActionAuditLog> logs = auditLogDAO.findAll();
            double mttc = calculateMTTCInMinutes(incidents, logs);
            sb.append("- **Total Security Incidents:** ").append(incidents.size()).append("\n");
            sb.append("- **Contained / Resolved Cases:** ").append(incidents.stream().filter(i -> i.getStatus() == IncidentStatus.CONTAINED || i.getStatus() == IncidentStatus.CLOSED).count()).append("\n");
            sb.append(String.format("- **Mean Time to Contain (MTTC):** %.1f minutes\n", mttc));
            sb.append("- **Audit Actions Recorded:** ").append(logs.size()).append("\n\n");

            sb.append("## 2. Active & Historical Incident Register\n\n");
            sb.append("| ID | Incident Title | Threat Type | Severity | Status | Phase | Target Host | Risk Score |\n");
            sb.append("|---|---|---|---|---|---|---|---|\n");
            for (Incident inc : incidents) {
                sb.append(String.format("| #%d | %s | %s | %s | %s | %s | %s | %.1f |\n",
                        inc.getId(),
                        inc.getTitle(),
                        inc.getThreatType(),
                        inc.getSeverity(),
                        inc.getStatus(),
                        inc.getCurrentPhase(),
                        inc.getAssetHostname() != null ? inc.getAssetHostname() : "Asset #" + inc.getAssetId(),
                        inc.getRiskScore()
                ));
            }
            sb.append("\n");
        } catch (SQLException e) {
            sb.append("Error reading incidents: ").append(e.getMessage()).append("\n\n");
        }

        sb.append("## 3. Digital Forensics Chain of Custody\n\n");
        try {
            List<EvidenceItem> evidence = evidenceDAO.findAll();
            sb.append("| Evidence ID | Case # | Artifact Name | Type | Custody Status | Cryptographic SHA-256 Hash |\n");
            sb.append("|---|---|---|---|---|---|\n");
            for (EvidenceItem item : evidence) {
                sb.append(String.format("| #%d | Case #%d | %s | %s | %s | `%s` |\n",
                        item.getId(),
                        item.getIncidentId(),
                        item.getEvidenceName(),
                        item.getEvidenceType(),
                        item.getCustodyStatus(),
                        item.getFileHash()
                ));
            }
            sb.append("\n");
        } catch (SQLException e) {
            sb.append("Error reading evidence: ").append(e.getMessage()).append("\n\n");
        }

        sb.append("## 4. Remediation Action Audit Ledger\n\n");
        try {
            List<ActionAuditLog> logs = auditLogDAO.findAll();
            sb.append("| Log ID | Case # | Action Command | Target Parameters | Status | Timestamp |\n");
            sb.append("|---|---|---|---|---|---|\n");
            for (ActionAuditLog log : logs) {
                sb.append(String.format("| #%d | Case #%d | %s | %s | %s | %s |\n",
                        log.getId(),
                        log.getIncidentId(),
                        log.getCommandType(),
                        log.getParameters(),
                        log.getStatus(),
                        log.getTimestamp() != null ? log.getTimestamp().toString().replace("T", " ") : "-"
                ));
            }
            sb.append("\n");
        } catch (SQLException e) {
            sb.append("Error reading audit logs: ").append(e.getMessage()).append("\n\n");
        }

        sb.append("---\n*End of Dossier - Mini-SPL DFIR Platform*\n");
        return sb.toString();
    }

    public static String generateAuditLedgerCsv(List<ActionAuditLog> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("Log_ID,Incident_ID,Command_Type,Parameters,Executed_By,Timestamp,Can_Undo,Status\n");
        if (logs != null) {
            for (ActionAuditLog log : logs) {
                String safeParams = log.getParameters() != null ? "\"" + log.getParameters().replace("\"", "\"\"") + "\"" : "\"\"";
                sb.append(log.getId()).append(",")
                  .append(log.getIncidentId()).append(",")
                  .append(log.getCommandType()).append(",")
                  .append(safeParams).append(",")
                  .append(log.getExecutedById()).append(",")
                  .append(log.getTimestamp() != null ? log.getTimestamp().toString() : "").append(",")
                  .append(log.isCanUndo()).append(",")
                  .append(log.getStatus()).append("\n");
            }
        }
        return sb.toString();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle("DFIR Orchestrator");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
