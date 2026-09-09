package com.minispl.presentation;

import com.minispl.domain.enums.AuditStatus;
import com.minispl.domain.enums.IncidentSeverity;
import com.minispl.domain.enums.IncidentStatus;
import com.minispl.domain.enums.PlaybookPhase;
import com.minispl.domain.model.ActionAuditLog;
import com.minispl.domain.model.Incident;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MTTCCalculationTest {

    @Test
    public void testEmptyIncidentsReturnsZero() {
        double mttc = ReportsViewController.calculateMTTCInMinutes(new ArrayList<>(), new ArrayList<>());
        assertEquals(0.0, mttc, 0.001);
    }

    @Test
    public void testNoContainedIncidentsReturnsZero() {
        Incident newInc = new Incident("New Alert", "RANSOMWARE", IncidentSeverity.HIGH, 1, 1, 1);
        newInc.setStatus(IncidentStatus.NEW);

        Incident triagedInc = new Incident("Triaged Alert", "PHISHING", IncidentSeverity.MEDIUM, 1, 1, 1);
        triagedInc.setStatus(IncidentStatus.TRIAGED);

        double mttc = ReportsViewController.calculateMTTCInMinutes(List.of(newInc, triagedInc), new ArrayList<>());
        assertEquals(0.0, mttc, 0.001);
    }

    @Test
    public void testExactMTTCFromStateTransitionAuditLog() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 9, 9, 10, 0, 0);
        LocalDateTime containTime = LocalDateTime.of(2026, 9, 9, 10, 30, 0); // 30 minutes later

        Incident inc = new Incident(1, "Case 1", "RANSOMWARE", IncidentSeverity.CRITICAL,
                IncidentStatus.CONTAINED, 1, 1, 1, 80.0, PlaybookPhase.CONTAINMENT, baseTime, null);

        ActionAuditLog log = new ActionAuditLog(1, 1, "STATE_TRANSITION",
                "{\"entity\":\"INCIDENT\",\"from\":\"TRIAGED\",\"to\":\"CONTAINED\"}",
                1, containTime, false, AuditStatus.EXECUTED);

        double mttc = ReportsViewController.calculateMTTCInMinutes(List.of(inc), List.of(log));
        assertEquals(30.0, mttc, 0.001, "MTTC should be exactly 30 minutes");
    }

    @Test
    public void testAverageMTTCAcrossMultipleContainedIncidents() {
        LocalDateTime t0 = LocalDateTime.of(2026, 9, 9, 8, 0, 0);

        // Case 1: 20 minutes to contain via ISOLATE_HOST
        Incident inc1 = new Incident(1, "Case 1", "RANSOMWARE", IncidentSeverity.HIGH,
                IncidentStatus.CONTAINED, 1, 1, 1, 75.0, PlaybookPhase.CONTAINMENT, t0, null);
        ActionAuditLog log1 = new ActionAuditLog(1, 1, "ISOLATE_HOST",
                "{\"hostname\":\"HOST-01\"}", 1, t0.plusMinutes(20), true, AuditStatus.EXECUTED);

        // Case 2: 40 minutes to contain via STATE_TRANSITION
        Incident inc2 = new Incident(2, "Case 2", "PHISHING", IncidentSeverity.CRITICAL,
                IncidentStatus.CLOSED, 2, 1, 2, 90.0, PlaybookPhase.CLOSED, t0, t0.plusMinutes(60));
        ActionAuditLog log2 = new ActionAuditLog(2, 2, "STATE_TRANSITION",
                "{\"from\":\"TRIAGED\",\"to\":\"CONTAINED\"}", 1, t0.plusMinutes(40), false, AuditStatus.EXECUTED);

        // Case 3: Not contained yet (NEW) -> should not affect average
        Incident inc3 = new Incident(3, "Case 3", "MALWARE", IncidentSeverity.LOW,
                IncidentStatus.NEW, 3, 1, 1, 20.0, PlaybookPhase.TRIAGE, t0, null);

        // Expected average of 20 and 40 is 30.0 minutes
        double mttc = ReportsViewController.calculateMTTCInMinutes(List.of(inc1, inc2, inc3), List.of(log1, log2));
        assertEquals(30.0, mttc, 0.001);
    }
}
