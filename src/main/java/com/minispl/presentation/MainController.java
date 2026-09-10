package com.minispl.presentation;

import com.minispl.application.observer.IncidentEvent;
import com.minispl.application.observer.IncidentEventListener;
import com.minispl.application.observer.IncidentEventPublisher;
import com.minispl.domain.enums.IncidentStatus;
import com.minispl.persistence.dao.EvidenceDAO;
import com.minispl.persistence.dao.IncidentDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MainController implements IncidentEventListener {

    @FXML private Button btnNavIncidents;
    @FXML private Button btnNavEvidence;
    @FXML private Button btnNavPlaybooks;
    @FXML private Button btnNavReports;

    @FXML private StackPane contentArea;

    @FXML private Label lblSidebarOpenCases;
    @FXML private Label lblSidebarEvidenceCount;

    private final IncidentDAO incidentDAO = new IncidentDAO();
    private final EvidenceDAO evidenceDAO = new EvidenceDAO();

    private final Map<String, Node> viewCache = new HashMap<>();
    private final Map<String, Object> controllerCache = new HashMap<>();
    private Button currentActiveButton;

    @FXML
    public void initialize() {
        IncidentEventPublisher.getInstance().subscribe(this);
        updateSidebarStats();
        // Load default view: Incidents
        showIncidents();
    }

    @Override
    public void onIncidentEvent(IncidentEvent event) {
        if (Platform.isFxApplicationThread()) {
            updateSidebarStats();
        } else {
            Platform.runLater(this::updateSidebarStats);
        }
    }

    @FXML
    public void showIncidents() {
        switchView("/com/minispl/presentation/incident-view.fxml", btnNavIncidents);
    }

    @FXML
    public void showEvidence() {
        switchView("/com/minispl/presentation/evidence-view.fxml", btnNavEvidence);
    }

    @FXML
    public void showPlaybooks() {
        switchView("/com/minispl/presentation/playbook-view.fxml", btnNavPlaybooks);
    }

    @FXML
    public void showReports() {
        switchView("/com/minispl/presentation/reports-view.fxml", btnNavReports);
    }

    private void switchView(String fxmlPath, Button targetButton) {
        try {
            Node viewNode = viewCache.get(fxmlPath);
            Object controller = controllerCache.get(fxmlPath);

            if (viewNode == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                viewNode = loader.load();
                controller = loader.getController();
                viewCache.put(fxmlPath, viewNode);
                controllerCache.put(fxmlPath, controller);
            }

            contentArea.getChildren().setAll(viewNode);

            // Update button styles
            if (currentActiveButton != null) {
                currentActiveButton.getStyleClass().remove("nav-button-active");
            }
            if (targetButton != null) {
                targetButton.getStyleClass().add("nav-button-active");
                currentActiveButton = targetButton;
            }

            // CRITICAL FIX: Refresh the controller to prevent stale views when switching tabs
            if (controller instanceof Refreshable r) {
                r.refresh();
            }

            updateSidebarStats();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateSidebarStats() {
        try {
            int openCount = incidentDAO.count() - incidentDAO.countByStatus(IncidentStatus.CLOSED);
            int evidenceCount = evidenceDAO.count();
            lblSidebarOpenCases.setText(openCount + " Active");
            lblSidebarEvidenceCount.setText(evidenceCount + " Items");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
