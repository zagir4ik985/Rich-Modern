using System;
using System.Buffers;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Threading;
using System.Threading.Tasks;

namespace ZagaLoader;

public class MinecraftLauncher
{
    private const string VERSION = "1.21.11";
    private const string FABRIC_VERSION = "0.18.4";
    private const string MOJANG_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private const string JAVA_URL = "https://download.java.net/java/GA/jdk21.0.2/f2283984656d49d69e91c558476027ac/13/GPL/openjdk-21.0.2_windows-x64_bin.zip";
    private const string FABRIC_INSTALLER_URL = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.1/fabric-installer-1.0.1.jar";
    private const string API_BASE = "https://zagadlc.zagir4ik985.workers.dev";

    private readonly string _mcDir;
    private readonly string _baseDir;
    private readonly string _javaDir;
    private readonly HttpClient _http;
    private readonly Action<string, int>? _onProgress;

    public MinecraftLauncher(string mcDir, HttpClient http, Action<string, int>? onProgress = null)
    {
        _mcDir = mcDir;
        _baseDir = Path.Combine(mcDir, "game");
        _javaDir = Path.Combine(mcDir, "java");
        _http = http;
        _onProgress = onProgress;
    }

    private void SetStatus(string status, int progress = -1)
    {
        _onProgress?.Invoke(status, progress);
    }

    private static JsonObject ParseJson(string json, string context)
    {
        return JsonNode.Parse(json)?.AsObject()
            ?? throw new Exception($"Corrupted {context} data");
    }

    // ==================== JAVA ====================

    public async Task<string> EnsureJava()
    {
        var javaExe = FindJavaExe(_javaDir);
        if (javaExe != null) return javaExe;

        Directory.CreateDirectory(_mcDir);
        Directory.CreateDirectory(_baseDir);

        SetStatus("Downloading Java 21...", 0);
        var zipFile = Path.Combine(_mcDir, "java.zip");
        if (!File.Exists(zipFile) || new FileInfo(zipFile).Length < 180_000_000)
        {
            await Download(JAVA_URL, zipFile, 0, 50);
        }
        else
        {
            SetStatus("Java zip exists, extracting...", 50);
        }

        SetStatus("Extracting Java...", 50);
        await ExtractZip(zipFile, _javaDir, 50, 95);
        try { File.Delete(zipFile); } catch { }

        javaExe = FindJavaExe(_javaDir);
        if (javaExe == null) throw new Exception("Java extraction failed");

        SetStatus("Java ready", 100);
        return javaExe;
    }

    private string? FindJavaExe(string dir)
    {
        var bin = Path.Combine(dir, "bin", "java.exe");
        if (File.Exists(bin)) return bin;

        if (Directory.Exists(dir))
        {
            foreach (var sub in Directory.GetDirectories(dir))
            {
                var found = FindJavaExe(sub);
                if (found != null) return found;
            }
        }
        return null;
    }

    // ==================== MINECRAFT ====================

    public async Task InstallMinecraft(string jwtToken)
    {
        var versionsDir = Path.Combine(_baseDir, "versions");
        var librariesDir = Path.Combine(_baseDir, "libraries");
        var assetsDir = Path.Combine(_baseDir, "assets");
        var versionDir = Path.Combine(versionsDir, VERSION);
        var versionJson = Path.Combine(versionDir, VERSION + ".json");
        var versionJar = Path.Combine(versionDir, VERSION + ".jar");

        JsonObject? versionData = null;

        if (File.Exists(versionJson) && File.Exists(versionJar) && new FileInfo(versionJar).Length > 0)
        {
            var existingJson = await File.ReadAllTextAsync(versionJson);
            versionData = ParseJson(existingJson, "version");
        }
        else
        {
            SetStatus($"Downloading Minecraft {VERSION}...", 0);

            var manifestJson = await DownloadString(MOJANG_MANIFEST);
            var manifest = ParseJson(manifestJson, "manifest");
            var versions = manifest["versions"]!.AsArray();

            string? versionUrl = null;
            foreach (var v in versions)
            {
                if (v!["id"]!.GetValue<string>() == VERSION)
                {
                    versionUrl = v["url"]!.GetValue<string>();
                    break;
                }
            }
            if (versionUrl == null) throw new Exception($"Version {VERSION} not found");

            var versionJsonStr = await DownloadString(versionUrl);
            versionData = ParseJson(versionJsonStr, "version");

            Directory.CreateDirectory(versionDir);
            await File.WriteAllTextAsync(versionJson, versionJsonStr);

            var clientUrl = versionData["downloads"]!["client"]!["url"]!.GetValue<string>();
            await Download(clientUrl, Path.Combine(versionDir, VERSION + ".jar"), 0, 40);

            SetStatus("Downloading libraries...", 40);
            Directory.CreateDirectory(librariesDir);
            await DownloadLibraries(versionData["libraries"]!.AsArray(), librariesDir, 40, 75);
        }

        SetStatus("Downloading assets...", 75);
        Directory.CreateDirectory(assetsDir);
        await DownloadAssets(versionData!, assetsDir, 75, 100);

        SetStatus("Installing Fabric...", 100);
        await InstallFabric(versionsDir, librariesDir, jwtToken);

        SetStatus("Installing mod...", 100);
        await InstallMod(versionsDir, jwtToken);
    }

