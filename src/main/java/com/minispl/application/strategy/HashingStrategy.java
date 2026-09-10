package com.minispl.application.strategy;

import java.io.File;

/**
 * Strategy Pattern Interface: HashingStrategy.
 * Encapsulates cryptographic hashing algorithms (SHA-256 vs. SHA-512)
 * for digital forensic evidence verification without altering core workflows.
 */
public interface HashingStrategy {

    String getAlgorithmName();

    String hash(byte[] data);

    String hashFile(File file) throws Exception;
}
