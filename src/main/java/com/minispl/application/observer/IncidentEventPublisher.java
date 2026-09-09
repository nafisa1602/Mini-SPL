package com.minispl.application.observer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton Event Bus / Subject for the Observer Pattern.
 * Decouples background business logic and state machines from JavaFX presentation controllers
 * and the persistent SQLite audit trail logger.
 */
public class IncidentEventPublisher {

    private static final Logger LOGGER = Logger.getLogger(IncidentEventPublisher.class.getName());
    private static final IncidentEventPublisher INSTANCE = new IncidentEventPublisher();

    private final List<IncidentEventListener> listeners = new CopyOnWriteArrayList<>();

    private IncidentEventPublisher() {}

    public static IncidentEventPublisher getInstance() {
        return INSTANCE;
    }

    /**
     * Registers an observer listener to receive incident events.
     */
    public void subscribe(IncidentEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Alias for subscribe to match standard listener semantics.
     */
    public void addListener(IncidentEventListener listener) {
        subscribe(listener);
    }

    /**
     * Unregisters an observer listener.
     */
    public void unsubscribe(IncidentEventListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Alias for unsubscribe.
     */
    public void removeListener(IncidentEventListener listener) {
        unsubscribe(listener);
    }

    /**
     * Broadcasts an incident event synchronously to all registered observers.
     */
    public void publish(IncidentEvent event) {
        if (event == null) return;
        for (IncidentEventListener listener : listeners) {
            try {
                listener.onIncidentEvent(event);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error notifying incident event listener: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Removes all registered listeners (primarily for testing isolation).
     */
    public void clearListeners() {
        listeners.clear();
    }

    public List<IncidentEventListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }
}