    private async Task DownloadLibraries(JsonArray libraries, string librariesDir, int startPct, int endPct)
    {
        var toDownload = new List<(string url, string path)>();

        foreach (var lib in libraries)
        {
            if (lib?["downloads"]?["artifact"] == null) continue;
            var path = lib["downloads"]!["artifact"]!["path"]!.GetValue<string>();
            var url = lib["downloads"]!["artifact"]!["url"]!.GetValue<string>();
            var fullPath = Path.Combine(librariesDir, path);
            if (!File.Exists(fullPath))
            {
                toDownload.Add((url, fullPath));
            }
        }

        if (toDownload.Count == 0) return;

        int done = 0;
        int total = toDownload.Count;
        int maxThreads = 8;
        using var semaphore = new SemaphoreSlim(maxThreads);

        var tasks = toDownload.Select(async item =>
        {
            await semaphore.WaitAsync();
            try
            {
                var dir = Path.GetDirectoryName(item.path);
                if (dir != null) Directory.CreateDirectory(dir);
                await Download(item.url, item.path, reportProgress: false);
                var pct = startPct + (int)((double)Interlocked.Increment(ref done) / total * (endPct - startPct));
                SetStatus($"Libraries: {done}/{total}", pct);
            }
            finally { semaphore.Release(); }
        });

        await Task.WhenAll(tasks);
    }

    private async Task DownloadAssets(JsonObject versionData, string assetsDir, int startPct, int endPct)
    {
        var assetIndexDir = Path.Combine(assetsDir, "indexes");
        Directory.CreateDirectory(assetIndexDir);

        var assetIndex = versionData["assetIndex"]?.AsObject();
        if (assetIndex == null) return;

        var assetId = assetIndex["id"]?.GetValue<string>();
        var assetUrl = assetIndex["url"]?.GetValue<string>();
        if (assetId == null || assetUrl == null) return;

        var indexFile = Path.Combine(assetIndexDir, assetId + ".json");
        if (!File.Exists(indexFile))
        {
            await Download(assetUrl, indexFile);
        }

        var indexJson = await File.ReadAllTextAsync(indexFile);
        var index = ParseJson(indexJson, "asset index");
        var objects = index["objects"]?.AsObject();
        if (objects == null) return;

        int total = objects.Count;
        int done = 0;
        int maxThreads = 8;
        using var semaphore = new SemaphoreSlim(maxThreads);
        var assetObjectsBase = Path.Combine(assetsDir, "objects");
        Directory.CreateDirectory(assetObjectsBase);

        var tasks = objects.Select(async kvp =>
        {
            await semaphore.WaitAsync();
            try
            {
                var obj = kvp.Value!.AsObject();
                var hash = obj["hash"]!.GetValue<string>();
                var subDir = hash.Substring(0, 2);
                var objPath = Path.Combine(assetObjectsBase, subDir, hash);
                if (!File.Exists(objPath))
                {
                    Directory.CreateDirectory(Path.GetDirectoryName(objPath)!);
                    await Download($"https://resources.download.minecraft.net/{subDir}/{hash}", objPath, reportProgress: false);
                }
                var pct = startPct + (int)((double)Interlocked.Increment(ref done) / total * (endPct - startPct));
                SetStatus($"Assets: {done}/{total}", pct);
            }
            finally { semaphore.Release(); }
        });

        await Task.WhenAll(tasks);
    }

