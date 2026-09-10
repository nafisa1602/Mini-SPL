package com.minispl.presentation;

import com.minispl.application.incident.IncidentStateMachine;
import com.minispl.application.observer.IncidentEvent;
import com.minispl.application.observer.IncidentEventListener;
import com.minispl.application.observer.IncidentEventPublisher;
import com.minispl.application.remediation.*;
import com.minispl.application.strategy.CvssRiskScoringStrategy;
import com.minispl.application.strategy.NistRiskScoringStrategy;
import com.minispl.application.strategy.RiskScoringContext;
import com.minispl.application.strategy.RiskScoringStrategy;
import com.minispl.domain.enums.IncidentSeverity;
import com.minispl.domain.enums.IncidentStatus;
import com.minispl.domain.enums.PlaybookPhase;
import com.minispl.domain.model.Asset;
import com.minispl.domain.model.Incident;
import com.minispl.domain.model.Playbook;
import com.minispl.domain.model.User;
import com.minispl.persistence.dao.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IncidentViewController implements Refreshable, IncidentEventListener {

    @FXML private Label lblTotal;
    @FXML private Label lblNew;
    @FXML private Label lblTriaged;
    @FXML private Label lblContained;
    @FXML private Label lblClosed;

    @FXML private ComboBox<String> cbFilter;
    @FXML private TableView<Incident> tblIncidents;
    @FXML private TableColumn<Incident, Integer> colId;
    @FXML private TableColumn<Incident, String> colTitle;
    @FXML private TableColumn<Incident, String> colThreat;
    @FXML private TableColumn<Incident, IncidentSeverity> colSeverity;
    @FXML private TableColumn<Incident, IncidentStatus> colStatus;
    @FXML private TableColumn<Incident, PlaybookPhase> colPhase;
    @FXML private TableColumn<Incident, String> colAsset;
    @FXML private TableColumn<Incident, String> colAnalyst;
    @FXML private TableColumn<Incident, Double> colRiskScore;

    @FXML private VBox detailPane;
    @FXML private Label lblDetailTitle;
    @FXML private Label lblDetailSeverity;
    @FXML private Label lblDetailStatus;
    @FXML private Label lblDetailPhase;
    @FXML private Label lblDetailAsset;
    @FXML private Label lblDetailAnalyst;
    @FXML private Label lblDetailRiskScore;
    @FXML private Label lblDetailCreatedAt;
    @FXML private Label lblStatusMessage;

    // State Pattern Transition Controls
    @FXML private HBox stateTransitionBox;
    @FXML private Button btnTriage;
    @FXML private Button btnContain;
    @FXML private Button btnClose;
    @FXML private Label lblClosedNotice;

    // Command Pattern Controls
    @FXML private Button btnIsolateHost;
    @FXML private Button btnBlockIP;
    @FXML private Button btnRevokeCreds;
    @FXML private Button btnUndoCommand;
    @FXML private Button btnDelete;

    private final IncidentDAO incidentDAO = new IncidentDAO();
    private final AssetDAO assetDAO = new AssetDAO();
    private final UserDAO userDAO = new UserDAO();
    private final PlaybookDAO playbookDAO = new PlaybookDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private final RemediationCommandFactory commandFactory = new RemediationCommandFactory(assetDAO);
    private final CommandInvoker commandInvoker = new CommandInvoker(auditLogDAO, commandFactory);
    private final RiskScoringContext riskContext = new RiskScoringContext();

    private final ObservableList<Incident> incidentList = FXCollections.observableArrayList();
    private Incident selectedIncident;

    @FXML
    public void initialize() {
        cbFilter.setItems(FXCollections.observableArrayList("ALL", "NEW", "TRIAGED", "CONTAINED", "CLOSED"));
        cbFilter.setValue("ALL");
        cbFilter.setOnAction(e -> applyFilter());

        colId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()).asObject());
        colTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        colThreat.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getThreatType()));
        colSeverity.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getSeverity()));
        colStatus.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStatus()));
        colPhase.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getCurrentPhase()));
        colAsset.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getAssetHostname() != null ? data.getValue().getAssetHostname() : "Asset #" + data.getValue().getAssetId()));
        colAnalyst.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getAnalystName() != null ? data.getValue().getAnalystName() : "Unassigned"));
        colRiskScore.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getRiskScore()).asObject());

        tblIncidents.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> showDetail(newVal));
        tblIncidents.setItems(incidentList);

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
            int total = incidentDAO.count();
            int cNew = incidentDAO.countByStatus(IncidentStatus.NEW);
            int cTriaged = incidentDAO.countByStatus(IncidentStatus.TRIAGED);
            int cContained = incidentDAO.countByStatus(IncidentStatus.CONTAINED);
            int cClosed = incidentDAO.countByStatus(IncidentStatus.CLOSED);

            lblTotal.setText(String.valueOf(total));
            lblNew.setText(String.valueOf(cNew));
            lblTriaged.setText(String.valueOf(cTriaged));
            lblContained.setText(String.valueOf(cContained));
            lblClosed.setText(String.valueOf(cClosed));

            applyFilter();
        } catch (SQLException e) {
            showError("Database Error", "Failed to load incidents: " + e.getMessage());
        }
    }

    private void applyFilter() {
        try {
            int selectedId = selectedIncident != null ? selectedIncident.getId() : -1;
            String filter = cbFilter.getValue();
            List<Incident> list;
            if (filter == null || "ALL".equalsIgnoreCase(filter)) {
                list = incidentDAO.findAll();
            } else {
                list = incidentDAO.findByStatus(IncidentStatus.valueOf(filter));
            }
            incidentList.setAll(list);

            // Re-select prior selection or first item
            if (!incidentList.isEmpty()) {
                Incident toSelect = incidentList.stream().filter(i -> i.getId() == selectedId).findFirst().orElse(incidentList.get(0));
                tblIncidents.getSelectionModel().select(toSelect);
            } else {
                showDetail(null);
            }
        } catch (SQLException e) {
            showError("Database Error", e.getMessage());
        }
    }

    private void showDetail(Incident inc) {
        this.selectedIncident = inc;
        if (inc == null) {
            lblDetailTitle.setText("No incident selected");
            lblDetailSeverity.setText("-");
            lblDetailStatus.setText("-");
            lblDetailPhase.setText("-");
            lblDetailAsset.setText("-");
            lblDetailAnalyst.setText("-");
            lblDetailRiskScore.setText("-");
            lblDetailCreatedAt.setText("-");
            lblStatusMessage.setText("");

            btnTriage.setVisible(false);
            btnTriage.setManaged(false);
            btnContain.setVisible(false);
            btnContain.setManaged(false);
            btnClose.setVisible(false);
            btnClose.setManaged(false);
            lblClosedNotice.setVisible(false);
            lblClosedNotice.setManaged(false);

            btnIsolateHost.setDisable(true);
            btnBlockIP.setDisable(true);
            btnRevokeCreds.setDisable(true);
            btnUndoCommand.setDisable(!commandInvoker.canUndo());
            btnDelete.setDisable(true);
            return;
        }

        lblDetailTitle.setText("INC-" + inc.getId() + ": " + inc.getTitle());
        lblDetailSeverity.setText(inc.getSeverity().name());
        lblDetailStatus.setText(inc.getStatus().name());
        lblDetailPhase.setText(inc.getCurrentPhase().name());
        lblDetailAsset.setText(inc.getAssetHostname() != null ? inc.getAssetHostname() : "Asset #" + inc.getAssetId());
        lblDetailAnalyst.setText(inc.getAnalystName() != null ? inc.getAnalystName() : "Unassigned");
        lblDetailRiskScore.setText(String.format("%.1f", inc.getRiskScore()));
        lblDetailCreatedAt.setText(inc.getCreatedAt() != null ? inc.getCreatedAt().toString().replace("T", " ") : "-");

        // STATE PATTERN: Query IncidentStateMachine for valid transitions only!
        IncidentStateMachine stateMachine = new IncidentStateMachine(inc, incidentDAO);
        List<IncidentStatus> validTransitions = stateMachine.getValidTransitions();

        btnTriage.setVisible(validTransitions.contains(IncidentStatus.TRIAGED));
        btnTriage.setManaged(btnTriage.isVisible());

        btnContain.setVisible(validTransitions.contains(IncidentStatus.CONTAINED));
        btnContain.setManaged(btnContain.isVisible());

        btnClose.setVisible(validTransitions.contains(IncidentStatus.CLOSED));
        btnClose.setManaged(btnClose.isVisible());

        boolean isClosed = (inc.getStatus() == IncidentStatus.CLOSED);
        lblClosedNotice.setVisible(isClosed);
        lblClosedNotice.setManaged(isClosed);

        btnIsolateHost.setDisable(isClosed);
        btnBlockIP.setDisable(isClosed);
        btnRevokeCreds.setDisable(isClosed);
        btnUndoCommand.setDisable(!commandInvoker.canUndo());
        btnDelete.setDisable(false);
    }

    // ==========================================
    // State Pattern Transition Handlers
    // ==========================================

    @FXML
    public void handleTriageTransition() {
        if (selectedIncident == null) return;
        try {
            IncidentStateMachine machine = new IncidentStateMachine(selectedIncident, incidentDAO);
            machine.triage(selectedIncident.getPlaybookId(), selectedIncident.getAssignedAnalystId());
            lblStatusMessage.setText("✓ State Transition: NEW -> TRIAGED (Phase: CONTAINMENT)");
            refresh();
        } catch (Exception e) {
            showError("State Machine Error", e.getMessage());
        }
    }

    @FXML
    public void handleContainTransition() {
        if (selectedIncident == null) return;
        try {
            IncidentStateMachine machine = new IncidentStateMachine(selectedIncident, incidentDAO);
            machine.contain();
            lblStatusMessage.setText("✓ State Transition: TRIAGED -> CONTAINED (Phase: EVIDENCE_COLLECTION)");
            refresh();
        } catch (Exception e) {
            showError("State Machine Error", e.getMessage());
        }
    }

    @FXML
    public void handleCloseTransition() {
        if (selectedIncident == null) return;
        try {
            IncidentStateMachine machine = new IncidentStateMachine(selectedIncident, incidentDAO);
            machine.close();
            lblStatusMessage.setText("✓ State Transition: CONTAINED -> CLOSED (Case Resolved)");
            refresh();
        } catch (Exception e) {
            showError("State Machine Error", e.getMessage());
        }
    }

    // ==========================================
    // Incident CRUD: Create & Delete
    // ==========================================

    @FXML
    public void handleCreateIncident() {
        try {
            List<Asset> assets = assetDAO.findAll();
            List<User> users = userDAO.findAll();
            List<Playbook> playbooks = playbookDAO.findAll();

            if (assets.isEmpty()) {
                showError("Validation", "No assets found in database. Create an asset first.");
                return;
            }

            Dialog<Incident> dialog = new Dialog<>();
            dialog.setTitle("Log New Security Incident");
            dialog.setHeaderText("Create Incident & Calculate Risk Score (Strategy Pattern)");

            ButtonType saveButtonType = new ButtonType("Save Incident", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            TextField txtTitle = new TextField();
            txtTitle.setPromptText("e.g. Cobalt Strike Beaconing Detected");
            txtTitle.setPrefWidth(320);

            ComboBox<String> cbThreat = new ComboBox<>(FXCollections.observableArrayList(
                    "RANSOMWARE", "PHISHING", "MALWARE", "DATA_EXFILTRATION", "BRUTE_FORCE", "LATERAL_MOVEMENT"
            ));
            cbThreat.setValue("RANSOMWARE");

            ComboBox<Asset> cbAsset = new ComboBox<>(FXCollections.observableArrayList(assets));
            cbAsset.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Asset item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getHostname() + " (" + item.getCriticalityTier() + ")");
                }
            });
            cbAsset.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Asset item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getHostname() + " (" + item.getCriticalityTier() + ")");
                }
            });
            cbAsset.setValue(assets.get(0));

            ComboBox<User> cbAnalyst = new ComboBox<>(FXCollections.observableArrayList(users));
            cbAnalyst.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(User item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getFullName() + " (" + item.getRole() + ")");
                }
            });
            cbAnalyst.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(User item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getFullName() + " (" + item.getRole() + ")");
                }
            });
            if (!users.isEmpty()) cbAnalyst.setValue(users.get(0));

            ComboBox<IncidentSeverity> cbSeverity = new ComboBox<>(FXCollections.observableArrayList(IncidentSeverity.values()));
            cbSeverity.setValue(IncidentSeverity.HIGH);

            // Strategy pattern selector
            RiskScoringStrategy nistStrategy = new NistRiskScoringStrategy();
            RiskScoringStrategy cvssStrategy = new CvssRiskScoringStrategy();
            ComboBox<RiskScoringStrategy> cbStrategy = new ComboBox<>(FXCollections.observableArrayList(nistStrategy, cvssStrategy));
            cbStrategy.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(RiskScoringStrategy item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName() + " Strategy");
                }
            });
            cbStrategy.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(RiskScoringStrategy item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName() + " Strategy");
                }
            });
            cbStrategy.setValue(nistStrategy);

            Label lblRiskPreview = new Label("70.0");
            lblRiskPreview.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");

            // Dynamic risk calculation listener
            Runnable updateRiskPreview = () -> {
                Asset a = cbAsset.getValue();
                Incident dummy = new Incident(txtTitle.getText(), cbThreat.getValue(), cbSeverity.getValue(),
                        a != null ? a.getId() : 1, null, null);
                RiskScoringStrategy strat = cbStrategy.getValue();
                double score = strat != null ? strat.calculateRiskScore(dummy, a) : 50.0;
                lblRiskPreview.setText(String.format("%.1f", score));
            };

            cbAsset.setOnAction(e -> updateRiskPreview.run());
            cbThreat.setOnAction(e -> updateRiskPreview.run());
            cbSeverity.setOnAction(e -> updateRiskPreview.run());
            cbStrategy.setOnAction(e -> updateRiskPreview.run());
            updateRiskPreview.run();

            grid.add(new Label("Incident Title:"), 0, 0);
            grid.add(txtTitle, 1, 0);
            grid.add(new Label("Threat Vector:"), 0, 1);
            grid.add(cbThreat, 1, 1);
            grid.add(new Label("Target Asset:"), 0, 2);
            grid.add(cbAsset, 1, 2);
            grid.add(new Label("Lead Handler:"), 0, 3);
            grid.add(cbAnalyst, 1, 3);
            grid.add(new Label("Severity:"), 0, 4);
            grid.add(cbSeverity, 1, 4);
            grid.add(new Label("Risk Strategy:"), 0, 5);
            grid.add(cbStrategy, 1, 5);
            grid.add(new Label("Computed Risk Score:"), 0, 6);
            grid.add(lblRiskPreview, 1, 6);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    if (txtTitle.getText().isBlank()) return null;
                    Asset a = cbAsset.getValue();
                    User u = cbAnalyst.getValue();
                    Incident inc = new Incident(
                            txtTitle.getText().trim(),
                            cbThreat.getValue(),
                            cbSeverity.getValue(),
                            a.getId(),
                            u != null ? u.getId() : null,
                            null
                    );
                    RiskScoringStrategy strat = cbStrategy.getValue();
                    double score = strat.calculateRiskScore(inc, a);
                    inc.setRiskScore(score);
                    inc.setStatus(IncidentStatus.NEW);
                    inc.setCurrentPhase(PlaybookPhase.TRIAGE);
                    return inc;
                }
                return null;
            });

            Optional<Incident> result = dialog.showAndWait();
            result.ifPresent(inc -> {
                try {
                    Incident created = incidentDAO.create(inc);
                    lblStatusMessage.setText("✓ Created Incident INC-" + created.getId() + " with Risk Score " + created.getRiskScore());
                    IncidentEventPublisher.getInstance().publish(new IncidentEvent(
                            IncidentEvent.EventType.INCIDENT_CREATED,
                            created.getId(),
                            null,
                            created.getStatus().name(),
                            created.getAssignedAnalystId(),
                            "Created incident: " + created.getTitle()
                    ));
                    loadData();
                    tblIncidents.getSelectionModel().select(created);
                } catch (SQLException ex) {
                    showError("Creation Failed", ex.getMessage());
                }
            });

        } catch (SQLException e) {
            showError("Database Error", e.getMessage());
        }
    }

    @FXML
    public void handleDeleteIncident() {
        if (selectedIncident == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Incident");
        alert.setHeaderText("Delete INC-" + selectedIncident.getId() + ": " + selectedIncident.getTitle() + "?");
        alert.setContentText("This will cascade and delete associated evidence items and action audit logs.");

        Optional<ButtonType> resp = alert.showAndWait();
        if (resp.isPresent() && resp.get() == ButtonType.OK) {
            try {
                boolean ok = incidentDAO.delete(selectedIncident.getId());
                if (ok) {
                    IncidentEventPublisher.getInstance().publish(new IncidentEvent(
                            IncidentEvent.EventType.INCIDENT_DELETED,
                            selectedIncident.getId(),
                            selectedIncident.getStatus().name(),
                            null,
                            selectedIncident.getAssignedAnalystId(),
                            "Deleted incident: " + selectedIncident.getTitle()
                    ));
                    lblStatusMessage.setText("✓ Deleted Incident INC-" + selectedIncident.getId());
                    loadData();
                }
            } catch (SQLException e) {
                showError("Delete Error", e.getMessage());
            }
        }
    }

    // ==========================================
    // Command Pattern: Remediation Actions
    // ==========================================

    @FXML
    public void handleIsolateHostCommand() {
        if (selectedIncident == null) return;
        try {
            int assetId = selectedIncident.getAssetId();
            String host = selectedIncident.getAssetHostname() != null ? selectedIncident.getAssetHostname() : "HOST-" + assetId;
            int analystId = selectedIncident.getAssignedAnalystId() != null ? selectedIncident.getAssignedAnalystId() : 1;

            Map<String, String> params = new HashMap<>();
            params.put("asset_id", String.valueOf(assetId));
            params.put("hostname", host);

            RemediationCommand cmd = commandFactory.createCommand(IsolateHostCommand.COMMAND_TYPE, selectedIncident.getId(), analystId, params);
            commandInvoker.execute(cmd);

            lblStatusMessage.setText("✓ Action Executed: " + cmd.getStatusMessage());
            btnUndoCommand.setDisable(false);
            refresh();
        } catch (Exception e) {
            showError("Remediation Command Error", e.getMessage());
        }
    }

    @FXML
    public void handleBlockIPCommand() {
        if (selectedIncident == null) return;
        TextInputDialog dialog = new TextInputDialog("198.51.100." + (int)(Math.random() * 200 + 10));
        dialog.setTitle("Block Malicious IP Address");
        dialog.setHeaderText("Simulate Firewall Rule Injection (BLOCK_IP Command)");
        dialog.setContentText("Target Threat IP:");

        Optional<String> res = dialog.showAndWait();
        res.ifPresent(ip -> {
            try {
                int analystId = selectedIncident.getAssignedAnalystId() != null ? selectedIncident.getAssignedAnalystId() : 1;
                Map<String, String> params = new HashMap<>();
                params.put("ip", ip);
                params.put("direction", "EGRESS");
                params.put("firewall", "Edge-Firewall-01");

                RemediationCommand cmd = commandFactory.createCommand(BlockIPCommand.COMMAND_TYPE, selectedIncident.getId(), analystId, params);
                commandInvoker.execute(cmd);

                lblStatusMessage.setText("✓ Action Executed: " + cmd.getStatusMessage());
                btnUndoCommand.setDisable(false);
                refresh();
            } catch (Exception e) {
                showError("Remediation Error", e.getMessage());
            }
        });
    }

    @FXML
    public void handleRevokeCredsCommand() {
        if (selectedIncident == null) return;
        TextInputDialog dialog = new TextInputDialog("compromised.account@soc.org");
        dialog.setTitle("Revoke User Credentials");
        dialog.setHeaderText("Simulate Identity Access Revocation (REVOKE_CREDENTIALS Command)");
        dialog.setContentText("User Email / Kerberos Account:");

        Optional<String> res = dialog.showAndWait();
        res.ifPresent(account -> {
            try {
                int analystId = selectedIncident.getAssignedAnalystId() != null ? selectedIncident.getAssignedAnalystId() : 1;
                Map<String, String> params = new HashMap<>();
                params.put("account", account);
                params.put("scope", "ALL_ACTIVE_SESSIONS");

                RemediationCommand cmd = commandFactory.createCommand(RevokeCredentialsCommand.COMMAND_TYPE, selectedIncident.getId(), analystId, params);
                commandInvoker.execute(cmd);

                lblStatusMessage.setText("✓ Action Executed: " + cmd.getStatusMessage());
                btnUndoCommand.setDisable(false);
                refresh();
            } catch (Exception e) {
                showError("Remediation Error", e.getMessage());
            }
        });
    }

    @FXML
    public void handleUndoCommand() {
        try {
            RemediationCommand undone = commandInvoker.undoLast();
            lblStatusMessage.setText("↺ Rollback Executed: " + undone.getStatusMessage());
            btnUndoCommand.setDisable(!commandInvoker.canUndo());
            refresh();
        } catch (Exception e) {
            showError("Rollback Error", e.getMessage());
        }
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("DFIR Orchestrator");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
