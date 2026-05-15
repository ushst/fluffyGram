#!/usr/bin/env pwsh

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Serial,
    [switch]$Clean,
    [switch]$SkipAssemble,
    [switch]$Launch,
    [string]$JavaHome = '',
    [string]$AndroidSdkRoot = '',
    [string]$RemoteApkPath = '/data/local/tmp/fluffygram-debug.apk'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Add-ToPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PathEntry
    )

    if ([string]::IsNullOrWhiteSpace($PathEntry) -or -not (Test-Path $PathEntry)) {
        return
    }

    $currentEntries = $env:PATH -split [IO.Path]::PathSeparator
    if ($currentEntries -notcontains $PathEntry) {
        $env:PATH = $PathEntry + [IO.Path]::PathSeparator + $env:PATH
    }
}

function Resolve-FirstExistingPath {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Candidates
    )

    foreach ($candidate in $Candidates | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    return $null
}

function Resolve-GradleUserHome {
    $currentGradleUserHome = [Environment]::GetEnvironmentVariable('GRADLE_USER_HOME')
    $defaultGradleUserHome = Join-Path $HOME '.gradle'

    if ([string]::IsNullOrWhiteSpace($currentGradleUserHome)) {
        return $defaultGradleUserHome
    }

    $normalized = $currentGradleUserHome.Replace('/', '\')
    if ($normalized -match '\\scoop\\apps\\gradle\\current\\\.gradle$') {
        Write-Warning "Overriding GRADLE_USER_HOME from scoop-managed cache: $currentGradleUserHome"
        return $defaultGradleUserHome
    }

    return $currentGradleUserHome
}

function Format-Size {
    param(
        [Parameter(Mandatory = $true)]
        [long]$Bytes
    )

    if ($Bytes -ge 1GB) {
        return '{0:N2} GB' -f ($Bytes / 1GB)
    }
    if ($Bytes -ge 1MB) {
        return '{0:N2} MB' -f ($Bytes / 1MB)
    }
    if ($Bytes -ge 1KB) {
        return '{0:N2} KB' -f ($Bytes / 1KB)
    }
    return "$Bytes B"
}

function Invoke-CheckedCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$DisplayName,
        [Parameter(Mandatory = $true)]
        [scriptblock]$Action
    )

    Write-Host "== $DisplayName ==" -ForegroundColor Cyan
    & $Action
    if ($LASTEXITCODE -ne 0) {
        throw "$DisplayName failed with exit code $LASTEXITCODE"
    }
}

