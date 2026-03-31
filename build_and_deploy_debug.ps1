#!/usr/bin/env pwsh

[CmdletBinding()]
param(
    [switch]$Clean,
    [switch]$Install = $true,
    [switch]$Launch = $true,
    [switch]$NoDaemon,
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

function Load-DotEnv {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path $Path)) {
        return
    }

    foreach ($rawLine in Get-Content -Path $Path) {
        $line = $rawLine.Trim()
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) {
            continue
        }

        $match = [regex]::Match($line, '^(?<key>[A-Za-z_][A-Za-z0-9_]*)\s*=\s*(?<value>.*)$')
        if (-not $match.Success) {
            continue
        }

        $key = $match.Groups['key'].Value
        $value = $match.Groups['value'].Value.Trim()
        if (
            ($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))
        ) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($key))) {
            [Environment]::SetEnvironmentVariable($key, $value)
        }
    }
}

function Format-Duration {
    param(
        [Parameter(Mandatory = $true)]
        [TimeSpan]$Duration
    )

    if ($Duration.TotalHours -ge 1) {
        return '{0:00}:{1:00}:{2:00}' -f [int]$Duration.TotalHours, $Duration.Minutes, $Duration.Seconds
    }

    return '{0:00}:{1:00}' -f $Duration.Minutes, $Duration.Seconds
}

function New-TelegramNotifier {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$GradleTasks,
        [Parameter(Mandatory = $true)]
        [string]$EnvFilePath
    )

    Load-DotEnv -Path $EnvFilePath

    $apiBase = [Environment]::GetEnvironmentVariable('FLUFFY_BUILD_TG_API_BASE')
    $botToken = [Environment]::GetEnvironmentVariable('FLUFFY_BUILD_TG_BOT_TOKEN')
    $chatId = [Environment]::GetEnvironmentVariable('FLUFFY_BUILD_TG_CHAT_ID')
    $topicId = [Environment]::GetEnvironmentVariable('FLUFFY_BUILD_TG_TOPIC_ID')
    $enabled = -not [string]::IsNullOrWhiteSpace($apiBase) -and -not [string]::IsNullOrWhiteSpace($botToken) -and -not [string]::IsNullOrWhiteSpace($chatId)

    $notifier = [pscustomobject]@{
        Enabled          = $enabled
        ApiBase          = $apiBase.TrimEnd('/')
        BotToken         = $botToken
        ChatId           = $chatId
        TopicId          = $topicId
        BuildStart       = Get-Date
        MessageId        = $null
        CurrentTask      = ''
        LastEditAt       = [datetime]::MinValue
        LastErrorTail    = [System.Collections.Generic.List[string]]::new()
        GradleTasksLabel = ($GradleTasks -join ', ')
        LastText         = ''
        FinalMessageSent = $false
    }

    if ($enabled) {
        Write-Host 'Telegram build notifications enabled.' -ForegroundColor Yellow
    }

    return $notifier
}

function Disable-TelegramNotifier {
    param(
        [Parameter(Mandatory = $true)]
        [psobject]$Notifier,
        [Parameter(Mandatory = $true)]
        [string]$Reason
    )

    if ($Notifier.Enabled) {
        Write-Warning "Telegram notifications disabled: $Reason"
    }
    $Notifier.Enabled = $false
}