    // ==================== FABRIC ====================

    private async Task InstallFabric(string versionsDir, string librariesDir, string jwtToken)
    {
        var existing = Directory.GetDirectories(versionsDir, $"*loader*{FABRIC_VERSION}*").FirstOrDefault()
                    ?? Directory.GetDirectories(versionsDir, $"*{VERSION}*fabric*").FirstOrDefault();
        if (existing != null) return;

        var installerJar = Path.Combine(_mcDir, "fabric-installer.jar");
        if (!File.Exists(installerJar))
        {
            await Download(FABRIC_INSTALLER_URL, installerJar);
        }

        var javaExe = FindJavaExe(_javaDir);
        if (javaExe == null) throw new Exception("Java not found");

        var psi = new ProcessStartInfo
        {
            FileName = javaExe,
            Arguments = $"-jar \"{installerJar}\" client -dir \"{_baseDir}\" -mcversion {VERSION} -loader {FABRIC_VERSION} -noprofile",
            UseShellExecute = false,
            CreateNoWindow = true,
            RedirectStandardOutput = true,
            RedirectStandardError = true
        };

        var proc = Process.Start(psi) ?? throw new Exception("Failed to start Java process for Fabric installation");
        await proc.WaitForExitAsync();
        var exitCode = proc.ExitCode;
        proc.Dispose();
        if (exitCode != 0)
        {
            throw new Exception($"Fabric install failed");
        }
    }

    // ==================== MOD ====================

    private async Task InstallMod(string versionsDir, string jwtToken)
    {
        var fabricModsDir = Path.Combine(_baseDir, "mods");
        Directory.CreateDirectory(fabricModsDir);

        var fabricApiFile = Path.Combine(fabricModsDir, "fabric-api.jar");
        if (!File.Exists(fabricApiFile))
        {
            SetStatus("Downloading fabric-api...", 40);
            await Download("https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.141.2%2B1.21.11/fabric-api-0.141.2%2B1.21.11.jar", fabricApiFile, 40, 60);
        }

        var baritoneFile = Path.Combine(fabricModsDir, "baritone-api-fabric-1.13.1.jar");
        var assembly = System.Reflection.Assembly.GetExecutingAssembly();
        if (!File.Exists(baritoneFile))
        {
            SetStatus("Extracting baritone...", 60);
            using var stream = assembly.GetManifestResourceStream("baritone-api-fabric-1.13.1.jar")
                ?? throw new Exception("Baritone not found in launcher resources");
            using var fileStream = File.Create(baritoneFile);
            await stream.CopyToAsync(fileStream);
        }

        var modFile = Path.Combine(fabricModsDir, "rockstar-1.0.0.jar");

        // Check for mod update from API
        await CheckAndUpdateMod(jwtToken);

        SetStatus("Extracting mod...", 50);

        // Always extract fresh encrypted JAR from embedded resources
        var encFile = Path.Combine(_mcDir, "rockstar-1.0.0.jar.encrypted");
        var encResourceName = "rockstar-1.0.0.jar.encrypted";
        using (var encStream = assembly.GetManifestResourceStream(encResourceName)
            ?? throw new Exception("Encrypted mod not found in launcher resources"))
        using (var fileStream = File.Create(encFile))
        {
            await encStream.CopyToAsync(fileStream);
        }

        SetStatus("Decrypting mod...", 75);

        // Get encryption key from API
        var req = new HttpRequestMessage(HttpMethod.Get, $"{API_BASE}/api/key");
        req.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", jwtToken);
        var resp = await _http.SendAsync(req);
        resp.EnsureSuccessStatusCode();
        var body = await resp.Content.ReadAsStringAsync();
        var json = ParseJson(body, "API response");
        var keyBase64 = json["key"]?.GetValue<string>();
        if (string.IsNullOrEmpty(keyBase64)) throw new Exception("No encryption key returned.");

        // Save key for future use (DPAPI protected)
        var keyFile = Path.Combine(_mcDir, "jar.key");
        Directory.CreateDirectory(_mcDir);
        var keyBytes = System.Text.Encoding.UTF8.GetBytes(keyBase64);
        var dpapiEntropy = new byte[] { 0x5A, 0x61, 0x67, 0x61, 0x44, 0x4C, 0x43, 0x76, 0x32, 0x2E, 0x30 };
        var protectedKey = System.Security.Cryptography.ProtectedData.Protect(keyBytes, dpapiEntropy, System.Security.Cryptography.DataProtectionScope.CurrentUser);
        File.WriteAllBytes(keyFile, protectedKey);

        await DecryptMod(encFile, modFile, keyBase64);
        SetStatus("Mod ready", 100);
    }

