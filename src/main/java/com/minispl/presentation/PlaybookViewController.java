package com.minispl.presentation;

import com.minispl.application.observer.IncidentEvent;
import com.minispl.application.observer.IncidentEventListener;
import com.minispl.application.observer.IncidentEventPublisher;
import com.minispl.application.playbook.InvestigationEngine;
import com.minispl.application.playbook.InvestigationReport;
import com.minispl.application.playbook.PlaybookEngineFactory;
import com.minispl.domain.enums.IncidentStatus;
import com.minispl.domain.enums.PlaybookPhase;
import com.minispl.domain.model.Incident;
import com.minispl.domain.model.Playbook;
import com.minispl.domain.model.PlaybookStep;
import com.minispl.persistence.dao.IncidentDAO;
import com.minispl.persistence.dao.PlaybookDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.List;

public class PlaybookViewController implements Refreshable, IncidentEventListener {

    @FXML private ListView<Playbook> listPlaybooks;
    @FXML private Label lblPlaybookTitle;
    @FXML private Label lblPlaybookDescription;
    @FXML private Button btnExecutePlaybook;
    @FXML private Label lblExecutionStatus;

    @FXML private TableView<PlaybookStep> tblSteps;
    @FXML private TableColumn<PlaybookStep, Integer> colSeq;
    @FXML private TableColumn<PlaybookStep, PlaybookPhase> colPhase;
    @FXML private TableColumn<PlaybookStep, String> colStepName;
    @FXML private TableColumn<PlaybookStep, String> colActionType;
    @FXML private TableColumn<PlaybookStep, String> colCriteria;

    private final PlaybookDAO playbookDAO = new PlaybookDAO();
    private final IncidentDAO incidentDAO = new IncidentDAO();
    private final ObservableList<Playbook> playbooks = FXCollections.observableArrayList();
    private final ObservableList<PlaybookStep> stepList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colSeq.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getSequenceOrder()).asObject());
        colPhase.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getPhase()));
        colStepName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStepName()));
        colActionType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getActionType() != null ? d.getValue().getActionType() : "MANUAL"));
        colCriteria.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPassFailCriteria()));

        tblSteps.setItems(stepList);
        listPlaybooks.setItems(playbooks);

        listPlaybooks.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Playbook item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getThreatType() + " Playbook");
                }
            }
        });

        listPlaybooks.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> selectPlaybook(newVal));

        IncidentEventPublisher.getInstance().subscribe(this);
        loadPlaybooks();
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
        loadPlaybooks();
    }

    private void loadPlaybooks() {
        try {
            int selectedIndex = listPlaybooks.getSelectionModel().getSelectedIndex();
            List<Playbook> all = playbookDAO.findAll();
            playbooks.setAll(all);
            if (!playbooks.isEmpty()) {
                int toSelect = (selectedIndex >= 0 && selectedIndex < playbooks.size()) ? selectedIndex : 0;
                listPlaybooks.getSelectionModel().select(toSelect);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void selectPlaybook(Playbook pb) {
        if (pb == null) {
            lblPlaybookTitle.setText("Select a playbook");
            lblPlaybookDescription.setText("");
            if (lblExecutionStatus != null) lblExecutionStatus.setText("");
            stepList.clear();
            return;
        }

        lblPlaybookTitle.setText(pb.getThreatType() + " Response Workflow");
        lblPlaybookDescription.setText(pb.getDescription());
        if (lblExecutionStatus != null) lblExecutionStatus.setText("");
        stepList.setAll(pb.getSteps());
    }

    /**
     * Executes the selected threat playbook using the Template Method pattern.
     * Enforces: Triage -> Containment -> Evidence Collection -> Eradication -> Post-Mortem.
     */
    @FXML
    public void handleExecutePlaybook() {
        Playbook selected = listPlaybooks.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Playbook Selection", "Please select a playbook from the list.");
            return;
        }

        try {
            // Find an open incident matching this threat type, or any non-closed incident
            List<Incident> allIncidents = incidentDAO.findAll();
            List<Incident> matching = allIncidents.stream()
                    .filter(i -> i.getThreatType().equalsIgnoreCase(selected.getThreatType()))
                    .filter(i -> i.getStatus() != IncidentStatus.CLOSED)
                    .toList();

            Incident target;
            if (!matching.isEmpty()) {
                target = matching.get(0);
            } else {
                List<Incident> anyOpen = allIncidents.stream()
                        .filter(i -> i.getStatus() != IncidentStatus.CLOSED)
                        .toList();
                if (!anyOpen.isEmpty()) {
                    target = anyOpen.get(0);
                } else if (!allIncidents.isEmpty()) {
                    target = allIncidents.get(0);
                } else {
                    showAlert(Alert.AlertType.INFORMATION, "No Incidents", "No incidents available to execute against. Please create one in Incidents tab.");
                    return;
                }
            }

            InvestigationEngine engine = PlaybookEngineFactory.getEngine(selected.getThreatType(), incidentDAO);
            InvestigationReport report = engine.executePlaybook(target);

            if (lblExecutionStatus != null) {
                lblExecutionStatus.setText("Template Method Executed: 5/5 phases completed for Case #" + target.getId());
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Template Method Playbook Execution");
            alert.setHeaderText(String.format("%s Investigation Engine: Invariant Lifecycle Complete", selected.getThreatType()));
            alert.setContentText(report.getExecutionSummary());
            alert.showAndWait();

            refresh();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Execution Error", e.getMessage());
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
