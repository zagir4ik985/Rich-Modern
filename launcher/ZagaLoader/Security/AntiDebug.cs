using System;
using System.Diagnostics;
using System.Linq;
using System.Runtime.InteropServices;
using System.Threading;

namespace ZagaLoader.Security;

internal static class AntiDebug
{
    private static readonly string[] DebuggedProcessNames = new[]
    {
        "dnspy", "dnspy64", "dnspy-x86",
        "decompiler", "ilspy",
        "x64dbg", "x32dbg", "ollydbg",
        "ida", "ida64", "idag", "idag64",
        "windbg", "cdb", "ntsd",
        "processhacker", "process hacker",
        "fiddler", "httpanalyzer",
        "reflector", "dotpeek",
        "cheatengine", "cheat engine",
        "httpdebuggerpro",
        "charles",
        "wireshark",
        "httpdebugger"
    };

    private static readonly string[] DebuggedWindowTitles = new[]
    {
        "dnspy", "ilspy", "decompiler",
        "x64dbg", "x32dbg", "ollydbg",
        "ida", "windbg",
        "process hacker", "processhacker",
        "fiddler", "cheat engine"
    };

    private static Timer? _monitorTimer;
    private static volatile bool _shutdownRequested;

    public static void Initialize()
    {
        if (IsDebuggerAttached() || IsDebuggedProcessRunning() || IsTimingTampered())
        {
            EmergencyShutdown();
            return;
        }

        _monitorTimer = new Timer(_ =>
        {
            if (_shutdownRequested) return;
            if (IsDebuggerAttached() || IsDebuggedProcessRunning())
            {
                EmergencyShutdown();
            }
        }, null, 3000, 3000);
    }

    public static void Stop()
    {
        _shutdownRequested = true;
        _monitorTimer?.Dispose();
    }

    private static bool IsDebuggerAttached()
    {
        try
        {
            if (Debugger.IsAttached) return true;
            if (Debugger.IsLogging()) return true;

            if (IsWindows())
            {
                try
                {
                    var process = Process.GetCurrentProcess();
                    if (process != null)
                    {
                        IntPtr debugPort = IntPtr.Zero;
                        int hr = CheckRemoteDebuggerPresent(process.Handle, out bool isDebugger);
                        if (hr == 0 && isDebugger) return true;
                    }
                }
                catch { }
            }

            return false;
        }
        catch { return false; }
    }

    private static bool IsDebuggedProcessRunning()
    {
        try
        {
            var processes = Process.GetProcesses();
            foreach (var proc in processes)
            {
                try
                {
                    var name = proc.ProcessName.ToLowerInvariant();
                    if (DebuggedProcessNames.Any(d => name.Contains(d)))
                    {
                        return true;
                    }

                    if (proc.MainWindowTitle != null)
                    {
                        var title = proc.MainWindowTitle.ToLowerInvariant();
                        if (DebuggedWindowTitles.Any(d => title.Contains(d)))
                        {
                            return true;
                        }
                    }
                }
                catch { }
                finally
                {
                    try { proc.Dispose(); } catch { }
                }
            }
            return false;
        }
        catch { return false; }
    }

    private static bool IsTimingTampered()
    {
        try
        {
            var sw = Stopwatch.StartNew();
            Thread.Sleep(100);
            sw.Stop();

            var elapsed = sw.ElapsedMilliseconds;
            return elapsed > 500;
        }
        catch { return false; }
    }

    private static void EmergencyShutdown()
    {
        _shutdownRequested = true;
        _monitorTimer?.Dispose();

        try
        {
            Environment.FailFast(null);
        }
        catch { }

        try
        {
            Process.GetCurrentProcess().Kill();
        }
        catch { }

        Environment.Exit(-1);
    }

    private static bool IsWindows()
    {
        return RuntimeInformation.IsOSPlatform(OSPlatform.Windows);
    }

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern int CheckRemoteDebuggerPresent(IntPtr hProcess, out bool isDebuggerPresent);
}