function Invoke-TelegramApi {
    param(
        [Parameter(Mandatory = $true)]
        [psobject]$Notifier,
        [Parameter(Mandatory = $true)]
        [string]$Method,
        [Parameter(Mandatory = $true)]
        [hashtable]$Body
    )

    if (-not $Notifier.Enabled) {
        return $null
    }

    try {
        return Invoke-RestMethod `
            -Method Post `
            -Uri "$($Notifier.ApiBase)/bot$($Notifier.BotToken)/$Method" `
            -Body $Body `
            -ContentType 'application/x-www-form-urlencoded' `
            -TimeoutSec 5
    } catch {
        $details = $_.Exception.Message
        if ($_.ErrorDetails -and -not [string]::IsNullOrWhiteSpace($_.ErrorDetails.Message)) {
            $details = $_.ErrorDetails.Message
        }

        if ($details -match 'message is not modified') {
            Write-Host "Telegram $Method skipped: message is not modified." -ForegroundColor DarkYellow
            return @{
                ok = $true
            }
        }

        Write-Warning "Telegram $Method failed: $details"
        Disable-TelegramNotifier -Notifier $Notifier -Reason $details
        return $null
    }
}

function Get-TelegramStatusText {
    param(
        [Parameter(Mandatory = $true)]
        [psobject]$Notifier,
        [Parameter(Mandatory = $true)]
        [string]$State,
        [string]$Summary = ''
    )

    $elapsed = Format-Duration -Duration ((Get-Date) - $Notifier.BuildStart)
    $lines = [System.Collections.Generic.List[string]]::new()
    $lines.Add('fluffyGram debug build')
    $lines.Add("State: $State")
    $lines.Add("Elapsed: $elapsed")
    $lines.Add("Tasks: $($Notifier.GradleTasksLabel)")

    if (-not [string]::IsNullOrWhiteSpace($Notifier.CurrentTask)) {
        $lines.Add("Current task: $($Notifier.CurrentTask)")
    }
    if (-not [string]::IsNullOrWhiteSpace($Summary)) {
        $lines.Add('')
        $lines.Add($Summary)
    }

    return ($lines -join "`n")
}

function Update-TelegramStatus {
    param(
        [Parameter(Mandatory = $true)]
        [psobject]$Notifier,
        [Parameter(Mandatory = $true)]
        [string]$State,
        [string]$Summary = '',
        [switch]$Force
    )

    if (-not $Notifier.Enabled) {
        return
    }

    $now = Get-Date
    if (-not $Force -and $Notifier.MessageId -and ($now - $Notifier.LastEditAt).TotalSeconds -lt 4) {
        return
    }

    $text = Get-TelegramStatusText -Notifier $Notifier -State $State -Summary $Summary
    if ($text -eq $Notifier.LastText) {
        return
    }

    $body = @{
        chat_id              = $Notifier.ChatId
        text                 = $text
        disable_notification = $true
    }
    if (-not [string]::IsNullOrWhiteSpace($Notifier.TopicId)) {
        $body.message_thread_id = $Notifier.TopicId
    }

    if ($Notifier.MessageId) {
        $body.message_id = $Notifier.MessageId
        $response = Invoke-TelegramApi -Notifier $Notifier -Method 'editMessageText' -Body $body
        if ($response) {
            $Notifier.LastEditAt = $now
            $Notifier.LastText = $text
        }
        return
    }

    $response = Invoke-TelegramApi -Notifier $Notifier -Method 'sendMessage' -Body $body
    if ($response -and $response.ok) {
        $Notifier.MessageId = $response.result.message_id
        $Notifier.LastEditAt = $now
        $Notifier.LastText = $text
    }
}

function Send-TelegramFinalMessage {
    param(
        [Parameter(Mandatory = $true)]
        [psobject]$Notifier,
        [Parameter(Mandatory = $true)]
        [string]$State,
        [string]$Summary = ''
    )

    if (-not $Notifier.Enabled -or $Notifier.FinalMessageSent) {
        return
    }

    $text = Get-TelegramStatusText -Notifier $Notifier -State $State -Summary $Summary
    $body = @{
        chat_id = $Notifier.ChatId
        text    = $text
    }
    if (-not [string]::IsNullOrWhiteSpace($Notifier.TopicId)) {
        $body.message_thread_id = $Notifier.TopicId
    }

    $response = Invoke-TelegramApi -Notifier $Notifier -Method 'sendMessage' -Body $body
    if ($response -and $response.ok) {
        $Notifier.FinalMessageSent = $true
    }
}

function Add-TelegramErrorTail {
    param(
        [Parameter(Mandatory = $true)]
        [psobject]$Notifier,
        [Parameter(Mandatory = $true)]
        [string]$Line
    )

    if ([string]::IsNullOrWhiteSpace($Line)) {
        return
    }

    $Notifier.LastErrorTail.Add($Line.Trim())
    while ($Notifier.LastErrorTail.Count -gt 6) {
        $Notifier.LastErrorTail.RemoveAt(0)
    }
}

function Process-GradleOutputLine {
    param(
        [Parameter(Mandatory = $true)]
        [psobject]$Notifier,
        [Parameter(Mandatory = $true)]
        [string]$Line,
        [switch]$IsError
    )

    if ($Line -match '^\> Task (?<task>\S+)') {
        $taskName = $Matches['task']
        if ($taskName -ne $Notifier.CurrentTask) {
            $Notifier.CurrentTask = $taskName
            Update-TelegramStatus -Notifier $Notifier -State 'running'
        }
    }

    if ($IsError -or $Line -match '^(FAILURE:|BUILD FAILED|Execution failed|What went wrong:|Caused by:)') {
        Add-TelegramErrorTail -Notifier $Notifier -Line $Line
    }
}

function Should-SkipConsoleLine {
    param(
        [string]$Line
    )

    if ([string]::IsNullOrEmpty($Line)) {
        return $false
    }

    return $Line -match '^FINDSTR:\s+(Cannot open|Не удается открыть)\s+>NUL$'
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

    $envFilePath = Join-Path $scriptRoot '.env'
    $script:buildNotifier = New-TelegramNotifier -GradleTasks $gradleTasks -EnvFilePath $envFilePath

    Write-Host "Running Gradle tasks: $($gradleTasks -join ', ')" -ForegroundColor Cyan
    $gradleArgs = @('--console=plain')
    if ($NoDaemon) {
        $gradleArgs += '--no-daemon'
    }
    $gradleArgs += $gradleTasks
    Update-TelegramStatus -Notifier $script:buildNotifier -State 'starting' -Force
    if ($isWindowsPlatform) {
        & cmd.exe /d /c "$gradleWrapper $($gradleArgs -join ' ')" 2>&1 | ForEach-Object {
            $line = $_.ToString()
            if (Should-SkipConsoleLine -Line $line) {
                return
            }
            Write-Host $line
            if (-not [string]::IsNullOrEmpty($line)) {
                Process-GradleOutputLine -Notifier $script:buildNotifier -Line $line
            }
        }
    } else {
        & $gradleWrapper @gradleArgs 2>&1 | ForEach-Object {
            $line = $_.ToString()
            if (Should-SkipConsoleLine -Line $line) {
                return
            }
            Write-Host $line
            if (-not [string]::IsNullOrEmpty($line)) {
                Process-GradleOutputLine -Notifier $script:buildNotifier -Line $line
            }
        }
    }
    $gradleExitCode = $LASTEXITCODE
    if ($gradleExitCode -ne 0) {
        $failureSummary = if ($script:buildNotifier.LastErrorTail.Count -gt 0) {
            'Last output:' + "`n" + ($script:buildNotifier.LastErrorTail -join "`n")
        } else {
            "Gradle exited with code $gradleExitCode."
        }
        Update-TelegramStatus -Notifier $script:buildNotifier -State 'failed' -Summary $failureSummary -Force
        Send-TelegramFinalMessage -Notifier $script:buildNotifier -State 'failed' -Summary $failureSummary
        throw "Gradle build failed with exit code $gradleExitCode"
    }
    Update-TelegramStatus -Notifier $script:buildNotifier -State 'completed' -Summary 'Gradle build finished successfully.' -Force

    if (-not $Install) {
        Write-Host 'Debug build completed successfully.' -ForegroundColor Green
        Send-TelegramFinalMessage -Notifier $script:buildNotifier -State 'completed' -Summary 'Build completed without install.'
        Write-Host 'Install skipped.' -ForegroundColor Yellow
    } elseif ($Launch) {
        Write-Host 'Debug build installed successfully.' -ForegroundColor Green
        $adbCommand = Get-Command adb -ErrorAction SilentlyContinue
        if (-not $adbCommand) {
            Update-TelegramStatus -Notifier $script:buildNotifier -State 'failed' -Summary 'adb was not found in PATH.' -Force
            Send-TelegramFinalMessage -Notifier $script:buildNotifier -State 'failed' -Summary 'adb was not found in PATH.'
            throw 'adb was not found in PATH. Set ANDROID_SDK_ROOT/ANDROID_HOME or install platform-tools.'
        }

        Write-Host 'Launching app...' -ForegroundColor Cyan
        & $adbCommand.Source shell am start -n 'org.ushastoe.fluffy.beta/org.telegram.ui.LaunchActivity' | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Update-TelegramStatus -Notifier $script:buildNotifier -State 'failed' -Summary "adb launch failed with exit code $LASTEXITCODE" -Force
            Send-TelegramFinalMessage -Notifier $script:buildNotifier -State 'failed' -Summary "adb launch failed with exit code $LASTEXITCODE"
            throw "adb launch failed with exit code $LASTEXITCODE"
        }
        Send-TelegramFinalMessage -Notifier $script:buildNotifier -State 'completed' -Summary 'Build installed and app launched on device.'
        Write-Host 'App launched on device.' -ForegroundColor Green
    } else {
        Write-Host 'Debug build installed successfully.' -ForegroundColor Green
        $postBuildSummary = if ($Install) { 'Build installed successfully.' } else { 'Build completed without install.' }
        Send-TelegramFinalMessage -Notifier $script:buildNotifier -State 'completed' -Summary $postBuildSummary
        Write-Host 'Launch skipped.' -ForegroundColor Yellow
    }
}
finally {
    Pop-Location
}
