package com.igdm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption service for Meta access tokens at rest.
 *
 * Design:
 * - AES-256 in GCM mode (authenticated encryption — integrity + confidentiality)
 * - Random 12-byte IV prepended to each ciphertext (unique per encryption)
 * - 128-bit authentication tag
 * - Key sourced from environment variable (TOKEN_ENCRYPTION_KEY)
 *
 * Storage format: Base64( IV[12] || CIPHERTEXT || AUTH_TAG[16] )
 */
@Service
public class TokenEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(TokenEncryptionService.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;   // 96 bits
    private static final int GCM_TAG_LENGTH = 128;  // bits

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenEncryptionService(@Value("${app.encryption.token-key}") String hexKey) {
        byte[] keyBytes = hexStringToBytes(hexKey);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "TOKEN_ENCRYPTION_KEY must be exactly 32 bytes (64 hex chars), got " + keyBytes.length);
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        log.info("TokenEncryptionService initialized with AES-256-GCM");
    }

    /**
     * Encrypt a plaintext access token.
     *
     * @param plaintext The access token to encrypt
     * @return Base64-encoded string: IV || ciphertext || auth tag
     */
    public String encrypt(String plaintext) {
        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

            // Prepend IV to ciphertext: IV[12] || CIPHERTEXT+TAG
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt token", e);
        }
    }

    /**
     * Decrypt an encrypted access token.
     *
     * @param encryptedBase64 Base64-encoded string from encrypt()
     * @return The original plaintext access token
     */
    public String decrypt(String encryptedBase64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedBase64);

            // Extract IV from the beginning
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            // Remaining bytes are ciphertext + auth tag
            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, "UTF-8");

        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt token — key may have changed", e);
        }
    }

    /**
     * Convert a hex string to byte array.
     * Handles both lowercase and uppercase hex characters.
     */
    private static byte[] hexStringToBytes(String hex) {
        // Remove any whitespace or "0x" prefix
        hex = hex.replaceAll("\\s+", "").replaceFirst("^0[xX]", "");

        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }

        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            bytes[i] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
        }
        return bytes;
    }
}
