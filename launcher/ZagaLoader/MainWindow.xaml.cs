using System;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Json;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json.Nodes;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Input;
using Microsoft.Web.WebView2.Core;
using Newtonsoft.Json.Linq;

namespace ZagaLoader;

public partial class MainWindow : Window
{
    private const string ApiBase = "https://zagadlc.zagir4ik985.workers.dev";
    private static readonly byte[] DpapiEntropy = new byte[] { 0x5A, 0x61, 0x67, 0x61, 0x44, 0x4C, 0x43, 0x76, 0x32, 0x2E, 0x30 };
    private static readonly HttpClient Http;
    private string? _jwtToken;
    private string? _currentLogin;
    private DateTime? _expiresAt;

    private static readonly string ZagaDir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".zaga");
    private static readonly string ConfigFile = Path.Combine(ZagaDir, "config.json");
    private static readonly string HwidFile = Path.Combine(ZagaDir, ".hwid");

    private bool _isDragging;
    private System.Windows.Point _dragStart;
    private bool _isLaunching;

    static MainWindow()
    {
        Security.AntiTamper.Initialize();
        Security.AntiDebug.Initialize();

        Http = new HttpClient();
    }

    public MainWindow()
    {
        InitializeComponent();
        MouseLeftButtonDown += (s, e) =>
        {
            _isDragging = true;
            _dragStart = e.GetPosition(this);
            CaptureMouse();
        };
        MouseLeftButtonUp += (s, e) =>
        {
            _isDragging = false;
            ReleaseMouseCapture();
        };
        MouseMove += (s, e) =>
        {
            if (_isDragging)
            {
                var pos = e.GetPosition(this);
                Left += pos.X - _dragStart.X;
                Top += pos.Y - _dragStart.Y;
            }
        };
    }

    private async void Window_Loaded(object sender, RoutedEventArgs e)
    {
        try
        {
            await WebView.EnsureCoreWebView2Async(null);
        }
        catch (Exception ex)
        {
            MessageBox.Show("WebView2 Runtime не установлен!\n\nСкачай: https://go.microsoft.com/fwlink/p/?LinkId=2124703\n\n" + ex.Message, "zagaDLC", MessageBoxButton.OK, MessageBoxImage.Error);
            Application.Current.Shutdown();
            return;
        }

        WebView.DefaultBackgroundColor = System.Drawing.Color.FromArgb(255, 10, 10, 18);
        WebView.CoreWebView2.Settings.AreDefaultContextMenusEnabled = false;
        WebView.CoreWebView2.Settings.AreDevToolsEnabled = false;
        WebView.CoreWebView2.Settings.IsStatusBarEnabled = false;
        WebView.CoreWebView2.Settings.AreBrowserAcceleratorKeysEnabled = false;
        WebView.CoreWebView2.Settings.IsWebMessageEnabled = true;
        WebView.CoreWebView2.Settings.IsScriptEnabled = true;
        WebView.CoreWebView2.Settings.AreDefaultScriptDialogsEnabled = false;

        WebView.CoreWebView2.AddWebResourceRequestedFilter("*", Microsoft.Web.WebView2.Core.CoreWebView2WebResourceContext.Document);
        WebView.CoreWebView2.WebResourceRequested += (s, e) =>
        {
            e.Response!.Headers.AppendHeader("X-Content-Type-Options", "nosniff");
            e.Response!.Headers.AppendHeader("X-Frame-Options", "DENY");
            e.Response!.Headers.AppendHeader("Referrer-Policy", "no-referrer");
            e.Response!.Headers.AppendHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()");
        };

        WebView.CoreWebView2.WebMessageReceived += OnWebMessageReceived;

        string htmlPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "web", "index.html");
        if (!File.Exists(htmlPath))
        {
            string? exeDir = System.Diagnostics.Process.GetCurrentProcess().MainModule?.FileName is string f ? Path.GetDirectoryName(f) : null;
            if (exeDir != null) htmlPath = Path.Combine(exeDir, "web", "index.html");
        }
        if (!File.Exists(htmlPath))
        {
            var asm = System.Reflection.Assembly.GetExecutingAssembly();
            using var stream = asm.GetManifestResourceStream("web.index.html");
            if (stream != null)
            {
                string tempDir = Path.Combine(Path.GetTempPath(), "zagaDLC");
                Directory.CreateDirectory(tempDir);
                htmlPath = Path.Combine(tempDir, "index.html");
                using var fs = File.Create(htmlPath);
                stream.CopyTo(fs);
            }
        }
        WebView.CoreWebView2.Navigate(new Uri(htmlPath).AbsoluteUri);
        WebView.CoreWebView2.NavigationCompleted += (s, e) => { };
    }

    private async void OnWebMessageReceived(object? sender, CoreWebView2WebMessageReceivedEventArgs e)
    {
        string msg = e.TryGetWebMessageAsString();

        if (msg == "close") { Application.Current.Shutdown(); return; }
        if (msg == "minimize") { WindowState = WindowState.Minimized; return; }

        if (msg.StartsWith("login|"))
        {
            var parts = msg.Split("|", 3);
            if (parts.Length == 3)
                await HandleLogin(parts[1], parts[2]);
            return;
        }

        if (msg.StartsWith("register|"))
        {
            var parts = msg.Split("|", 4);
            if (parts.Length == 4)
                await HandleRegister(parts[1], parts[2], parts[3]);
            return;
        }

        if (msg == "logout") HandleLogout();
        if (msg == "play") await HandlePlay();
    }

    private async Task HandleLogin(string login, string password)
    {
        try
        {
            var hwid = GetHwid();
            var payload = new { login, password, hwid };
            var resp = await Http.PostAsJsonAsync($"{ApiBase}/api/login", payload);
            var body = await resp.Content.ReadAsStringAsync();
            var json = JObject.Parse(body);

            if (!resp.IsSuccessStatusCode || json["error"] != null)
            {
                SendToJs($"loginError|{json["error"]?.ToString() ?? "Login failed"}");
                return;
            }

            _jwtToken = json["token"]?.ToString();
            _currentLogin = login;

            var userInfo = await FetchUserInfo();
            if (userInfo != null)
            {
                var expiresStr = userInfo["expires_at"]?.ToString() ?? "";
                var uid = userInfo["uid"]?.ToString() ?? "";
                if (!string.IsNullOrEmpty(expiresStr))
                {
                    if (DateTime.TryParse(expiresStr, System.Globalization.CultureInfo.InvariantCulture, System.Globalization.DateTimeStyles.AssumeUniversal | System.Globalization.DateTimeStyles.AdjustToUniversal, out var exp))
                        _expiresAt = exp;
                    else if (DateTime.TryParse(expiresStr, out var expLocal))
                        _expiresAt = expLocal.ToUniversalTime();
                }
                SendToJs($"loginSuccess|{_currentLogin}|{uid}|{expiresStr}");
            }
            else
            {
                SendToJs("loginError|Failed to get user info");
            }
        }
        catch
        {
            SendToJs("loginError|Connection error");
        }
    }

    private async Task HandleRegister(string login, string password, string confirm)
    {
        if (password != confirm)
        {
            SendToJs("registerError|Passwords don't match");
            return;
        }
        try
        {
            var hwid = GetHwid();
            var payload = new { login, password, hwid };
            var resp = await Http.PostAsJsonAsync($"{ApiBase}/api/register", payload);
            var body = await resp.Content.ReadAsStringAsync();
            var json = JObject.Parse(body);

            if (!resp.IsSuccessStatusCode || json["error"] != null)
            {
                SendToJs($"registerError|{json["error"]?.ToString() ?? "Registration failed"}");
                return;
            }
            SendToJs("registerSuccess");
        }
        catch
        {
            SendToJs("registerError|Connection error");
        }
    }

    private void HandleLogout()
    {
        _jwtToken = null;
        _currentLogin = null;
    }

    private async Task HandlePlay()
    {
        if (_isLaunching) return;
        if (string.IsNullOrEmpty(_jwtToken))
        {
            SendToJs("launchError|Please login first");
            return;
        }
        if (_expiresAt.HasValue && _expiresAt.Value < DateTime.UtcNow)
        {
            SendToJs("launchError|License expired! Please renew.");
            return;
        }
        var token = _jwtToken;
        var login = _currentLogin;
        _isLaunching = true;
        try
        {
            var launcher = new MinecraftLauncher(ZagaDir, Http, (msg, pct) =>
            {
                Dispatcher.BeginInvoke(() => SendToJs($"status|{msg}|{pct}"));
            });

            var javaExe = await launcher.EnsureJava();
            await launcher.InstallMinecraft(token);

            SendToJs("launching");
            await launcher.Launch(javaExe, token, ApiBase, login ?? "Player", onStarted: () =>
            {
                Dispatcher.BeginInvoke(() =>
                {
                    try
                    {
                        WebView.CoreWebView2.WebMessageReceived -= OnWebMessageReceived;
                        WebView.Dispose();
                    }
                    catch { }
                    WindowState = WindowState.Minimized;
                    GC.Collect();
                    GC.WaitForPendingFinalizers();
                });
            });
        }
        catch (Exception ex)
        {
            var errMsg = ex.ToString();
            if (ex.InnerException != null) errMsg += "\n--- Inner ---\n" + ex.InnerException.ToString();
            try { File.WriteAllText(Path.Combine(ZagaDir, "launch_error.log"), errMsg); } catch { }
            var inner = ex.InnerException != null ? $"\n{ex.InnerException.Message}" : "";
            SendToJs($"launchError|{ex.Message}{inner}");
        }
        finally
        {
            _isLaunching = false;
            try { Application.Current.Shutdown(); } catch { }
        }
    }

    private void SendToJs(string message)
    {
        try
        {
            Dispatcher.BeginInvoke(() =>
            {
                try
                {
                    var safe = message
                        .Replace("\\", "\\\\")
                        .Replace("'", "\\'")
                        .Replace("\n", "\\n")
                        .Replace("\r", "\\r")
                        .Replace("\"", "\\\"")
                        .Replace("<", "\\x3c")
                        .Replace(">", "\\x3e");
                    WebView.CoreWebView2.ExecuteScriptAsync($"window.onCsharpMessage('{safe}')");
                }
                catch { }
            });
        }
        catch { }
    }

    private async Task<JObject?> FetchUserInfo()
    {
        try
        {
            var req = new System.Net.Http.HttpRequestMessage(System.Net.Http.HttpMethod.Get, $"{ApiBase}/api/user/info");
            req.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", _jwtToken);
            var resp = await Http.SendAsync(req);
            if (!resp.IsSuccessStatusCode) return null;
            var body = await resp.Content.ReadAsStringAsync();
            return JObject.Parse(body);
        }
        catch { return null; }
    }

    public async Task TryAutoLogin()
    {
        if (!File.Exists(ConfigFile)) return;
        try
        {
            JObject? json = null;
            var raw = File.ReadAllText(ConfigFile);

            try
            {
                var protectedBytes = File.ReadAllBytes(ConfigFile);
                var plainBytes = System.Security.Cryptography.ProtectedData.Unprotect(protectedBytes, DpapiEntropy, System.Security.Cryptography.DataProtectionScope.CurrentUser);
                json = JObject.Parse(System.Text.Encoding.UTF8.GetString(plainBytes));
            }
            catch
            {
                try { File.Delete(ConfigFile); } catch { }
                return;
            }

            var token = json["jwt"]?.ToString();
            var login = json["login"]?.ToString();
            if (string.IsNullOrEmpty(token) || string.IsNullOrEmpty(login)) return;

            _jwtToken = token;
            _currentLogin = login;

            var userInfo = await FetchUserInfo();
            if (userInfo != null)
            {
                var expiresStr = userInfo["expires_at"]?.ToString() ?? "";
                var uid = userInfo["uid"]?.ToString() ?? "";
                if (!string.IsNullOrEmpty(expiresStr) && DateTime.TryParse(expiresStr, out var exp))
                {
                    _expiresAt = exp;
                    if (exp < DateTime.UtcNow)
                    {
                        _jwtToken = null;
                        _currentLogin = null;
                        return;
                    }
                }
                SendToJs($"autoLogin|{_currentLogin}|{uid}|{expiresStr}");
            }
            else
            {
                _jwtToken = null;
                _currentLogin = null;
            }
        }
        catch { }
    }

    // ==================== HWID ====================

    private static string GetHwid()
    {
        try
        {
            if (File.Exists(HwidFile))
            {
                var cached = File.ReadAllText(HwidFile).Trim();
                if (!string.IsNullOrEmpty(cached)) return cached;
            }

            var sb = new StringBuilder();
            sb.Append(GetCpuId()); sb.Append('|');
            sb.Append(GetMotherboardSerial()); sb.Append('|');
            sb.Append(GetDiskSerial()); sb.Append('|');
            sb.Append(GetMacAddress());

            using var sha = SHA256.Create();
            var hash = sha.ComputeHash(Encoding.UTF8.GetBytes(sb.ToString()));
            var hwid = BitConverter.ToString(hash).Replace("-", "").ToLowerInvariant();

            Directory.CreateDirectory(ZagaDir);
            File.WriteAllText(HwidFile, hwid);
            return hwid;
        }
        catch
        {
            var sb = new StringBuilder();
            sb.Append(Environment.MachineName); sb.Append('|');
            sb.Append(Environment.UserName); sb.Append('|');
            sb.Append(Guid.NewGuid().ToString("N"));
            using var sha = SHA256.Create();
            var hash = sha.ComputeHash(Encoding.UTF8.GetBytes(sb.ToString()));
            return BitConverter.ToString(hash).Replace("-", "").ToLowerInvariant();
        }
    }

    private static string RunWmic(string args)
    {
        try
        {
            var psi = new ProcessStartInfo("wmic", args)
            {
                RedirectStandardOutput = true, UseShellExecute = false, CreateNoWindow = true
            };
            var output = Process.Start(psi)?.StandardOutput.ReadToEnd() ?? "";
            foreach (var line in output.Split('\n'))
            {
                var t = line.Trim();
                if (!string.IsNullOrEmpty(t) && !t.StartsWith(args.Split(' ')[^1]))
                    return t;
            }
        }
        catch { }
        return "unknown";
    }

    private static string GetCpuId() => RunWmic("cpu get ProcessorId");
    private static string GetMotherboardSerial() => RunWmic("baseboard get SerialNumber");
    private static string GetDiskSerial() => RunWmic("diskdrive get SerialNumber");

    private static string GetMacAddress()
    {
        try
        {
            foreach (var ni in System.Net.NetworkInformation.NetworkInterface.GetAllNetworkInterfaces())
            {
                if (ni.OperationalStatus != System.Net.NetworkInformation.OperationalStatus.Up) continue;
                var mac = ni.GetPhysicalAddress().GetAddressBytes();
                if (mac is { Length: > 0 })
                    return string.Join(":", mac.Select(b => b.ToString("X2")));
            }
        }
        catch { }
        return "mac-unknown";
    }
}
