using System;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Security.Cryptography;
using System.Text;

namespace ZagaLoader.Security;

internal static class AntiTamper
{
    private static byte[]? _expectedChecksum;
    private static bool _verified;

    private static readonly string[] CriticalAssemblies = new[]
    {
        "ZagaLoader.dll",
        "ZagaUpdater.dll"
    };

    private static readonly string[] CriticalResourceNames = new[]
    {
        "rockstar-1.0.0.jar.encrypted",
        "web.index.html"
    };

    public static void Initialize()
    {
        try
        {
            _expectedChecksum = ComputeCriticalChecksum();
            Verify();
        }
        catch
        {
            EmergencyShutdown();
        }
    }

    public static void Verify()
    {
        if (_verified) return;
        if (_expectedChecksum == null) return;

        try
        {
            var currentChecksum = ComputeCriticalChecksum();
            if (currentChecksum == null || !CompareByteArrays(currentChecksum, _expectedChecksum))
            {
                EmergencyShutdown();
            }
            _verified = true;
        }
        catch
        {
            EmergencyShutdown();
        }
    }

    private static byte[]? ComputeCriticalChecksum()
    {
        try
        {
            using var sha = SHA256.Create();
            using var ms = new MemoryStream();

            var assembly = Assembly.GetExecutingAssembly();
            using (var stream = assembly.GetManifestResourceStream(assembly.GetManifestResourceNames()[0] ?? ""))
            {
                if (stream != null)
                {
                    var buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = stream.Read(buffer, 0, buffer.Length)) > 0)
                    {
                        ms.Write(buffer, 0, bytesRead);
                    }
                }
            }

            foreach (var resName in CriticalResourceNames)
            {
                try
                {
                    using var resStream = assembly.GetManifestResourceStream(resName);
                    if (resStream != null)
                    {
                        var buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = resStream.Read(buffer, 0, buffer.Length)) > 0)
                        {
                            ms.Write(buffer, 0, bytesRead);
                        }
                    }
                }
                catch { }
            }

            ms.Position = 0;
            return sha.ComputeHash(ms);
        }
        catch { return null; }
    }

    private static bool CompareByteArrays(byte[] a, byte[] b)
    {
        if (a.Length != b.Length) return false;
        int result = 0;
        for (int i = 0; i < a.Length; i++)
        {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    private static void EmergencyShutdown()
    {
        try
        {
            Process.GetCurrentProcess().Kill();
        }
        catch { }

        Environment.FailFast("Security violation detected");
    }
}
