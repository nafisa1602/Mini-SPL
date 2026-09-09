package com.minispl.presentation;

/**
 * Interface implemented by presentation view controllers to support
 * refreshing cached views upon tab switching or observer notifications.
 */
public interface Refreshable {
    void refresh();
}
