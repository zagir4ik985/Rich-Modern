package zaga.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

public class JarDecryptor {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private final byte[] keyBytes;

    public JarDecryptor(byte[] keyBytes) {
        this.keyBytes = keyBytes;
    }

    public JarDecryptor(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException("Encryption key is null or empty");
        }
        try {
            this.keyBytes = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid base64 encryption key: " + e.getMessage(), e);
        }
    }

    public void decrypt(InputStream encryptedInput, Path outputPath) throws Exception {
        SecretKey key = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);

        byte[] iv = new byte[GCM_IV_LENGTH];
        int totalRead = 0;
        while (totalRead < GCM_IV_LENGTH) {
            int read = encryptedInput.read(iv, totalRead, GCM_IV_LENGTH - totalRead);
            if (read == -1) throw new IOException("Unexpected end of stream while reading IV");
            totalRead += read;
        }

        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] encryptedBytes = encryptedInput.readAllBytes();
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, decryptedBytes);
    }

    public static byte[] encrypt(byte[] data, byte[] keyBytes) throws Exception {
        SecretKey key = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);

        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] encrypted = cipher.doFinal(data);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(iv);
        baos.write(encrypted);
        return baos.toByteArray();
    }

    public static byte[] generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        return keyGen.generateKey().getEncoded();
    }
}