    private async Task DecryptMod(string encFile, string outFile, string keyBase64)
    {
        byte[] keyBytes;
        try { keyBytes = Convert.FromBase64String(keyBase64.Trim()); }
        catch { throw new Exception("Invalid encryption key format from server."); }
        if (keyBytes.Length != 16 && keyBytes.Length != 24 && keyBytes.Length != 32)
            throw new Exception("Invalid encryption key length: " + keyBytes.Length);

        var fi = new FileInfo(encFile);
        if (fi.Length < 28)
            throw new Exception("Encrypted mod file is corrupted (too small: " + fi.Length + " bytes)");

        var dir = Path.GetDirectoryName(outFile);
        if (dir != null) Directory.CreateDirectory(dir);

        using var encStream = new FileStream(encFile, FileMode.Open, FileAccess.Read, FileShare.Read);

        var iv = new byte[12];
        await encStream.ReadExactlyAsync(iv);

        var tag = new byte[16];
        encStream.Seek(-16, SeekOrigin.End);
        await encStream.ReadExactlyAsync(tag);

        encStream.Seek(12, SeekOrigin.Begin);
        var cipherLen = (int)(fi.Length - 12 - 16);
        var cipherBytes = ArrayPool<byte>.Shared.Rent(cipherLen);
        try
        {
            int read = 0;
            while (read < cipherLen)
                read += await encStream.ReadAsync(cipherBytes.AsMemory(read, cipherLen - read));

            var plainBytes = ArrayPool<byte>.Shared.Rent(cipherLen);
            try
            {
                using var aesGcm = new AesGcm(keyBytes, 16);
                aesGcm.Decrypt(iv, cipherBytes.AsSpan(0, cipherLen), tag, plainBytes.AsSpan(0, cipherLen));

                using var outStream = new FileStream(outFile, FileMode.Create, FileAccess.Write);
                await outStream.WriteAsync(plainBytes.AsMemory(0, cipherLen));
            }
            finally { ArrayPool<byte>.Shared.Return(plainBytes); }
        }
        finally { ArrayPool<byte>.Shared.Return(cipherBytes); }
    }

    // ==================== LAUNCH ====================

