#!/usr/bin/env pwsh

[CmdletBinding()]
param(
    [switch]$Clean,
    [switch]$Install = $true,
    [switch]$Launch = $true,
    [string]$JavaHome = '',
    [string]$AndroidSdkRoot = ''
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

$scriptRoot = Split-Path -Parent $PSCommandPath
Push-Location $scriptRoot
try {
    $isWindowsPlatform = $PSVersionTable.Platform -eq 'Win32NT' -or $env:OS -eq 'Windows_NT'
    $gradleWrapper = if ($isWindowsPlatform) { '.\gradlew.bat' } else { './gradlew' }

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
    } else {
        Write-Warning 'No configured JAVA_HOME candidate was found. Falling back to existing environment.'
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
    } else {
        Write-Warning 'Android SDK root was not found in common locations. Gradle may rely on local.properties.'
    }

    Write-Host '== FluffyGram Debug Build & Deploy ==' -ForegroundColor Cyan
    & java -version

    $gradleTasks = @()
    if ($Clean) {
        $gradleTasks += 'clean'
    }
    $gradleTasks += ':TMessagesProj_App:assembleAfatDebug'
    if ($Install) {
        $gradleTasks += ':TMessagesProj_App:installAfatDebug'
    }

    Write-Host "Running Gradle tasks: $($gradleTasks -join ', ')" -ForegroundColor Cyan
    & $gradleWrapper @gradleTasks
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE"
    }

    Write-Host 'Debug build installed successfully.' -ForegroundColor Green

    if (-not $Install) {
        Write-Host 'Install skipped.' -ForegroundColor Yellow
    } elseif ($Launch) {
        $adbCommand = Get-Command adb -ErrorAction SilentlyContinue
        if (-not $adbCommand) {
            throw 'adb was not found in PATH. Set ANDROID_SDK_ROOT/ANDROID_HOME or install platform-tools.'
        }

        Write-Host 'Launching app...' -ForegroundColor Cyan
        & $adbCommand.Source shell am start -n 'org.ushastoe.fluffy.beta/org.telegram.ui.LaunchActivity' | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "adb launch failed with exit code $LASTEXITCODE"
        }
        Write-Host 'App launched on device.' -ForegroundColor Green
    } else {
        Write-Host 'Launch skipped.' -ForegroundColor Yellow
    }
}
finally {
    Pop-Location
}
