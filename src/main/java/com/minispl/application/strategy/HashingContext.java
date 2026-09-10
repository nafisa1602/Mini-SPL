package com.minispl.application.strategy;

import java.io.File;

/**
 * Strategy Pattern Context: HashingContext.
 * Allows dynamic runtime swapping between SHA-256 and SHA-512 cryptographic hashing.
 */
public class HashingContext {

    private HashingStrategy strategy;

    public HashingContext() {
        this.strategy = new Sha256HashingStrategy();
    }

    public HashingContext(HashingStrategy strategy) {
        this.strategy = strategy != null ? strategy : new Sha256HashingStrategy();
    }

    public void setStrategy(HashingStrategy strategy) {
        if (strategy != null) {
            this.strategy = strategy;
        }
    }

    public HashingStrategy getStrategy() {
        return strategy;
    }

    public String computeHash(byte[] data) {
        return strategy.hash(data);
    }

    public String computeFileHash(File file) throws Exception {
        return strategy.hashFile(file);
    }
}
