package zaga.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

public class JarIntegrity {
    private static final String ENCRYPTED_JAR_RESOURCE = "/rich-1.0.01.jar.encrypted";
    private static final Path INTEGRITY_DIR = Path.of(System.getProperty("user.home"), ".zaga", ".sec");
    private static final int MIN_VALID_SIZE = 1024;
    private static final int MAX_VALID_SIZE = 50 * 1024 * 1024;

    public static boolean verifyJarIntegrity() {
        try {
            byte[] jarBytes = readEncryptedJar();
            if (jarBytes == null || jarBytes.length == 0) return false;
            if (jarBytes.length < MIN_VALID_SIZE || jarBytes.length > MAX_VALID_SIZE) return false;

            String currentHash = computeHash(jarBytes);
            byte[] storedData = loadEncryptedHash();

            if (storedData == null) {
                saveEncryptedHash(currentHash);
                return true;
            }

            String storedHash = decryptHash(storedData);
            if (storedHash == null) {
                saveEncryptedHash(currentHash);
                return true;
            }

            if (!constantTimeEquals(currentHash, storedHash)) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getJarHash() {
        try {
            byte[] jarBytes = readEncryptedJar();
            if (jarBytes == null) return null;
            return computeHash(jarBytes);
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] readEncryptedJar() {
        try (InputStream is = JarIntegrity.class.getResourceAsStream(ENCRYPTED_JAR_RESOURCE)) {
            if (is == null) return null;
            return is.readAllBytes();
        } catch (Exception e) {
            return null;
        }
    }

    private static String computeHash(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] loadEncryptedHash() {
        try {
            Path encFile = INTEGRITY_DIR.resolve("dat");
            if (Files.exists(encFile)) {
                return Files.readAllBytes(encFile);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void saveEncryptedHash(String hash) {
        try {
            Files.createDirectories(INTEGRITY_DIR);
            byte[] encrypted = encryptHash(hash);
            if (encrypted != null) {
                Path encFile = INTEGRITY_DIR.resolve("dat");
                Files.write(encFile, encrypted);
            }
        } catch (Exception ignored) {}
    }

    private static byte[] encryptHash(String hash) {
        try {
            String machineKey = getMachineKey();
            if (machineKey == null) return null;

            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(machineKey.toCharArray(), salt, 100000, 256);
            SecretKey key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));

            byte[] encrypted = cipher.doFinal(hash.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            byte[] result = new byte[iv.length + salt.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(salt, 0, result, iv.length, salt.length);
            System.arraycopy(encrypted, 0, result, iv.length + salt.length, encrypted.length);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private static String decryptHash(byte[] data) {
        try {
            if (data.length < 28) return null;

            String machineKey = getMachineKey();
            if (machineKey == null) return null;

            byte[] iv = Arrays.copyOfRange(data, 0, 12);
            byte[] salt = Arrays.copyOfRange(data, 12, 28);
            byte[] encrypted = Arrays.copyOfRange(data, 28, data.length);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(machineKey.toCharArray(), salt, 100000, 256);
            SecretKey key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static String getMachineKey() {
        try {
            String hwid = HwidUtil.getHwid();
            if (hwid != null && !hwid.isEmpty()) {
                return "zaga-" + hwid + "-integrity";
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
