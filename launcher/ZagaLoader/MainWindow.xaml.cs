using System;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Reflection;
using System.Text.Json;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Input;

namespace ZagaLoader
{
    public partial class MainWindow : Window
    {
        private static readonly string MC_VERSION = "1.21.11";
        private static readonly string FABRIC_LOADER = "0.18.4";
        private static readonly string FABRIC_API_VERSION = "0.141.2+1.21.11";
        private static readonly string GITHUB_REPO = "zagir4ik985/Rich-Modern";
        private static readonly string FABRIC_INSTALLER_URL = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.1/fabric-installer-1.0.1.jar";
        private static readonly string FABRIC_API_URL = "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.141.2+1.21.11/fabric-api-0.141.2+1.21.11.jar";

        private string mcDir;
        private string modsDir;
        private string versionsDir;

        public MainWindow()
        {
            InitializeComponent();
            mcDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft");
            modsDir = Path.Combine(mcDir, "mods");
            versionsDir = Path.Combine(mcDir, "versions");
            MCPathText.Text = mcDir;
        }

        private void Log(string msg)
        {
            Dispatcher.Invoke(() =>
            {
                LogBox.Items.Add(msg);
                LogBox.ScrollIntoView(LogBox.Items[LogBox.Items.Count - 1]);
            });
        }

        private void SetProgress(double pct)
        {
            Dispatcher.Invoke(() =>
            {
                ProgressBar.Value = pct;
                StatusText.Text = $"{pct:F0}%";
            });
        }

        private void TitleBar_MouseLeftButtonDown(object sender, MouseButtonEventArgs e) { DragMove(); }
        private void Minimize_Click(object sender, RoutedEventArgs e) { WindowState = WindowState.Minimized; }
        private void Close_Click(object sender, RoutedEventArgs e) { Close(); }

        private async void LaunchButton_Click(object sender, RoutedEventArgs e)
        {
            LaunchButton.IsEnabled = false;
            try
            {
                await Task.Run(() => Launch());
            }
            catch (Exception ex)
            {
                Log($"ОШИБКА: {ex.Message}");
                MessageBox.Show(ex.Message, "Ошибка", MessageBoxButton.OK, MessageBoxImage.Error);
            }
            LaunchButton.IsEnabled = true;
        }

        private void Launch()
        {
            Log("=== zagaDLC Launcher v3.1 ===");
            Log($"Minecraft {MC_VERSION} | Fabric Loader {FABRIC_LOADER}");

            Directory.CreateDirectory(mcDir);
            Directory.CreateDirectory(modsDir);
            Directory.CreateDirectory(versionsDir);

            string fabricVersionDir = Path.Combine(versionsDir, $"fabric-loader-{FABRIC_LOADER}-{MC_VERSION}");
            string fabricJson = Path.Combine(fabricVersionDir, $"fabric-loader-{FABRIC_LOADER}-{MC_VERSION}.json");

            if (!File.Exists(fabricJson))
            {
                Log("Fabric не установлен. Устанавливаю...");
                InstallFabric();
            }
            else
            {
                Log("Fabric уже установлен.");
            }

            DownloadFabricApi();
            DownloadModJar();
            LaunchMinecraft();
        }

        private void InstallFabric()
        {
            SetProgress(5);
            string installerJar = Path.Combine(Path.GetTempPath(), "fabric-installer.jar");

            using (var wc = new WebClient())
            {
                Log("Скачиваю Fabric Installer...");
                wc.DownloadFile(FABRIC_INSTALLER_URL, installerJar);
            }

            Log("Запускаю установку Fabric...");
            var psi = new ProcessStartInfo
            {
                FileName = "java",
                Arguments = $"-jar \"{installerJar}\" client -dir \"{mcDir}\" -mcversion {MC_VERSION} -loader {FABRIC_LOADER} -noprofile",
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true
            };

            var proc = Process.Start(psi);
            string output = proc.StandardOutput.ReadToEnd();
            string errors = proc.StandardError.ReadToEnd();
            proc.WaitForExit();

            if (proc.ExitCode != 0)
            {
                Log($"Fabric Installer output: {output}");
                Log($"Fabric Installer errors: {errors}");
                throw new Exception($"Fabric установка провалилась (код {proc.ExitCode})");
            }

            Log("Fabric установлен успешно.");
            SetProgress(20);

            try { File.Delete(installerJar); } catch { }
        }

        private void DownloadFabricApi()
        {
            string fabricApiJar = Path.Combine(modsDir, $"fabric-api-{FABRIC_API_VERSION}.jar");
            if (File.Exists(fabricApiJar))
            {
                Log("Fabric API уже скачан.");
                return;
            }

            Log("Скачиваю Fabric API...");
            SetProgress(30);

            using (var wc = new WebClient())
            {
                wc.DownloadFile(FABRIC_API_URL, fabricApiJar);
            }

            Log("Fabric API скачан.");
            SetProgress(50);
        }

