package com.minispl.domain.model;

import java.util.ArrayList;
import java.util.List;

public class Playbook {
    private int id;
    private String threatType;
    private String description;
    private List<PlaybookStep> steps = new ArrayList<>();

    public Playbook() {}

    public Playbook(int id, String threatType, String description) {
        this.id = id;
        this.threatType = threatType;
        this.description = description;
    }

    public Playbook(String threatType, String description) {
        this.threatType = threatType;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getThreatType() {
        return threatType;
    }

    public void setThreatType(String threatType) {
        this.threatType = threatType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PlaybookStep> getSteps() {
        return steps;
    }

    public void setSteps(List<PlaybookStep> steps) {
        this.steps = steps;
    }

    @Override
    public String toString() {
        return threatType + " Playbook";
    }
}
