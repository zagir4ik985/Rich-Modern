package zaga.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;

public class Config {
    private static final Path CONFIG_PATH = Path.of(System.getProperty("user.home"), ".zaga", "config.dat");
    private static final Gson gson = new GsonBuilder().create();
    private static final Object lock = new Object();
    private static ConfigData data;

    public static class ConfigData {
        public volatile String token = null;
        public volatile String login = null;
        public volatile String uid = null;
        public volatile String jarKey = null;
        public volatile String lastVersion = "1.0.0";
        public volatile String themeName = null;
    }

    public static void load() {
        synchronized (lock) {
            try {
                if (Files.exists(CONFIG_PATH)) {
                    byte[] encrypted = Files.readAllBytes(CONFIG_PATH);
                    String json = decrypt(encrypted);
                    if (json != null) {
                        data = gson.fromJson(json, ConfigData.class);
                    }
                }
            } catch (Exception e) {
                System.err.println("[Config] Failed to load config: " + e.getMessage());
                data = new ConfigData();
            }
            if (data == null) data = new ConfigData();
            if (data.lastVersion == null) data.lastVersion = "1.0.0";
        }
    }

    public static void save() {
        synchronized (lock) {
            try {
                Files.createDirectories(CONFIG_PATH.getParent());
                String json = gson.toJson(data);
                byte[] encrypted = encrypt(json);
                if (encrypted != null) {
                    Path tmp = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
                    Files.write(tmp, encrypted);
                    Files.move(tmp, CONFIG_PATH, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static ConfigData get() {
        synchronized (lock) {
            if (data == null) load();
            return data;
        }
    }

    private static byte[] encrypt(String plaintext) {
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

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            byte[] result = new byte[iv.length + salt.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(salt, 0, result, iv.length, salt.length);
            System.arraycopy(encrypted, 0, result, iv.length + salt.length, encrypted.length);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private static String decrypt(byte[] data) {
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
                return "zaga-" + hwid + "-config";
            }
        } catch (Exception ignored) {}
        return null;
    }
}
