[CmdletBinding()]
param(
    [switch]$Clean,
    [switch]$Launch = $true,
    [string]$JavaHome = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptRoot = Split-Path -Parent $PSCommandPath
Push-Location $scriptRoot
try {
    $javaCandidates = @(
        $JavaHome,
        'C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot',
        'C:\Program Files\Android\Android Studio\jbr',
        $env:JAVA_HOME
    ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique

    $resolvedJavaHome = $null
    foreach ($candidate in $javaCandidates) {
        if (Test-Path $candidate) {
            $resolvedJavaHome = $candidate
            break
        }
    }

    if ($resolvedJavaHome) {
        $env:JAVA_HOME = $resolvedJavaHome
        $env:PATH = "$resolvedJavaHome\bin;$env:PATH"
        $env:GRADLE_OPTS = "-Dorg.gradle.java.home=`"$resolvedJavaHome`""
        Write-Host "Using JAVA_HOME: $resolvedJavaHome" -ForegroundColor Yellow
    } else {
        Write-Warning 'No configured JAVA_HOME candidate was found. Falling back to existing environment.'
    }

    Write-Host '== FluffyGram Debug Build & Deploy ==' -ForegroundColor Cyan
    java -version

    $gradleTasks = @()
    if ($Clean) {
        $gradleTasks += 'clean'
    }
    $gradleTasks += ':TMessagesProj_App:assembleAfatDebug'
    $gradleTasks += ':TMessagesProj_App:installAfatDebug'

    Write-Host "Running Gradle tasks: $($gradleTasks -join ', ')" -ForegroundColor Cyan
    & .\gradlew @gradleTasks
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE"
    }

    Write-Host 'Debug build installed successfully.' -ForegroundColor Green

    if ($Launch) {
        Write-Host 'Launching app...' -ForegroundColor Cyan
        adb shell am start -n org.ushastoe.fluffy.beta/org.telegram.ui.LaunchActivity | Out-Null
        Write-Host 'App launched on device.' -ForegroundColor Green
    } else {
        Write-Host 'Launch skipped.' -ForegroundColor Yellow
    }
}
finally {
    Pop-Location
}
