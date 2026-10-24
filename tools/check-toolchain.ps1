# Reports which parts of the Bluebird build toolchain are present.
$jh = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
$sdk = [Environment]::GetEnvironmentVariable('ANDROID_HOME', 'User')
"JAVA_HOME    = $jh  (exists: $(if ($jh) { Test-Path $jh } else { $false }))"
"ANDROID_HOME = $sdk  (exists: $(if ($sdk) { Test-Path $sdk } else { $false }))"
"Java dir     : $(Test-Path "$env:LOCALAPPDATA\Programs\Java")"
"SDK dir      : $(Test-Path "$env:LOCALAPPDATA\Android\Sdk")"
"git          : $((Get-Command git -ErrorAction SilentlyContinue).Source)"
"git (std)    : $(Test-Path "$env:LOCALAPPDATA\Programs\Git\cmd\git.exe") / $(Test-Path 'C:\Program Files\Git\cmd\git.exe')"