        private void DownloadModJar()
        {
            string modJar = Path.Combine(modsDir, "zagaDLC.jar");
            if (File.Exists(modJar))
            {
                File.Delete(modJar);
            }

            Log("Скачиваю zagaDLC мод...");
            SetProgress(55);

            string downloadUrl = $"https://github.com/{GITHUB_REPO}/releases/latest/download/zagaDLC.jar";

            try
            {
                using (var wc = new WebClient())
                {
                    wc.DownloadFile(downloadUrl, modJar);
                }
                Log("zagaDLC мод скачан.");
            }
            catch (Exception ex)
            {
                Log($"Ошибка скачивания мода: {ex.Message}");
                Log("Попробуйте скачать мод вручную и положить в mods/");
            }

            SetProgress(75);
        }

        private void LaunchMinecraft()
        {
            Log("Подготавливаю запуск...");
            SetProgress(85);

            string fabricVersionDir = Path.Combine(versionsDir, $"fabric-loader-{FABRIC_LOADER}-{MC_VERSION}");
            string fabricJson = Path.Combine(fabricVersionDir, $"fabric-loader-{FABRIC_LOADER}-{MC_VERSION}.json");

            if (!File.Exists(fabricJson))
            {
                throw new Exception($"Fabric JSON не найден: {fabricJson}");
            }

            string jsonContent = File.ReadAllText(fabricJson);
            using var doc = JsonDocument.Parse(jsonContent);
            var root = doc.RootElement;

            string mainClass = root.TryGetProperty("mainClass", out var mc) ? mc.GetString() : "net.fabricmc.loader.impl.launch.knot.KnotClient";

            string librariesDir = Path.Combine(mcDir, "libraries");
            string gameJar = Path.Combine(versionsDir, MC_VERSION, $"{MC_VERSION}.jar");

            if (!File.Exists(gameJar))
            {
                throw new Exception($"Minecraft JAR не найден: {gameJar}\nЗапустите Minecraft {MC_VERSION} один раз через официальный лаунчер.");
            }

            string classpath = "";
            if (root.TryGetProperty("libraries", out var libraries))
            {
                var entries = libraries.EnumerateArray().ToList();
                foreach (var lib in entries)
                {
                    if (lib.TryGetProperty("url", out var urlEl))
                    {
                        string url = urlEl.GetString();
                        if (lib.TryGetProperty("name", out var nameEl))
                        {
                            string name = nameEl.GetString();
                            string path = name.Replace('.', '/').Replace(':', '/');
                            string[] parts = path.Split('/');
                            string fileName = string.Join("/", parts.Skip(Math.Max(0, parts.Length - 3)));
                            string libPath = Path.Combine(librariesDir, fileName.Replace('/', '\\'));

                            if (!File.Exists(libPath))
                            {
                                string dir = Path.GetDirectoryName(libPath);
                                Directory.CreateDirectory(dir);
                                string downloadUrl = url.TrimEnd('/') + "/" + fileName;
                                Log($"  Библиотека: {parts[parts.Length - 3]}:{parts[parts.Length - 2]}:{parts[parts.Length - 1]}");
                                try
                                {
                                    using var wc = new WebClient();
                                    wc.DownloadFile(downloadUrl, libPath);
                                }
                                catch (Exception ex)
                                {
                                    Log($"  Предупреждение: не удалось скачать {ex.Message}");
                                }
                            }
                        }
                    }
                }
            }

            var cpEntries = Directory.GetFiles(librariesDir, "*.jar", SearchOption.AllDirectories);
            classpath = string.Join(";", cpEntries.Append(gameJar));

            string nativesDir = Path.Combine(mcDir, "natives");
            Directory.CreateDirectory(nativesDir);

            string javaPath = "java";
            string javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
            if (!string.IsNullOrEmpty(javaHome))
            {
                string javaExe = Path.Combine(javaHome, "bin", "java.exe");
                if (File.Exists(javaExe)) javaPath = javaExe;
            }

            string appData = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
            string username = Environment.UserName;
            string uuid = Guid.NewGuid().ToString("N");

            string launchArgs = $"-Xmx2G -Xms512M " +
                $"-Djava.library.path=\"{nativesDir}\" " +
                $"-cp \"{classpath}\" " +
                $"{mainClass} " +
                $"--username zagaDLC " +
                $"--version {MC_VERSION} " +
                $"--gameDir \"{mcDir}\" " +
                $"--assetsDir \"{Path.Combine(mcDir, "assets")}\" " +
                $"--assetIndex {MC_VERSION} " +
                $"--uuid {uuid} " +
                $"--accessToken 0 " +
                $"--userType msa " +
                $"--versionType fabric";

            Log("Запускаю Minecraft...");
            SetProgress(100);
            StatusText.Text = "Запуск!";

            Process.Start(new ProcessStartInfo
            {
                FileName = javaPath,
                Arguments = launchArgs,
                WorkingDirectory = mcDir,
                UseShellExecute = false
            });

            Log("Minecraft запущен! Можете закрыть лаунчер.");

            Dispatcher.Invoke(() =>
            {
                LaunchButton.Content = "ЗАПУЩЕНО ✓";
                LaunchButton.Foreground = System.Windows.Media.Brushes.LightGreen;
            });
        }
    }
}
