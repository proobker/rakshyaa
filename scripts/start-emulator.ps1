param([string]$Avd = "Medium_Phone_API_36.1")
$ErrorActionPreference = "Stop"
$sdkPath = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } elseif ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$adbPath = Join-Path $sdkPath "platform-tools\adb.exe"
$emulatorPath = Join-Path $sdkPath "emulator\emulator.exe"
if (!(Test-Path -LiteralPath $adbPath) -or !(Test-Path -LiteralPath $emulatorPath)) { throw "Install Android SDK platform-tools and emulator first." }
& $adbPath start-server
if ($LASTEXITCODE -ne 0) { throw "ADB could not start. Check which process owns TCP port 5037." }
$devices = & $adbPath devices
if ($devices -match "emulator-\d+\s+(device|offline)") { throw "An emulator is already running. Close it before launching a cold boot." }
if ($Avd -notmatch '^[A-Za-z0-9_.-]+$') { throw "Invalid AVD name." }
# Cold boot bypasses stale snapshots; software graphics avoids host GPU startup problems.
# No wipe-data option: installed apps and their data are preserved.
Start-Process -FilePath $emulatorPath -ArgumentList @("-avd", $Avd, "-no-snapshot", "-gpu", "swiftshader_indirect") -WindowStyle Normal
Write-Host "Emulator launched. Wait for the Android home screen before installing the APK."