function Invoke-AdbCapture {
    param(
        [Parameter(Mandatory = $true)]
        [string]$AdbPath,
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $output = & $AdbPath @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    foreach ($line in $output) {
        Write-Host $line
    }
    if ($exitCode -ne 0) {
        throw "adb $($Arguments -join ' ') failed with exit code $exitCode"
    }
    return $output
}

$scriptRoot = Split-Path -Parent $PSCommandPath
Push-Location $scriptRoot
try {
    $psPlatform = $null
    if ($PSVersionTable.PSVersion.Major -ge 6 -and $PSVersionTable.ContainsKey('Platform')) {
        $psPlatform = $PSVersionTable.Platform
    }
    $isWindowsPlatform = $psPlatform -eq 'Win32NT' -or $env:OS -eq 'Windows_NT'
    $gradleWrapper = if ($isWindowsPlatform) { '.\gradlew.bat' } else { './gradlew' }

    $resolvedGradleUserHome = Resolve-GradleUserHome
    if (-not [string]::IsNullOrWhiteSpace($resolvedGradleUserHome)) {
        $env:GRADLE_USER_HOME = $resolvedGradleUserHome
        if (-not (Test-Path $resolvedGradleUserHome)) {
            New-Item -ItemType Directory -Force -Path $resolvedGradleUserHome | Out-Null
        }
        Write-Host "Using GRADLE_USER_HOME: $resolvedGradleUserHome" -ForegroundColor Yellow
    }

    $javaCandidates = @(
        $JavaHome,
        $env:JAVA_HOME,
        '/usr/lib/jvm/java-21-openjdk-amd64',
        '/usr/lib/jvm/java-21-openjdk',
        '/usr/lib/jvm/default-java',
        '/usr/lib/jvm/default',
        '/opt/android-studio/jbr',
        "$HOME/.jdks/temurin-21",
        'C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot',
        'C:\Program Files\Android\Android Studio\jbr'
    ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique

    $resolvedJavaHome = Resolve-FirstExistingPath -Candidates $javaCandidates
    if ($resolvedJavaHome) {
        $env:JAVA_HOME = $resolvedJavaHome
        Add-ToPath (Join-Path $resolvedJavaHome 'bin')
        Write-Host "Using JAVA_HOME: $resolvedJavaHome" -ForegroundColor Yellow
    }

    $sdkCandidates = @(
        $AndroidSdkRoot,
        $env:ANDROID_SDK_ROOT,
        $env:ANDROID_HOME,
        '/home/krol/Android/Sdk',
        (Join-Path $HOME 'Android/Sdk'),
        '/opt/android-sdk',
        '/usr/lib/android-sdk',
        'C:\Users\krol\AppData\Local\Android\Sdk'
    ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique

    $resolvedSdkRoot = Resolve-FirstExistingPath -Candidates $sdkCandidates
    if ($resolvedSdkRoot) {
        if (-not $env:ANDROID_SDK_ROOT) {
            $env:ANDROID_SDK_ROOT = $resolvedSdkRoot
        }
        if (-not $env:ANDROID_HOME) {
            $env:ANDROID_HOME = $resolvedSdkRoot
        }
        Add-ToPath (Join-Path $resolvedSdkRoot 'platform-tools')
        Write-Host "Using Android SDK: $resolvedSdkRoot" -ForegroundColor Yellow
    }

    $adbCommand = Get-Command adb -ErrorAction SilentlyContinue
    if (-not $adbCommand) {
        throw 'adb was not found in PATH. Set ANDROID_SDK_ROOT/ANDROID_HOME or install platform-tools.'
    }

    Write-Host '== FluffyGram Remote Debug Install ==' -ForegroundColor Cyan
    Write-Host "Target device: $Serial" -ForegroundColor Yellow

    if ($Serial.Contains(':')) {
        Invoke-CheckedCommand -DisplayName "adb connect $Serial" -Action {
            & $adbCommand.Source connect $Serial
        }
    } else {
        Invoke-CheckedCommand -DisplayName "adb get-state ($Serial)" -Action {
            & $adbCommand.Source -s $Serial get-state
        }
    }

    if (-not $SkipAssemble) {
        $gradleTasks = @()
        if ($Clean) {
            $gradleTasks += 'clean'
        }
        $gradleTasks += ':TMessagesProj_App:assembleAfatDebug'
        Invoke-CheckedCommand -DisplayName ($gradleTasks -join ' ') -Action {
            & $gradleWrapper '--console=plain' @gradleTasks
        }
    } else {
        Write-Host '== Skipping Gradle assemble ==' -ForegroundColor Cyan
    }

    $apkPath = Join-Path $scriptRoot 'TMessagesProj_App\build\outputs\apk\afat\debug\app.apk'
    if (-not (Test-Path $apkPath)) {
        throw "APK not found at $apkPath"
    }

    $apkItem = Get-Item -LiteralPath $apkPath
    Write-Host ("APK: {0} ({1})" -f $apkItem.FullName, (Format-Size -Bytes $apkItem.Length)) -ForegroundColor Yellow

    Invoke-CheckedCommand -DisplayName 'Upload APK to device' -Action {
        & $adbCommand.Source -s $Serial push $apkItem.FullName $RemoteApkPath
    }

    $sessionId = $null
    $sessionCommitted = $false
    try {
        Write-Host '== Create install session ==' -ForegroundColor Cyan
        $createOutput = Invoke-AdbCapture -AdbPath $adbCommand.Source -Arguments @('-s', $Serial, 'shell', 'cmd', 'package', 'install-create', '-r')
        $sessionMatch = [regex]::Match(($createOutput -join "`n"), '\[(?<id>\d+)\]')
        if (-not $sessionMatch.Success) {
            throw 'Could not parse package install session id.'
        }
        $sessionId = $sessionMatch.Groups['id'].Value
        Write-Host "Install session: $sessionId" -ForegroundColor Yellow

        Invoke-CheckedCommand -DisplayName 'Write APK into install session' -Action {
            & $adbCommand.Source -s $Serial shell cmd package install-write -S $apkItem.Length $sessionId base $RemoteApkPath
        }

        Invoke-CheckedCommand -DisplayName 'Commit install session' -Action {
            & $adbCommand.Source -s $Serial shell cmd package install-commit $sessionId
        }
        $sessionCommitted = $true
    }
    finally {
        if ($sessionId -and -not $sessionCommitted) {
            & $adbCommand.Source -s $Serial shell cmd package install-abandon $sessionId *> $null
        }
    }

    Invoke-CheckedCommand -DisplayName 'Cleanup remote APK' -Action {
        & $adbCommand.Source -s $Serial shell rm -f $RemoteApkPath
    }

    if ($Launch) {
        Invoke-CheckedCommand -DisplayName 'Launch app' -Action {
            & $adbCommand.Source -s $Serial shell am start -n 'org.ushastoe.fluffy.beta/org.telegram.ui.LaunchActivity'
        }
    }

    Write-Host 'Remote debug install completed successfully.' -ForegroundColor Green
}
finally {
    Pop-Location
}