    public async Task Launch(string javaExe, string jwtToken, string apiUrl, string username = "Player", Action? onStarted = null)
    {
        var versionsDir = Path.Combine(_baseDir, "versions");
        var librariesDir = Path.Combine(_baseDir, "libraries");
        var assetsDir = Path.Combine(_baseDir, "assets");

        var fabricVersionDir = Directory.GetDirectories(versionsDir, $"*loader*{FABRIC_VERSION}*").FirstOrDefault();
        if (fabricVersionDir == null)
            fabricVersionDir = Directory.GetDirectories(versionsDir, $"*{VERSION}*fabric*").FirstOrDefault();
        if (fabricVersionDir == null) throw new Exception("Fabric not installed");

        var fabricProfileName = $"{VERSION}-fabric-{FABRIC_VERSION}";
        var fabricJson = Path.Combine(fabricVersionDir, fabricProfileName + ".json");
        if (!File.Exists(fabricJson))
        {
            var jsonFiles = Directory.GetFiles(fabricVersionDir, "*.json");
            if (jsonFiles.Length > 0) fabricJson = jsonFiles[0];
            else throw new Exception("Fabric JSON not found");
        }

        var fabricData = ParseJson(await File.ReadAllTextAsync(fabricJson), "Fabric JSON");
        var mainClass = fabricData["mainClass"]?.GetValue<string>() ?? "net.fabricmc.loader.impl.launch.knot.KnotClient";

        var classpath = new Dictionary<string, string>();

        var mcVersionJson = Path.Combine(versionsDir, VERSION, VERSION + ".json");
        if (File.Exists(mcVersionJson))
        {
            var mcData = ParseJson(await File.ReadAllTextAsync(mcVersionJson), "Minecraft version");
            if (mcData["libraries"] != null)
            {
                foreach (var lib in mcData["libraries"]!.AsArray())
                {
                    var name = lib?["name"]?.GetValue<string>();
                    if (name != null && !name.Contains("natives-"))
                    {
                        var path = ConvertMavenPath(name, librariesDir);
                        if (path != null && File.Exists(path))
                        {
                            var key = GetLibraryKey(name);
                            classpath[key] = path;
                        }
                    }
                }
            }
        }

        if (fabricData["libraries"] != null)
        {
            foreach (var lib in fabricData["libraries"]!.AsArray())
            {
                var name = lib?["name"]?.GetValue<string>();
                if (name != null && !name.Contains("natives-"))
                {
                    var path = ConvertMavenPath(name, librariesDir);
                    if (path != null && File.Exists(path))
                    {
                        var key = GetLibraryKey(name);
                        classpath[key] = path;
                    }
                }
            }
        }

        var mcJar = Path.Combine(versionsDir, VERSION, VERSION + ".jar");
        if (File.Exists(mcJar)) classpath["minecraft"] = mcJar;

        var classpathStr = string.Join(";", classpath.Values);

        var gameDir = _baseDir;
        var assetsIndexPath = Path.Combine(assetsDir, "indexes", "legacy.json");
        if (!File.Exists(assetsIndexPath))
        {
            var indexFiles = Directory.GetFiles(Path.Combine(assetsDir, "indexes"), "*.json");
            if (indexFiles.Length > 0) assetsIndexPath = indexFiles[0];
        }

        var nativesDir = Path.Combine(librariesDir, "natives");
        if (!Directory.Exists(nativesDir) || Directory.GetFiles(nativesDir).Length == 0)
        {
            Directory.CreateDirectory(nativesDir);
            var nativeJars = Directory.GetFiles(librariesDir, "*natives-windows*.jar", SearchOption.AllDirectories);
            foreach (var nj in nativeJars)
            {
                try { ZipFile.ExtractToDirectory(nj, nativesDir, true); } catch { }
            }
        }

        var args = new StringBuilder();
        args.Append($"-Xmx2G -Xms512M ");
        args.Append($"-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 ");
        args.Append($"-XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC ");
        args.Append($"-XX:G1HeapRegionSize=16M -XX:G1ReservePercent=25 ");
        args.Append($"-XX:InitiatingHeapOccupancyPercent=20 -XX:G1NewSizePercent=30 ");
        args.Append($"-XX:G1MaxNewSizePercent=40 -XX:G1MixedGCCountTarget=4 ");
        args.Append($"-XX:+AlwaysPreTouch -Dfml.ignoreInvalidMinecraftCertificates=true ");
        args.Append($"-Djava.library.path=\"{nativesDir}\" ");
        args.Append($"-cp \"{classpathStr}\" ");
        args.Append($"{mainClass} ");
        args.Append($"--username \"{username}\" ");
        args.Append($"--version {VERSION} ");
        args.Append($"--gameDir \"{gameDir}\" ");
        args.Append($"--assetsDir \"{assetsDir}\" ");
        args.Append($"--assetIndex \"{Path.GetFileNameWithoutExtension(assetsIndexPath)}\" ");
        args.Append($"--accessToken dummy ");
        args.Append($"--uuid {Guid.NewGuid():N} ");
        args.Append($"--userType legacy ");
        args.Append($"--versionType zagaDLC");

        SetStatus("Launching Minecraft...", 100);

        var logFile = Path.Combine(_mcDir, $"game_{DateTime.Now:yyyyMMdd_HHmmss}.log");
        using var logWriter = new StreamWriter(logFile, append: false) { AutoFlush = true };

        var proc = Process.Start(new ProcessStartInfo
        {
            FileName = javaExe,
            Arguments = args.ToString(),
            UseShellExecute = false,
            CreateNoWindow = true,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            WorkingDirectory = _baseDir,
        }) ?? throw new Exception("Failed to start Minecraft process");

        proc.OutputDataReceived += (s, e) => { if (e.Data != null) logWriter.WriteLine(e.Data); };
        proc.ErrorDataReceived += (s, e) => { if (e.Data != null) logWriter.WriteLine("[ERR] " + e.Data); };
        proc.BeginOutputReadLine();
        proc.BeginErrorReadLine();

        onStarted?.Invoke();

        await proc.WaitForExitAsync();
        proc.Dispose();
        logWriter.Dispose();
    }

