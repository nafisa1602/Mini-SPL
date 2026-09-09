package com.minispl.application.observer;

/**
 * Observer interface for components subscribing to incident lifecycle,
 * evidence custody, and remediation command events.
 */
@FunctionalInterface
public interface IncidentEventListener {
    void onIncidentEvent(IncidentEvent event);
}
