package zaga.utils;

import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HwidUtil {
    private static final Path HWID_CACHE = Path.of(System.getProperty("user.home"), ".zaga", ".hwid");

    public static String getHwid() {
        try {
            String cached = readCached();
            if (cached != null) return cached;

            String raw = collectFingerprint();
            String hash = sha256(raw);
            saveCached(hash);
            return hash;
        } catch (Exception e) {
            return "fallback-" + System.getProperty("user.name", "unknown");
        }
    }

    private static String collectFingerprint() throws Exception {
        StringBuilder sb = new StringBuilder();

        sb.append(getCpuId());
        sb.append("|");
        sb.append(getMotherboardSerial());
        sb.append("|");
        sb.append(getDiskSerial());
        sb.append("|");
        sb.append(getMacAddress());

        return sb.toString();
    }

    private static String getCpuId() {
        Process p = null;
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("wmic", "cpu", "get", "ProcessorId");
            } else {
                pb = new ProcessBuilder("cat", "/proc/cpuinfo");
            }
            pb.redirectErrorStream(true);
            p = pb.start();
            String output = new String(p.getInputStream().readAllBytes());
            p.waitFor();
            if (os.contains("win")) {
                String[] lines = output.split("\\R");
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("ProcessorId")) {
                        return line;
                    }
                }
            } else {
                for (String line : output.split("\\R")) {
                    if (line.startsWith("Serial")) return line;
                }
            }
        } catch (Exception e) {
            return "cpu-unknown";
        } finally {
            if (p != null) try { p.destroy(); } catch (Exception ignored) {}
        }
        return "cpu-unknown";
    }

    private static String getMotherboardSerial() {
        Process p = null;
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("wmic", "baseboard", "get", "SerialNumber");
            } else {
                pb = new ProcessBuilder("cat", "/sys/class/dmi/id/board_serial");
            }
            pb.redirectErrorStream(true);
            p = pb.start();
            String output = new String(p.getInputStream().readAllBytes());
            p.waitFor();
            String[] lines = output.split("\\R");
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("Serial")) {
                    return line;
                }
            }
        } catch (Exception e) {
            return "mb-unknown";
        } finally {
            if (p != null) try { p.destroy(); } catch (Exception ignored) {}
        }
        return "mb-unknown";
    }

    private static String getDiskSerial() {
        Process p = null;
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("wmic", "diskdrive", "get", "SerialNumber");
            } else {
                pb = new ProcessBuilder("lsblk", "-dno", "SERIAL", "/dev/sda");
            }
            pb.redirectErrorStream(true);
            p = pb.start();
            String output = new String(p.getInputStream().readAllBytes());
            p.waitFor();
            String[] lines = output.split("\\R");
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("Serial")) {
                    return line;
                }
            }
        } catch (Exception e) {
            return "disk-unknown";
        } finally {
            if (p != null) try { p.destroy(); } catch (Exception ignored) {}
        }
        return "disk-unknown";
    }

    private static String getMacAddress() throws Exception {
        List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface ni : interfaces) {
            if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) continue;
            byte[] mac = ni.getHardwareAddress();
            if (mac != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < mac.length; i++) {
                    sb.append(String.format("%02X", mac[i]));
                    if (i < mac.length - 1) sb.append(":");
                }
                return sb.toString();
            }
        }
        return "mac-unknown";
    }

    private static String sha256(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String readCached() {
        try {
            if (Files.exists(HWID_CACHE)) {
                String cached = Files.readString(HWID_CACHE).trim();
                if (!cached.isEmpty()) return cached;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void saveCached(String hwid) {
        try {
            Files.createDirectories(HWID_CACHE.getParent());
            Files.writeString(HWID_CACHE, hwid);
        } catch (Exception ignored) {}
    }
}