    private string? ConvertMavenPath(string name, string librariesDir)
    {
        var parts = name.Split(':');
        if (parts.Length < 3) return null;
        var group = parts[0].Replace('.', '/');
        var artifact = parts[1];
        var version = parts[2];
        var classifier = parts.Length > 3 ? $"-{parts[3]}" : "";
        var path = Path.Combine(librariesDir, $"{group}/{artifact}/{version}/{artifact}-{version}{classifier}.jar");
        return path;
    }

    private static string GetLibraryKey(string name)
    {
        var parts = name.Split(':');
        if (parts.Length < 3) return name;
        if (parts.Length >= 4)
            return $"{parts[0]}:{parts[1]}:{parts[3]}";
        return $"{parts[0]}:{parts[1]}";
    }

    // ==================== DOWNLOAD ====================

    private async Task Download(string url, string outputFile, int startPct = 0, int endPct = 100, bool reportProgress = true)
    {
        var dir = Path.GetDirectoryName(outputFile);
        if (dir != null) Directory.CreateDirectory(dir);

        var tempFile = outputFile + ".tmp";
        for (int attempt = 0; attempt < 3; attempt++)
        {
            try
            {
                using var response = await _http.GetAsync(url, HttpCompletionOption.ResponseHeadersRead);
                response.EnsureSuccessStatusCode();
                var totalBytes = response.Content.Headers.ContentLength ?? 0;

                using var stream = await response.Content.ReadAsStreamAsync();
                using (var fileStream = File.Create(tempFile))
                {
                    var buffer = new byte[81920];
                    long downloaded = 0;
                    int bytesRead;
                    var lastProgress = DateTime.MinValue;

                    while ((bytesRead = await stream.ReadAsync(buffer)) > 0)
                    {
                        await fileStream.WriteAsync(buffer.AsMemory(0, bytesRead));
                        downloaded += bytesRead;

                        if (reportProgress && totalBytes > 0 && (DateTime.UtcNow - lastProgress).TotalMilliseconds > 500)
                        {
                            var pct = startPct + (int)((double)downloaded / totalBytes * (endPct - startPct));
                            SetStatus($"Downloading {(downloaded / 1024.0 / 1024.0):F1} MB", pct);
                            lastProgress = DateTime.UtcNow;
                        }
                    }
                }

                if (File.Exists(outputFile)) File.Delete(outputFile);
                File.Move(tempFile, outputFile);
                return;
            }
            catch when (attempt < 2)
            {
                try { File.Delete(tempFile); } catch { }
                await Task.Delay(1000 * (attempt + 1));
            }
        }
    }

    private async Task<string> DownloadString(string url)
    {
        using var response = await _http.GetAsync(url);
        response.EnsureSuccessStatusCode();
        return await response.Content.ReadAsStringAsync();
    }

