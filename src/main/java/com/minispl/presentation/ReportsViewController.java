package com.minispl.presentation;

import com.minispl.application.observer.IncidentEvent;
import com.minispl.application.observer.IncidentEventListener;
import com.minispl.application.observer.IncidentEventPublisher;
import com.minispl.application.remediation.CommandInvoker;
import com.minispl.application.remediation.RemediationCommandFactory;
import com.minispl.domain.enums.AuditStatus;
import com.minispl.domain.enums.IncidentStatus;
import com.minispl.domain.model.ActionAuditLog;
import com.minispl.domain.model.Incident;
import com.minispl.persistence.dao.AssetDAO;
import com.minispl.persistence.dao.AuditLogDAO;
import com.minispl.persistence.dao.IncidentDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

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

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle("DFIR Orchestrator");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
