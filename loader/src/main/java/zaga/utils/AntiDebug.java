package zaga.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

public class AntiDebug {
    private static final String[] DEBUG_FLAGS = {
        "-agentlib:jdwp",
        "-agentlib:dt_socket",
        "-Xdebug",
        "-Xrunjdwp",
        "-agentpath:",
        "-javaagent:",
        "-Dcom.sun.management.jmxremote",
        "-Dvisualvm.id"
    };

    private static final String[] DEBUG_PROCESSES = {
        "jdb.exe", "jdb",
        "ida.exe", "ida64.exe",
        "x64dbg.exe", "x32dbg.exe",
        "ollydbg.exe",
        "gdb", "lldb",
        "frida", "frida-server",
        "jd-gui",
        "procyon",
        "cfr",
        "fernflower"
    };

    public static boolean isDebuggerDetected() {
        return checkJvmArgs() || checkProcessList() || checkTiming() || checkEnvironment();
    }

    private static boolean checkJvmArgs() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        List<String> args = runtime.getInputArguments();
        String argsStr = String.join(" ", args).toLowerCase();

        for (String flag : DEBUG_FLAGS) {
            if (argsStr.contains(flag.toLowerCase())) {
                return true;
            }
        }

        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement el : stack) {
            if (el.getClassName().contains("jdwp") || el.getClassName().contains("debug")) {
                return true;
            }
        }

        return false;
    }

    private static boolean checkProcessList() {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("tasklist", "/FO", "CSV");
            } else {
                pb = new ProcessBuilder("ps", "aux");
            }
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes()).toLowerCase();
            p.waitFor();

            for (String proc : DEBUG_PROCESSES) {
                if (output.contains(proc.toLowerCase())) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static boolean checkTiming() {
        long start = System.nanoTime();
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        long elapsed = System.nanoTime() - start;
        return elapsed > 200_000_000;
    }

    private static boolean checkEnvironment() {
        String debug = System.getenv("JAVA_TOOL_OPTIONS");
        if (debug != null && debug.toLowerCase().contains("jdwp")) return true;

        String agentPath = System.getProperty("sun.java.command", "");
        if (agentPath.contains("agentlib") || agentPath.contains("javaagent")) return true;

        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("wmic", "process", "where",
                    "name='java.exe'", "get", "commandline");
            } else {
                pb = new ProcessBuilder("bash", "-c", "cat /proc/self/cmdline");
            }
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String cmd = new String(p.getInputStream().readAllBytes()).toLowerCase();
            p.waitFor();
            if (cmd.contains("jdwp") || cmd.contains("debug")) return true;
        } catch (Exception ignored) {}

        return false;
    }

    public static String getBlockedMessage() {
        return "Debugging detected. Launch aborted.";
    }
}
