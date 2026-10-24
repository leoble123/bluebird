# Installs the Android command-line tools, accepts the SDK licenses, and installs the
# SDK packages Bluebird builds against. Safe to re-run.
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
[Environment]::SetEnvironmentVariable('ANDROID_HOME', $sdk, 'User')

$latest = Join-Path $sdk 'cmdline-tools\latest'
if (-not (Test-Path "$latest\bin\sdkmanager.bat")) {
    $dl = Join-Path $env:TEMP 'bluebird-sdk-download'
    New-Item -ItemType Directory -Force $dl | Out-Null
    $zip = Join-Path $dl 'cmdline-tools.zip'
    Write-Host 'Downloading Android command-line tools...'
    Invoke-WebRequest 'https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip' -OutFile $zip
    Expand-Archive $zip -DestinationPath "$dl\x" -Force
    New-Item -ItemType Directory -Force (Join-Path $sdk 'cmdline-tools') | Out-Null
    Move-Item "$dl\x\cmdline-tools" $latest
}

$sm = "$latest\bin\sdkmanager.bat"
$ErrorActionPreference = 'Continue'
$answers = (1..40 | ForEach-Object { 'y' }) -join "`r`n"
$answers | & $sm --licenses
& $sm --install 'platform-tools' 'platforms;android-36' 'platforms;android-37.0' 'build-tools;36.1.0'
Get-ChildItem $sdk | Select-Object -ExpandProperty Name
Write-Host 'SDK setup finished.'
