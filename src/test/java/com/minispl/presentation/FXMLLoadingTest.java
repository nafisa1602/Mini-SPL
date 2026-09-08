package com.minispl.presentation;

import com.minispl.persistence.DatabaseManager;
import com.minispl.persistence.DatabaseSeeder;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FXMLLoadingTest {

    @BeforeAll
    public static void initJFX() throws Exception {
        String testDbPath = "test_fxml_" + System.currentTimeMillis() + ".db";
        File testDb = new File(testDbPath);
        testDb.deleteOnExit();

        DatabaseManager.setDbUrl("jdbc:sqlite:" + testDbPath);
        DatabaseManager.getInstance().initializeDatabase();
        new DatabaseSeeder().seedIfEmpty();

        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX toolkit failed to initialize");
    }

    @Test
    public void testMainViewFXML() throws Exception {
        assertFXMLParses("/com/minispl/presentation/main-view.fxml");
    }

    @Test
    public void testIncidentViewFXML() throws Exception {
        assertFXMLParses("/com/minispl/presentation/incident-view.fxml");
    }

    @Test
    public void testEvidenceViewFXML() throws Exception {
        assertFXMLParses("/com/minispl/presentation/evidence-view.fxml");
    }

    @Test
    public void testPlaybookViewFXML() throws Exception {
        assertFXMLParses("/com/minispl/presentation/playbook-view.fxml");
    }

    @Test
    public void testReportsViewFXML() throws Exception {
        assertFXMLParses("/com/minispl/presentation/reports-view.fxml");
    }

    private void assertFXMLParses(String resourcePath) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
                Object root = loader.load();
                if (root != null) {
                    success.set(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Loading timed out for " + resourcePath);
        assertTrue(success.get(), "FXML failed to parse cleanly: " + resourcePath);
    }
}
