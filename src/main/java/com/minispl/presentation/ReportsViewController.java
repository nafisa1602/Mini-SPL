package com.minispl.presentation;

import com.minispl.application.remediation.CommandInvoker;
import com.minispl.application.remediation.RemediationCommandFactory;
import com.minispl.domain.enums.AuditStatus;
import com.minispl.domain.model.ActionAuditLog;
import com.minispl.persistence.dao.AssetDAO;
import com.minispl.persistence.dao.AuditLogDAO;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ReportsViewController {

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
        loadData();
    }

    @FXML
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
            lblAvgContainTime.setText("42.5 mins (Simulated MTTC)");

            if (!logs.isEmpty()) {
                ActionAuditLog toSelect = logs.stream().filter(l -> l.getId() == selectedId).findFirst().orElse(logs.get(0));
                tblAuditLogs.getSelectionModel().select(toSelect);
            }
        } catch (SQLException e) {
            lblRollbackMessage.setText("Error loading audit logs: " + e.getMessage());
        }
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
