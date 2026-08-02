package zaga.crypto;

import java.io.*;
import java.nio.file.*;
import java.util.Base64;

public class EncryptJar {
    private static final Path KEY_FILE = Path.of(System.getProperty("user.home"), ".zaga", "jar.key");

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java zaga.crypto.EncryptJar <input.jar> <output.jar.encrypted> [base64key]");
            System.out.println("If key is omitted, reads from ~/.zaga/jar.key or generates a new one.");
            System.exit(1);
        }

        Path inputPath = Path.of(args[0]);
        Path outputPath = Path.of(args[1]);

        if (!Files.exists(inputPath)) {
            System.err.println("Input file not found: " + inputPath);
            System.exit(1);
        }

        byte[] keyBytes;
        String base64Key;

        if (args.length >= 3 && args[2] != null && !args[2].isBlank()) {
            base64Key = args[2];
            keyBytes = Base64.getDecoder().decode(base64Key);
            System.out.println("Using provided key.");
        } else if (Files.exists(KEY_FILE)) {
            base64Key = Files.readString(KEY_FILE).trim();
            keyBytes = Base64.getDecoder().decode(base64Key);
            System.out.println("Using key from " + KEY_FILE);
        } else {
            keyBytes = JarDecryptor.generateKey();
            base64Key = Base64.getEncoder().encodeToString(keyBytes);
            Files.createDirectories(KEY_FILE.getParent());
            Files.writeString(KEY_FILE, base64Key);
            System.out.println("Generated new key, saved to " + KEY_FILE);
        }

        byte[] jarData = Files.readAllBytes(inputPath);
        byte[] encrypted = JarDecryptor.encrypt(jarData, keyBytes);

        Files.write(outputPath, encrypted);

        System.out.println("Encrypted: " + outputPath);
        System.out.println("Key (for Cloudflare Workers JAR_ENCRYPTION_KEY):");
        System.out.println(base64Key);
    }
}
