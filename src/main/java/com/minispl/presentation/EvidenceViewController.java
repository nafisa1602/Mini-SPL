package com.minispl.presentation;

import com.minispl.application.evidence.EvidenceCustodyStateMachine;
import com.minispl.application.observer.IncidentEvent;
import com.minispl.application.observer.IncidentEventListener;
import com.minispl.application.observer.IncidentEventPublisher;
import com.minispl.domain.enums.CustodyStatus;
import com.minispl.domain.model.Asset;
import com.minispl.domain.model.EvidenceItem;
import com.minispl.domain.model.Incident;
import com.minispl.domain.model.User;
import com.minispl.persistence.dao.AssetDAO;
import com.minispl.persistence.dao.EvidenceDAO;
import com.minispl.persistence.dao.IncidentDAO;
import com.minispl.persistence.dao.UserDAO;
import javafx.application.Platform;
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
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class EvidenceViewController implements Refreshable, IncidentEventListener {

    @FXML private TableView<EvidenceItem> tblEvidence;
    @FXML private TableColumn<EvidenceItem, Integer> colId;
    @FXML private TableColumn<EvidenceItem, Integer> colIncident;
    @FXML private TableColumn<EvidenceItem, String> colName;
    @FXML private TableColumn<EvidenceItem, String> colType;
    @FXML private TableColumn<EvidenceItem, String> colHash;
    @FXML private TableColumn<EvidenceItem, CustodyStatus> colStatus;
    @FXML private TableColumn<EvidenceItem, String> colSourceAsset;
    @FXML private TableColumn<EvidenceItem, String> colCustodian;
    @FXML private TableColumn<EvidenceItem, String> colCollectedAt;

    @FXML private Label lblEvidenceName;
    @FXML private Label lblFullHash;
    @FXML private Label lblCustodyStatus;
    @FXML private Label lblCustodianName;
    @FXML private Label lblSourceHost;
    @FXML private Label lblTotalArtifacts;
    @FXML private Label lblCustodyMessage;

    // State Pattern Transition Controls
    @FXML private HBox custodyControlsBox;
    @FXML private Button btnBeginAnalysis;
    @FXML private Button btnPlaceCourtHold;
    @FXML private Button btnReleaseCourtHold;
    @FXML private Button btnArchiveArtifact;
    @FXML private Label lblArchivedNotice;
    @FXML private Button btnDeleteEvidence;
    @FXML private Button btnVerifyIntegrity;

    private final EvidenceDAO evidenceDAO = new EvidenceDAO();
    private final IncidentDAO incidentDAO = new IncidentDAO();
    private final AssetDAO assetDAO = new AssetDAO();
    private final UserDAO userDAO = new UserDAO();

    private final ObservableList<EvidenceItem> evidenceList = FXCollections.observableArrayList();
    private EvidenceItem selectedItem;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colIncident.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getIncidentId()).asObject());
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEvidenceName()));
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEvidenceType()));
        colHash.setCellValueFactory(d -> new SimpleStringProperty(truncateHash(d.getValue().getFileHash())));
        colStatus.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getCustodyStatus()));
        colSourceAsset.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getSourceAssetHostname() != null ? d.getValue().getSourceAssetHostname() : "Asset #" + d.getValue().getSourceAssetId()));
        colCustodian.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCustodianName() != null ? d.getValue().getCustodianName() : "User #" + d.getValue().getCurrentCustodianId()));
        colCollectedAt.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCollectedAt() != null ? d.getValue().getCollectedAt().toString().replace("T", " ") : "-"));

        tblEvidence.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> showDetail(newVal));
        tblEvidence.setItems(evidenceList);

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
            int selectedId = selectedItem != null ? selectedItem.getId() : -1;
            List<EvidenceItem> items = evidenceDAO.findAll();
            evidenceList.setAll(items);
            lblTotalArtifacts.setText("Total Preserved Artifacts: " + items.size());

            if (!items.isEmpty()) {
                EvidenceItem toSelect = items.stream().filter(i -> i.getId() == selectedId).findFirst().orElse(items.get(0));
                tblEvidence.getSelectionModel().select(toSelect);
            } else {
                showDetail(null);
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to load evidence: " + e.getMessage());
        }
    }

    private void showDetail(EvidenceItem item) {
        this.selectedItem = item;
        if (item == null) {
            lblEvidenceName.setText("No artifact selected");
            lblFullHash.setText("-");
            lblCustodyStatus.setText("-");
            lblCustodianName.setText("-");
            lblSourceHost.setText("-");
            lblCustodyMessage.setText("");

            btnBeginAnalysis.setVisible(false);
            btnBeginAnalysis.setManaged(false);
            btnPlaceCourtHold.setVisible(false);
            btnPlaceCourtHold.setManaged(false);
            btnReleaseCourtHold.setVisible(false);
            btnReleaseCourtHold.setManaged(false);
            btnArchiveArtifact.setVisible(false);
            btnArchiveArtifact.setManaged(false);
            lblArchivedNotice.setVisible(false);
            lblArchivedNotice.setManaged(false);
            btnDeleteEvidence.setDisable(true);
            return;
        }

        lblEvidenceName.setText(item.getEvidenceName() + " (" + item.getEvidenceType() + ")");
        lblFullHash.setText(item.getFileHash());
        lblCustodyStatus.setText(item.getCustodyStatus().name());
        lblCustodianName.setText(item.getCustodianName() != null ? item.getCustodianName() : "User #" + item.getCurrentCustodianId());
        lblSourceHost.setText(item.getSourceAssetHostname() != null ? item.getSourceAssetHostname() : "Asset #" + item.getSourceAssetId());

        // STATE PATTERN: Check valid transitions via EvidenceCustodyStateMachine
        EvidenceCustodyStateMachine stateMachine = new EvidenceCustodyStateMachine(item, evidenceDAO);
        List<CustodyStatus> validTransitions = stateMachine.getValidTransitions();

        btnBeginAnalysis.setVisible(validTransitions.contains(CustodyStatus.IN_ANALYSIS) && item.getCustodyStatus() == CustodyStatus.SEIZED);
        btnBeginAnalysis.setManaged(btnBeginAnalysis.isVisible());

        btnPlaceCourtHold.setVisible(validTransitions.contains(CustodyStatus.COURT_HOLD));
        btnPlaceCourtHold.setManaged(btnPlaceCourtHold.isVisible());

        btnReleaseCourtHold.setVisible(validTransitions.contains(CustodyStatus.IN_ANALYSIS) && item.getCustodyStatus() == CustodyStatus.COURT_HOLD);
        btnReleaseCourtHold.setManaged(btnReleaseCourtHold.isVisible());

        btnArchiveArtifact.setVisible(validTransitions.contains(CustodyStatus.ARCHIVED));
        btnArchiveArtifact.setManaged(btnArchiveArtifact.isVisible());

        boolean isArchived = (item.getCustodyStatus() == CustodyStatus.ARCHIVED);
        lblArchivedNotice.setVisible(isArchived);
        lblArchivedNotice.setManaged(isArchived);

        btnDeleteEvidence.setDisable(false);
    }

    // ==========================================
    // State Pattern Transition Handlers
    // ==========================================

    @FXML
    public void handleBeginAnalysis() {
        if (selectedItem == null) return;
        try {
            EvidenceCustodyStateMachine machine = new EvidenceCustodyStateMachine(selectedItem, evidenceDAO);
            machine.beginAnalysis(selectedItem.getCurrentCustodianId());
            lblCustodyMessage.setText("✓ Custody Transition: SEIZED -> IN_ANALYSIS");
            refresh();
        } catch (Exception e) {
            showError("Custody State Error", e.getMessage());
        }
    }

    @FXML
    public void handlePlaceCourtHold() {
        if (selectedItem == null) return;
        try {
            EvidenceCustodyStateMachine machine = new EvidenceCustodyStateMachine(selectedItem, evidenceDAO);
            machine.placeOnCourtHold(selectedItem.getCurrentCustodianId());
            lblCustodyMessage.setText("✓ Custody Transition: IN_ANALYSIS -> COURT_HOLD (Judicial Freeze)");
            refresh();
        } catch (Exception e) {
            showError("Custody State Error", e.getMessage());
        }
    }

    @FXML
    public void handleReleaseCourtHold() {
        if (selectedItem == null) return;
        try {
            EvidenceCustodyStateMachine machine = new EvidenceCustodyStateMachine(selectedItem, evidenceDAO);
            machine.releaseFromCourtHold(selectedItem.getCurrentCustodianId());
            lblCustodyMessage.setText("✓ Custody Transition: COURT_HOLD -> IN_ANALYSIS (Hold Released)");
            refresh();
        } catch (Exception e) {
            showError("Custody State Error", e.getMessage());
        }
    }

    @FXML
    public void handleArchiveArtifact() {
        if (selectedItem == null) return;
        try {
            EvidenceCustodyStateMachine machine = new EvidenceCustodyStateMachine(selectedItem, evidenceDAO);
            machine.archive(selectedItem.getCurrentCustodianId());
            lblCustodyMessage.setText("✓ Custody Transition: -> ARCHIVED (Cold Vault Sealed)");
            refresh();
        } catch (Exception e) {
            showError("Custody State Error", e.getMessage());
        }
    }

    // ==========================================
    // Evidence CRUD: Create & Delete
    // ==========================================

    @FXML
    public void handleCreateEvidence() {
        try {
            List<Incident> incidents = incidentDAO.findAll();
            List<Asset> assets = assetDAO.findAll();
            List<User> users = userDAO.findAll();

            if (incidents.isEmpty()) {
                showError("Validation", "No incidents available. Create an incident first.");
                return;
            }

            Dialog<EvidenceItem> dialog = new Dialog<>();
            dialog.setTitle("Catalog Digital Forensic Evidence");
            dialog.setHeaderText("Evidence CRUD: Intake Forensic Artifact with SHA-256 Hash");

            ButtonType saveBtn = new ButtonType("Catalog Artifact", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            ComboBox<Incident> cbInc = new ComboBox<>(FXCollections.observableArrayList(incidents));
            cbInc.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Incident item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : "INC-" + item.getId() + ": " + item.getTitle());
                }
            });
            cbInc.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Incident item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : "INC-" + item.getId() + ": " + item.getTitle());
                }
            });
            cbInc.setValue(incidents.get(0));

            ComboBox<Asset> cbAsset = new ComboBox<>(FXCollections.observableArrayList(assets));
            cbAsset.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Asset item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getHostname() + " (" + item.getIpAddress() + ")");
                }
            });
            cbAsset.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Asset item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getHostname() + " (" + item.getIpAddress() + ")");
                }
            });
            if (!assets.isEmpty()) cbAsset.setValue(assets.get(0));

            TextField txtName = new TextField();
            txtName.setPromptText("e.g. Memory_Dump_FIN01.raw");

            ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList(
                    "DISK_IMAGE", "MEMORY_DUMP", "NETWORK_PCAP", "LOG_FILE", "CREDENTIAL_CACHE"
            ));
            cbType.setValue("MEMORY_DUMP");

            TextField txtHash = new TextField();
            txtHash.setPromptText("SHA-256 Hash hex string");
            txtHash.setText(generateDemoSha256("artifact-" + System.currentTimeMillis()));

            Button btnGenHash = new Button("⚡ Generate Random Hash");
            btnGenHash.setStyle("-fx-font-size: 11px;");
            btnGenHash.setOnAction(e -> txtHash.setText(generateDemoSha256(txtName.getText() + System.nanoTime())));

            HBox hashBox = new HBox(8, txtHash, btnGenHash);

            ComboBox<User> cbCustodian = new ComboBox<>(FXCollections.observableArrayList(users));
            cbCustodian.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(User item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getFullName() + " (" + item.getRole() + ")");
                }
            });
            cbCustodian.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(User item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getFullName() + " (" + item.getRole() + ")");
                }
            });
            if (!users.isEmpty()) cbCustodian.setValue(users.get(0));

            grid.add(new Label("Related Incident:"), 0, 0);
            grid.add(cbInc, 1, 0);
            grid.add(new Label("Source Host Asset:"), 0, 1);
            grid.add(cbAsset, 1, 1);
            grid.add(new Label("Artifact File Name:"), 0, 2);
            grid.add(txtName, 1, 2);
            grid.add(new Label("Evidence Type:"), 0, 3);
            grid.add(cbType, 1, 3);
            grid.add(new Label("SHA-256 Hash:"), 0, 4);
            grid.add(hashBox, 1, 4);
            grid.add(new Label("Intake Custodian:"), 0, 5);
            grid.add(cbCustodian, 1, 5);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(btn -> {
                if (btn == saveBtn) {
                    if (txtName.getText().isBlank()) return null;
                    Incident inc = cbInc.getValue();
                    Asset ast = cbAsset.getValue();
                    User cst = cbCustodian.getValue();
                    EvidenceItem item = new EvidenceItem(
                            inc.getId(),
                            ast != null ? ast.getId() : 1,
                            txtName.getText().trim(),
                            cbType.getValue(),
                            txtHash.getText().trim(),
                            cst != null ? cst.getId() : 1
                    );
                    item.setCustodyStatus(CustodyStatus.SEIZED);
                    return item;
                }
                return null;
            });

            Optional<EvidenceItem> res = dialog.showAndWait();
            res.ifPresent(item -> {
                try {
                    EvidenceItem created = evidenceDAO.create(item);
                    lblCustodyMessage.setText("✓ Cataloged Evidence Item #" + created.getId() + " in SEIZED custody state.");
                    IncidentEventPublisher.getInstance().publish(new IncidentEvent(
                            IncidentEvent.EventType.EVIDENCE_CREATED,
                            created.getIncidentId(),
                            null,
                            created.getCustodyStatus().name(),
                            created.getCurrentCustodianId(),
                            "Cataloged evidence artifact: " + created.getEvidenceName()
                    ));
                    loadData();
                    tblEvidence.getSelectionModel().select(created);
                } catch (SQLException ex) {
                    showError("Save Error", ex.getMessage());
                }
            });

        } catch (SQLException e) {
            showError("Database Error", e.getMessage());
        }
    }

    @FXML
    public void handleDeleteEvidence() {
        if (selectedItem == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Evidence Artifact");
        alert.setHeaderText("Remove Artifact #" + selectedItem.getId() + ": " + selectedItem.getEvidenceName() + "?");
        alert.setContentText("This permanently deletes the evidence record from the SQLite database.");

        Optional<ButtonType> resp = alert.showAndWait();
        if (resp.isPresent() && resp.get() == ButtonType.OK) {
            try {
                boolean ok = evidenceDAO.delete(selectedItem.getId());
                if (ok) {
                    IncidentEventPublisher.getInstance().publish(new IncidentEvent(
                            IncidentEvent.EventType.EVIDENCE_DELETED,
                            selectedItem.getIncidentId(),
                            selectedItem.getCustodyStatus().name(),
                            null,
                            selectedItem.getCurrentCustodianId(),
                            "Deleted evidence artifact: " + selectedItem.getEvidenceName()
                    ));
                    lblCustodyMessage.setText("✓ Deleted Artifact #" + selectedItem.getId());
                    loadData();
                }
            } catch (SQLException e) {
                showError("Delete Error", e.getMessage());
            }
        }
    }

    @FXML
    public void handleVerifyIntegrity() {
        if (selectedItem == null) {
            showError("No Selection", "Please select a digital forensic artifact from the table to verify.");
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Cryptographic Integrity Verifier (SHA-256)");
        dialog.setHeaderText("Verifying Artifact: " + selectedItem.getEvidenceName() + " (ID #" + selectedItem.getId() + ")");

        ButtonType btnVerify = new ButtonType("Run Verification", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnVerify, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        Label lblExpected = new Label(selectedItem.getFileHash());
        lblExpected.setStyle("-fx-font-family: monospace; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");

        TextField tfInputHash = new TextField();
        tfInputHash.setPromptText("Paste expected SHA-256 hash or choose file...");
        tfInputHash.setPrefWidth(320);

        Button btnBrowse = new Button("Browse File...");
        btnBrowse.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Artifact to Hash & Verify");
            if (dialog.getDialogPane().getScene() != null && dialog.getDialogPane().getScene().getWindow() != null) {
                File file = chooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
                if (file != null) {
                    try {
                        String computed = computeFileSHA256(file);
                        tfInputHash.setText(computed);
                    } catch (Exception ex) {
                        showError("Hash Error", "Failed to compute file hash: " + ex.getMessage());
                    }
                }
            }
        });

        HBox inputRow = new HBox(8, tfInputHash, btnBrowse);

        grid.add(new Label("Custody Record Hash:"), 0, 0);
        grid.add(lblExpected, 1, 0);
        grid.add(new Label("Verification Hash / File:"), 0, 1);
        grid.add(inputRow, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == btnVerify) {
                return tfInputHash.getText().trim();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(hash -> {
            if (hash.isEmpty()) {
                showError("Validation Error", "Please provide a hash or select a file to verify.");
                return;
            }

            boolean matches = selectedItem.getFileHash().equalsIgnoreCase(hash);
            if (matches) {
                IncidentEventPublisher.getInstance().publish(new IncidentEvent(
                        IncidentEvent.EventType.EVIDENCE_INTEGRITY_VERIFIED,
                        selectedItem.getIncidentId(),
                        "UNVERIFIED",
                        "VERIFIED_MATCH",
                        1,
                        "SHA-256 verified for Artifact #" + selectedItem.getId() + " (" + selectedItem.getEvidenceName() + ")"
                ));
                lblCustodyMessage.setText("✓ Cryptographic integrity verified: SHA-256 match confirmed.");
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Integrity Verified");
                alert.setHeaderText("✓ Cryptographic Integrity Confirmed");
                alert.setContentText("Artifact SHA-256 hash matches the database chain of custody ledger perfectly.\n\nHash: " + hash + "\n\nIntegrity Status: UNTAMPERED");
                alert.showAndWait();
            } else {
                lblCustodyMessage.setText("⚠ INTEGRITY ALERT: SHA-256 hash mismatch detected!");
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Integrity Warning");
                alert.setHeaderText("⚠ Cryptographic Hash Mismatch Detected!");
                alert.setContentText("The provided hash does NOT match the stored chain of custody record!\n\nExpected: " + selectedItem.getFileHash() + "\nActual:   " + hash + "\n\nWarning: The evidence artifact may have been modified or corrupted.");
                alert.showAndWait();
            }
        });
    }

    public static String computeFileSHA256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[8192];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }

    private String generateDemoSha256(String seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(seed.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        }
    }

    private String truncateHash(String hash) {
        if (hash == null) return "-";
        if (hash.length() > 20) {
            return hash.substring(0, 10) + "..." + hash.substring(hash.length() - 8);
        }
        return hash;
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("DFIR Orchestrator");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