    private async Task ExtractZip(string zipFile, string outputDir, int startPct, int endPct)
    {
        await Task.Run(() =>
        {
            using var archive = System.IO.Compression.ZipFile.OpenRead(zipFile);
            int total = archive.Entries.Count;
            int done = 0;
            foreach (var entry in archive.Entries)
            {
                if (string.IsNullOrEmpty(entry.Name))
                {
                    Directory.CreateDirectory(Path.Combine(outputDir, entry.FullName));
                    continue;
                }
                var destPath = Path.Combine(outputDir, entry.FullName);
                var fullDest = Path.GetFullPath(destPath);
                var fullOutput = Path.GetFullPath(outputDir);
                if (!fullDest.StartsWith(fullOutput, StringComparison.OrdinalIgnoreCase))
                {
                    throw new InvalidOperationException("Zip entry attempts path traversal: " + entry.FullName);
                }
                var dir = Path.GetDirectoryName(destPath);
                if (dir != null) Directory.CreateDirectory(dir);
                entry.ExtractToFile(destPath, true);
                done++;
                if (done % 10 == 0 || done == total)
                {
                    var pct = startPct + (int)((double)done / total * (endPct - startPct));
                    SetStatus($"Extracting: {done}/{total}", pct);
                }
            }
        });
    }

    // ==================== AUTO UPDATE ====================

    private async Task CheckAndUpdateMod(string jwtToken)
    {
        try
        {
            var modFile = Path.Combine(_baseDir, "mods", "rockstar-1.0.0.jar");
            var encFile = Path.Combine(_mcDir, "rockstar-1.0.0.jar.encrypted");
            var versionFile = Path.Combine(_mcDir, "mod_version.json");

            const string githubApiUrl = "https://api.github.com/repos/zagir4ik985/zagaDLC-releases/releases";

            var resp = await _http.GetAsync(githubApiUrl);
            if (!resp.IsSuccessStatusCode) return;

            var body = await resp.Content.ReadAsStringAsync();
            var releases = System.Text.Json.JsonSerializer.Deserialize<List<JsonElement>>(body);
            if (releases == null || releases.Count == 0) return;

            var latest = releases[0];
            var remoteVersion = latest.GetProperty("tag_name").GetString()?.TrimStart('v') ?? "";
            if (string.IsNullOrEmpty(remoteVersion)) return;

            string localVersion = "";
            if (File.Exists(versionFile))
            {
                try
                {
                    var localData = ParseJson(await File.ReadAllTextAsync(versionFile), "local version");
                    localVersion = localData["version"]?.GetValue<string>() ?? "";
                }
                catch { }
            }

            if (localVersion == remoteVersion) return;

            // Find encrypted JAR asset in release
            var assets = latest.GetProperty("assets").EnumerateArray();
            string? downloadUrl = null;
            string? expectedHash = null;
            foreach (var asset in assets)
            {
                var name = asset.GetProperty("name").GetString() ?? "";
                if (name.EndsWith(".encrypted") || name.EndsWith(".jar.encrypted"))
                {
                    downloadUrl = asset.GetProperty("browser_download_url").GetString();
                    break;
                }
            }

            // Check for hash file in release body
            if (latest.TryGetProperty("body", out var releaseBody))
            {
                var bodyText = releaseBody.GetString() ?? "";
                var hashMatch = System.Text.RegularExpressions.Regex.Match(bodyText, @"SHA256:\s*([a-fA-F0-9]{64})");
                if (hashMatch.Success) expectedHash = hashMatch.Groups[1].Value.ToLowerInvariant();
            }

            if (string.IsNullOrEmpty(downloadUrl)) return;

            // Download new encrypted mod
            SetStatus("Updating mod...", 50);
            await Download(downloadUrl, encFile, 50, 80);

            // Verify hash if provided
            if (!string.IsNullOrEmpty(expectedHash) && File.Exists(encFile))
            {
                using var sha = System.Security.Cryptography.SHA256.Create();
                using var fs = File.OpenRead(encFile);
                var hash = BitConverter.ToString(sha.ComputeHash(fs)).Replace("-", "").ToLowerInvariant();
                if (hash != expectedHash)
                {
                    File.Delete(encFile);
                    throw new Exception("Mod update hash mismatch — possible tampering");
                }
            }

            // Delete decrypted mod to force re-decryption
            if (File.Exists(modFile)) File.Delete(modFile);

            // Save version
            Directory.CreateDirectory(_mcDir);
            var versionData = new { version = remoteVersion };
            await File.WriteAllTextAsync(versionFile, System.Text.Json.JsonSerializer.Serialize(versionData));

            SetStatus("Mod updated", 80);
        }
        catch
        {
            // Silent fail — use embedded resource as fallback
        }
    }
}
