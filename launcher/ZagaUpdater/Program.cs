using System;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace ZagaUpdater;

class Program
{
    private const string GitHubReleasesUrl = "https://api.github.com/repos/zagir4ik985/zagaDLC-releases/releases";
    private static readonly HttpClient Http;
    private static readonly string ZagaDir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".zaga");
    private static readonly string LoaderJar = Path.Combine(ZagaDir, "loader.jar");
    private static readonly string VersionFile = Path.Combine(ZagaDir, "version.json");

    static Program()
    {
        Http = new HttpClient();
        Http.DefaultRequestHeaders.UserAgent.ParseAdd("zagaDLC-Updater/1.0.5");
    }

    static async Task<int> Main(string[] args)
    {
        Console.WriteLine("zagaDLC Updater v1.0.5");
        Console.WriteLine("======================");
        Console.WriteLine();

        try
        {
            var needsUpdate = await CheckVersion();
            if (needsUpdate)
            {
                Console.WriteLine("Downloading update...");
                await DownloadUpdate();
                Console.WriteLine("Update complete.");
            }
            else
            {
                Console.WriteLine("Already up to date.");
            }

            if (!File.Exists(LoaderJar))
            {
                Console.WriteLine("ERROR: loader.jar not found after update.");
                return 1;
            }

            Console.WriteLine("Launching loader...");
            var psi = new ProcessStartInfo
            {
                FileName = "java",
                Arguments = $"-jar \"{LoaderJar}\"",
                UseShellExecute = false
            };
            Process.Start(psi);
            return 0;
        }
        catch (Exception ex)
        {
            Console.WriteLine($"ERROR: {ex.Message}");
            return 1;
        }
    }

    static async Task<bool> CheckVersion()
    {
        for (int attempt = 1; attempt <= 3; attempt++)
        {
            try
            {
                var resp = await Http.GetAsync(GitHubReleasesUrl);
                if (!resp.IsSuccessStatusCode)
                {
                    Console.WriteLine($"Attempt {attempt}: HTTP {(int)resp.StatusCode}");
                    if (attempt < 3) await Task.Delay(1000 * attempt);
                    continue;
                }

                var body = await resp.Content.ReadAsStringAsync();
                var releases = JsonSerializer.Deserialize<List<GitHubRelease>>(body);

                var latestRelease = releases?.FirstOrDefault();
                if (latestRelease?.tag_name == null)
                {
                    Console.WriteLine("No releases found.");
                    return false;
                }

                var remoteVersion = latestRelease.tag_name.TrimStart('v');
                var asset = latestRelease.assets?.FirstOrDefault(a => a.name?.EndsWith(".jar") == true);

                if (asset?.browser_download_url == null)
                {
                    Console.WriteLine("No loader.jar in latest release.");
                    return false;
                }

                if (File.Exists(VersionFile))
                {
                    try
                    {
                        var localData = JsonSerializer.Deserialize<JsonElement>(File.ReadAllText(VersionFile));
                        var localVersion = localData.GetProperty("version").GetString() ?? "";
                        if (localVersion == remoteVersion && File.Exists(LoaderJar))
                        {
                            return false;
                        }
                    }
                    catch { }
                }

                Console.WriteLine($"New version: {remoteVersion}");
                return true;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Attempt {attempt} failed: {ex.Message}");
                if (attempt < 3) await Task.Delay(1000 * attempt);
            }
        }

        Console.WriteLine("Could not check version after 3 attempts.");
        return false;
    }

    static async Task DownloadUpdate()
    {
        for (int attempt = 1; attempt <= 3; attempt++)
        {
            try
            {
                var resp = await Http.GetAsync(GitHubReleasesUrl);
                resp.EnsureSuccessStatusCode();
                var body = await resp.Content.ReadAsStringAsync();
                var releases = JsonSerializer.Deserialize<List<GitHubRelease>>(body);

                var latestRelease = releases?.FirstOrDefault();
                if (latestRelease?.tag_name == null) return;

                var remoteVersion = latestRelease.tag_name.TrimStart('v');
                var asset = latestRelease.assets?.FirstOrDefault(a => a.name?.EndsWith(".jar") == true);
                if (asset?.browser_download_url == null) return;

                string? expectedHash = null;
                if (latestRelease.body != null)
                {
                    var hashMatch = System.Text.RegularExpressions.Regex.Match(
                        latestRelease.body, @"SHA256:\s*([a-fA-F0-9]{64})");
                    if (hashMatch.Success)
                        expectedHash = hashMatch.Groups[1].Value.ToLowerInvariant();
                }

                Console.WriteLine($"Downloading {asset.name} (attempt {attempt})...");
                var bytes = await Http.GetByteArrayAsync(asset.browser_download_url);

                if (!string.IsNullOrEmpty(expectedHash))
                {
                    using var sha = SHA256.Create();
                    var actualHash = BitConverter.ToString(sha.ComputeHash(bytes)).Replace("-", "").ToLowerInvariant();

                    if (actualHash != expectedHash)
                    {
                        Console.WriteLine($"HASH MISMATCH! Expected: {expectedHash}");
                        Console.WriteLine($"Actual: {actualHash}");
                        Console.WriteLine("Download may be tampered. Aborting.");
                        throw new Exception("Hash verification failed — possible tampering");
                    }
                    Console.WriteLine("Hash verified OK.");
                }
                else
                {
                    Console.WriteLine("WARNING: No hash in release body — skipping verification.");
                }

                Directory.CreateDirectory(ZagaDir);
                var tempFile = LoaderJar + ".tmp";
                await File.WriteAllBytesAsync(tempFile, bytes);
                if (File.Exists(LoaderJar)) File.Delete(LoaderJar);
                File.Move(tempFile, LoaderJar);

                var versionSave = new { version = remoteVersion };
                File.WriteAllText(VersionFile, JsonSerializer.Serialize(versionSave));

                Console.WriteLine("Download complete.");
                return;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Attempt {attempt} failed: {ex.Message}");
                if (attempt < 3) await Task.Delay(2000 * attempt);
            }
        }

        throw new Exception("Download failed after 3 attempts");
    }
}

public class GitHubRelease
{
    public string? tag_name { get; set; }
    public GitHubAsset[]? assets { get; set; }
    public string? body { get; set; }
}

public class GitHubAsset
{
    public string? browser_download_url { get; set; }
    public string? name { get; set; }
}
