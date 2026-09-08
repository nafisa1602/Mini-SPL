package com.minispl.presentation;

import com.minispl.domain.enums.PlaybookPhase;
import com.minispl.domain.model.Playbook;
import com.minispl.domain.model.PlaybookStep;
import com.minispl.persistence.dao.PlaybookDAO;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.List;

public class PlaybookViewController {

    @FXML private ListView<Playbook> listPlaybooks;
    @FXML private Label lblPlaybookTitle;
    @FXML private Label lblPlaybookDescription;

    @FXML private TableView<PlaybookStep> tblSteps;
    @FXML private TableColumn<PlaybookStep, Integer> colSeq;
    @FXML private TableColumn<PlaybookStep, PlaybookPhase> colPhase;
    @FXML private TableColumn<PlaybookStep, String> colStepName;
    @FXML private TableColumn<PlaybookStep, String> colActionType;
    @FXML private TableColumn<PlaybookStep, String> colCriteria;

    private final PlaybookDAO playbookDAO = new PlaybookDAO();
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
                    setText("🛡  " + item.getThreatType() + " Playbook");
                }
            }
        });

        listPlaybooks.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> selectPlaybook(newVal));

        loadPlaybooks();
    }

    private void loadPlaybooks() {
        try {
            List<Playbook> all = playbookDAO.findAll();
            playbooks.setAll(all);
            if (!playbooks.isEmpty()) {
                listPlaybooks.getSelectionModel().select(0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void selectPlaybook(Playbook pb) {
        if (pb == null) {
            lblPlaybookTitle.setText("Select a playbook");
            lblPlaybookDescription.setText("");
            stepList.clear();
            return;
        }

        lblPlaybookTitle.setText(pb.getThreatType() + " Response Workflow");
        lblPlaybookDescription.setText(pb.getDescription());
        stepList.setAll(pb.getSteps());
    }
}
