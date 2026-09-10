package com.minispl.application;

import com.minispl.application.strategy.HashingContext;
import com.minispl.application.strategy.Sha256HashingStrategy;
import com.minispl.application.strategy.Sha512HashingStrategy;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class HashingStrategyTest {

    @Test
    public void testSha256Strategy() {
        Sha256HashingStrategy strategy = new Sha256HashingStrategy();
        assertEquals("SHA-256", strategy.getAlgorithmName());

        byte[] input = "forensic_data".getBytes(StandardCharsets.UTF_8);
        String hash = strategy.hash(input);

        assertNotNull(hash);
        assertEquals(64, hash.length(), "SHA-256 digest should be 64 hex characters");
    }

    @Test
    public void testSha512Strategy() {
        Sha512HashingStrategy strategy = new Sha512HashingStrategy();
        assertEquals("SHA-512", strategy.getAlgorithmName());

        byte[] input = "forensic_data".getBytes(StandardCharsets.UTF_8);
        String hash = strategy.hash(input);

        assertNotNull(hash);
        assertEquals(128, hash.length(), "SHA-512 digest should be 128 hex characters");
    }

    @Test
    public void testHashingContextRuntimeSwapping() throws Exception {
        File temp = File.createTempFile("evidence_test_", ".dat");
        temp.deleteOnExit();

        try (FileWriter writer = new FileWriter(temp)) {
            writer.write("MALWARE_SAMPLE_MEMORY_DUMP");
        }

        HashingContext context = new HashingContext(new Sha256HashingStrategy());
        String sha256 = context.computeFileHash(temp);
        assertEquals(64, sha256.length());

        // Swap strategy at runtime to SHA-512
        context.setStrategy(new Sha512HashingStrategy());
        assertEquals("SHA-512", context.getStrategy().getAlgorithmName());
        String sha512 = context.computeFileHash(temp);
        assertEquals(128, sha512.length());

        assertNotEquals(sha256, sha512);
    }
}
