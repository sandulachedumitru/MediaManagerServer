package com.miti.photos_manager_server.utils;

import net.openhft.hashing.LongHashFunction;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Dumitru Săndulache (sandulachedumitru@hotmail.com)
 */

public class HashUtils {
    private static final int BUFFER_SIZE = 262144; // 256 KB buffer

    public static String computeFileHash_SHA256(Path file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public static String computeFileHash_XXHash(Path file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            LongHashFunction xxHash = LongHashFunction.xx();
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long hash = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                hash = xxHash.hashBytes(buffer, 0, bytesRead);
            }

            return Long.toHexString(hash);
        }
    }
}
