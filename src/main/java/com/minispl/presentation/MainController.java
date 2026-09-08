package com.minispl.presentation;

import com.minispl.domain.enums.IncidentStatus;
import com.minispl.persistence.dao.EvidenceDAO;
import com.minispl.persistence.dao.IncidentDAO;
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

public class MainController {

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
    private Button currentActiveButton;

    @FXML
    public void initialize() {
        updateSidebarStats();
        // Load default view: Incidents
        showIncidents();
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
            if (viewNode == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                viewNode = loader.load();
                viewCache.put(fxmlPath, viewNode);
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
