@echo off
echo === Building zagaDLC Launcher ===
echo.

set PATH=%PATH%;%USERPROFILE%\.dotnet

echo [1/3] Building ZagaUpdater...
dotnet publish ZagaUpdater\ZagaUpdater.csproj -c Release -r win-x64 --self-contained -p:PublishSingleFile=true -o bin\publish
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to build ZagaUpdater
    exit /b 1
)
echo OK: ZagaUpdater built
echo.

echo [2/3] Building ZagaLoader...
dotnet publish ZagaLoader\ZagaLoader.csproj -c Release -r win-x64 --self-contained -p:PublishSingleFile=true -o bin\publish
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to build ZagaLoader
    exit /b 1
)
echo OK: ZagaLoader built
echo.

echo [3/3] Done!
echo.
echo Output:
dir bin\publish\*.exe
echo.
echo ZagaUpdater.exe = bootstrap updater (~5MB)
echo zagaDLC.exe     = main launcher (~200MB with .NET runtime)
echo.
echo To distribute:
echo   1. Upload zagaDLC.exe to R2 bucket as loader-{version}.exe
echo   2. Update version.json in R2 with new version + SHA256
echo   3. Users download ZagaUpdater.exe once, it auto-updates zagaDLC.exe
