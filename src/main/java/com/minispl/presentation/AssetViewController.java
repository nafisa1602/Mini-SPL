package com.minispl.presentation;

import com.minispl.application.observer.IncidentEvent;
import com.minispl.application.observer.IncidentEventListener;
import com.minispl.application.observer.IncidentEventPublisher;
import com.minispl.domain.enums.AssetStatus;
import com.minispl.domain.enums.CriticalityTier;
import com.minispl.domain.model.Asset;
import com.minispl.persistence.dao.AssetDAO;
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
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AssetViewController implements Refreshable, IncidentEventListener {

    @FXML private Label lblTotalAssets;
    @FXML private Label lblTier1Assets;
    @FXML private Label lblIsolatedAssets;
    @FXML private Label lblOnlineAssets;

    @FXML private ComboBox<String> cbStatusFilter;
    @FXML private TextField txtSearch;

    @FXML private TableView<Asset> tblAssets;
    @FXML private TableColumn<Asset, Integer> colId;
    @FXML private TableColumn<Asset, String> colHostname;
    @FXML private TableColumn<Asset, String> colIp;
    @FXML private TableColumn<Asset, CriticalityTier> colCriticality;
    @FXML private TableColumn<Asset, AssetStatus> colStatus;
    @FXML private TableColumn<Asset, String> colDescription;

    @FXML private VBox detailPane;
    @FXML private Label lblDetailHostname;
    @FXML private Label lblDetailIp;
    @FXML private Label lblDetailCriticality;
    @FXML private Label lblDetailStatus;
    @FXML private Label lblDetailDescription;
    @FXML private Label lblStatusMessage;

    @FXML private Button btnEditAsset;
    @FXML private Button btnToggleQuarantine;
    @FXML private Button btnDeleteAsset;

    private final AssetDAO assetDAO = new AssetDAO();
    private final ObservableList<Asset> masterList = FXCollections.observableArrayList();
    private final ObservableList<Asset> filteredList = FXCollections.observableArrayList();
    private Asset selectedAsset;

    @FXML
    public void initialize() {
        // Register with singleton event bus
        IncidentEventPublisher.getInstance().subscribe(this);

        cbStatusFilter.setItems(FXCollections.observableArrayList("ALL", "ONLINE", "ISOLATED", "COMPROMISED", "OFFLINE"));
        cbStatusFilter.setValue("ALL");
        cbStatusFilter.setOnAction(e -> applyFilterAndSearch());

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilterAndSearch());

        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colHostname.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getHostname()));
        colIp.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getIpAddress()));
        colCriticality.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getCriticalityTier()));
        colStatus.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getStatus()));
        colDescription.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));

        tblAssets.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> showDetail(newVal));
        tblAssets.setItems(filteredList);

        loadData();
    }

    @Override
    public void refresh() {
        loadData();
    }

    @Override
    public void onIncidentEvent(IncidentEvent event) {
        if (event.getType() == IncidentEvent.EventType.COMMAND_EXECUTED ||
            event.getType() == IncidentEvent.EventType.COMMAND_UNDONE ||
            event.getType() == IncidentEvent.EventType.ASSET_CREATED ||
            event.getType() == IncidentEvent.EventType.ASSET_UPDATED ||
            event.getType() == IncidentEvent.EventType.ASSET_DELETED) {
            Platform.runLater(this::refresh);
        }
    }

    private void loadData() {
        try {
            int selectedId = selectedAsset != null ? selectedAsset.getId() : -1;
            List<Asset> assets = assetDAO.findAll();
            masterList.setAll(assets);

            int total = assets.size();
            long tier1 = assets.stream().filter(a -> a.getCriticalityTier() == CriticalityTier.CRITICAL).count();
            long isolated = assets.stream().filter(a -> a.getStatus() == AssetStatus.ISOLATED).count();
            long online = assets.stream().filter(a -> a.getStatus() == AssetStatus.ONLINE).count();

            lblTotalAssets.setText(String.valueOf(total));
            lblTier1Assets.setText(String.valueOf(tier1));
            lblIsolatedAssets.setText(String.valueOf(isolated));
            lblOnlineAssets.setText(String.valueOf(online));

            applyFilterAndSearch();

            if (!filteredList.isEmpty()) {
                Asset toSelect = filteredList.stream().filter(a -> a.getId() == selectedId).findFirst().orElse(filteredList.get(0));
                tblAssets.getSelectionModel().select(toSelect);
            } else {
                showDetail(null);
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to load assets: " + e.getMessage());
        }
    }

    private void applyFilterAndSearch() {
        String filter = cbStatusFilter.getValue();
        String query = txtSearch.getText() != null ? txtSearch.getText().trim().toLowerCase() : "";

        List<Asset> matched = masterList.stream()
                .filter(asset -> {
                    if (filter != null && !"ALL".equalsIgnoreCase(filter)) {
                        if (!asset.getStatus().name().equalsIgnoreCase(filter)) {
                            return false;
                        }
                    }
                    if (!query.isEmpty()) {
                        boolean matchHost = asset.getHostname().toLowerCase().contains(query);
                        boolean matchIp = asset.getIpAddress().toLowerCase().contains(query);
                        boolean matchDesc = asset.getDescription() != null && asset.getDescription().toLowerCase().contains(query);
                        return matchHost || matchIp || matchDesc;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        filteredList.setAll(matched);
    }

    private void showDetail(Asset asset) {
        this.selectedAsset = asset;
        if (asset == null) {
            lblDetailHostname.setText("No asset selected");
            lblDetailIp.setText("-");
            lblDetailCriticality.setText("-");
            lblDetailStatus.setText("-");
            lblDetailDescription.setText("-");
            btnEditAsset.setDisable(true);
            btnToggleQuarantine.setDisable(true);
            btnDeleteAsset.setDisable(true);
            return;
        }

        lblDetailHostname.setText(asset.getHostname());
        lblDetailIp.setText(asset.getIpAddress());
        lblDetailCriticality.setText(asset.getCriticalityTier().name());
        lblDetailStatus.setText(asset.getStatus().name());
        lblDetailDescription.setText(asset.getDescription() != null && !asset.getDescription().isEmpty() ? asset.getDescription() : "No description provided");

        btnEditAsset.setDisable(false);
        btnDeleteAsset.setDisable(false);
        btnToggleQuarantine.setDisable(false);

        if (asset.getStatus() == AssetStatus.ISOLATED) {
            btnToggleQuarantine.setText("Release Quarantine");
            btnToggleQuarantine.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        } else {
            btnToggleQuarantine.setText("Quarantine Host");
            btnToggleQuarantine.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold;");
        }
    }

    @FXML
    public void handleNewAsset() {
        Dialog<Asset> dialog = new Dialog<>();
        dialog.setTitle("Register New Infrastructure Asset");
        dialog.setHeaderText("Add an endpoint, server, or database to the DFIR Inventory");

        ButtonType btnSave = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSave, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField tfHostname = new TextField();
        tfHostname.setPromptText("e.g. srv-dc-01.corp.internal");
        TextField tfIp = new TextField();
        tfIp.setPromptText("e.g. 192.168.10.50");

        ComboBox<CriticalityTier> cbTier = new ComboBox<>(FXCollections.observableArrayList(CriticalityTier.values()));
        cbTier.setValue(CriticalityTier.HIGH);

        ComboBox<AssetStatus> cbStatus = new ComboBox<>(FXCollections.observableArrayList(AssetStatus.values()));
        cbStatus.setValue(AssetStatus.ONLINE);

        TextArea taDesc = new TextArea();
        taDesc.setPromptText("Asset role and network zone description");
        taDesc.setPrefRowCount(3);

        grid.add(new Label("Hostname:"), 0, 0);
        grid.add(tfHostname, 1, 0);
        grid.add(new Label("IP Address:"), 0, 1);
        grid.add(tfIp, 1, 1);
        grid.add(new Label("Criticality Tier:"), 0, 2);
        grid.add(cbTier, 1, 2);
        grid.add(new Label("Status:"), 0, 3);
        grid.add(cbStatus, 1, 3);
        grid.add(new Label("Description:"), 0, 4);
        grid.add(taDesc, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSave) {
                if (tfHostname.getText().trim().isEmpty() || tfIp.getText().trim().isEmpty()) {
                    showError("Validation Error", "Hostname and IP Address cannot be empty.");
                    return null;
                }
                return new Asset(
                        tfHostname.getText().trim(),
                        tfIp.getText().trim(),
                        cbTier.getValue(),
                        cbStatus.getValue(),
                        taDesc.getText().trim()
                );
            }
            return null;
        });

        Optional<Asset> result = dialog.showAndWait();
        result.ifPresent(asset -> {
            try {
                Asset created = assetDAO.create(asset);
                IncidentEventPublisher.getInstance().publish(new IncidentEvent(
                        IncidentEvent.EventType.ASSET_CREATED,
                        0,
                        null,
                        created.getStatus().name(),
                        1,
                        "Registered new asset " + created.getHostname() + " (" + created.getIpAddress() + ")"
                ));
                lblStatusMessage.setText("✓ Registered asset: " + created.getHostname());
                loadData();
            } catch (SQLException e) {
                showError("Registration Failed", e.getMessage());
            }
        });
    }

    @FXML
    public void handleEditAsset() {
        if (selectedAsset == null) return;

        Dialog<Asset> dialog = new Dialog<>();
        dialog.setTitle("Edit Infrastructure Asset");
        dialog.setHeaderText("Update configuration for " + selectedAsset.getHostname());

        ButtonType btnSave = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSave, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField tfHostname = new TextField(selectedAsset.getHostname());
        TextField tfIp = new TextField(selectedAsset.getIpAddress());

        ComboBox<CriticalityTier> cbTier = new ComboBox<>(FXCollections.observableArrayList(CriticalityTier.values()));
        cbTier.setValue(selectedAsset.getCriticalityTier());

        ComboBox<AssetStatus> cbStatus = new ComboBox<>(FXCollections.observableArrayList(AssetStatus.values()));
        cbStatus.setValue(selectedAsset.getStatus());

        TextArea taDesc = new TextArea(selectedAsset.getDescription() != null ? selectedAsset.getDescription() : "");
        taDesc.setPrefRowCount(3);

        grid.add(new Label("Hostname:"), 0, 0);
        grid.add(tfHostname, 1, 0);
        grid.add(new Label("IP Address:"), 0, 1);
        grid.add(tfIp, 1, 1);
        grid.add(new Label("Criticality Tier:"), 0, 2);
        grid.add(cbTier, 1, 2);
        grid.add(new Label("Status:"), 0, 3);
        grid.add(cbStatus, 1, 3);
        grid.add(new Label("Description:"), 0, 4);
        grid.add(taDesc, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSave) {
                if (tfHostname.getText().trim().isEmpty() || tfIp.getText().trim().isEmpty()) {
                    showError("Validation Error", "Hostname and IP Address cannot be empty.");
                    return null;
                }
                selectedAsset.setHostname(tfHostname.getText().trim());
                selectedAsset.setIpAddress(tfIp.getText().trim());
                selectedAsset.setCriticalityTier(cbTier.getValue());
                selectedAsset.setStatus(cbStatus.getValue());
                selectedAsset.setDescription(taDesc.getText().trim());
                return selectedAsset;
            }
            return null;
        });

        Optional<Asset> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            try {
                boolean ok = assetDAO.update(updated);
                if (ok) {
                    IncidentEventPublisher.getInstance().publish(new IncidentEvent(
                            IncidentEvent.EventType.ASSET_UPDATED,
                            0,
                            null,
                            updated.getStatus().name(),
                            1,
                            "Updated configuration for asset " + updated.getHostname()
                    ));
                    lblStatusMessage.setText("✓ Updated asset: " + updated.getHostname());
                    loadData();
                }
            } catch (SQLException e) {
                showError("Update Failed", e.getMessage());
            }
        });
    }

    @FXML
    public void handleToggleQuarantine() {
        if (selectedAsset == null) return;

        AssetStatus targetStatus = (selectedAsset.getStatus() == AssetStatus.ISOLATED)
                ? AssetStatus.ONLINE
                : AssetStatus.ISOLATED;

        String actionWord = (targetStatus == AssetStatus.ISOLATED) ? "Quarantine" : "Release";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Network Quarantine");
        confirm.setHeaderText(actionWord + " Asset " + selectedAsset.getHostname() + "?");
        confirm.setContentText("This will change the network status to " + targetStatus + " and publish an event to the security audit log.");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                boolean ok = assetDAO.updateStatus(selectedAsset.getId(), targetStatus);
                if (ok) {
                    IncidentEventPublisher.getInstance().publish(new IncidentEvent(
                            IncidentEvent.EventType.ASSET_UPDATED,
                            0,
                            selectedAsset.getStatus().name(),
                            targetStatus.name(),
                            1,
                            actionWord + " asset " + selectedAsset.getHostname() + " (" + selectedAsset.getIpAddress() + ")"
                    ));
                    lblStatusMessage.setText("✓ " + actionWord + " executed for " + selectedAsset.getHostname());
                    loadData();
                }
            } catch (SQLException e) {
                showError("Status Update Failed", e.getMessage());
            }
        }
    }

    @FXML
    public void handleDeleteAsset() {
        if (selectedAsset == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Decommission");
        confirm.setHeaderText("Permanently delete asset " + selectedAsset.getHostname() + "?");
        confirm.setContentText("Warning: Deleting this asset will fail if it has active incident references.");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                boolean ok = assetDAO.delete(selectedAsset.getId());
                if (ok) {
                    IncidentEventPublisher.getInstance().publish(new IncidentEvent(
                            IncidentEvent.EventType.ASSET_DELETED,
                            0,
                            null,
                            null,
                            1,
                            "Decommissioned asset " + selectedAsset.getHostname()
                    ));
                    lblStatusMessage.setText("✓ Decommissioned asset " + selectedAsset.getHostname());
                    selectedAsset = null;
                    loadData();
                } else {
                    showError("Delete Failed", "Could not delete asset.");
                }
            } catch (SQLException e) {
                showError("Constraint Violation", "Cannot delete asset: It is currently linked to existing incidents or evidence.\n\nDetails: " + e.getMessage());
            }
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Asset Management");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
