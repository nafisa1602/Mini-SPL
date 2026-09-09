package com.minispl.application.strategy;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 * Concrete Strategy: SHA-512 cryptographic hashing.
 */
public class Sha512HashingStrategy implements HashingStrategy {

    @Override
    public String getAlgorithmName() {
        return "SHA-512";
    }

    @Override
    public String hash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hashBytes = digest.digest(data);
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("SHA-512 computation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String hashFile(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return bytesToHex(digest.digest());
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
