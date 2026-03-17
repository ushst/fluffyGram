param(
    [string]$Root = "TMessagesProj/src/main/java/org/telegram/ui",
    [string]$OutDir = "."
)

$ErrorActionPreference = "Stop"

$allFiles = Get-ChildItem -Path $Root -Recurse -Filter *.java | ForEach-Object { $_.FullName.Replace((Get-Location).Path + '\\', '') }

$summary = @()
$filesWithoutHook = @()
$topPriority = @()

foreach ($file in $allFiles) {
    $content = Get-Content $file -Raw

    $hasTextOps = ($content -match 'new\s+TextView\s*\(') -or
                  ($content -match 'new\s+TextPaint\s*\(') -or
                  ($content -match 'new\s+Text\s*\(') -or
                  ($content -match '\.setTypeface\s*\(')

    if (-not $hasTextOps) {
        continue
    }

    $hasHook = $content -match 'AppFontHook'
    if (-not $hasHook) {
        $filesWithoutHook += $file
    }

    $suspicious = Select-String -Path $file -Pattern 'setTypeface\(AndroidUtilities\.bold\(\)\)|setTypeface\(Typeface\.DEFAULT|new\s+TextPaint\s*\(|new\s+Text\s*\(' -AllMatches
    $suspiciousCount = if ($suspicious) { $suspicious.Count } else { 0 }

    $summary += [PSCustomObject]@{
        File = $file
        HasAppFontHook = $hasHook
        SuspiciousOps = $suspiciousCount
    }

    if ((-not $hasHook) -and $suspiciousCount -gt 0) {
        $topPriority += [PSCustomObject]@{
            File = $file
            SuspiciousOps = $suspiciousCount
        }
    }
}

$summary = $summary | Sort-Object @{ Expression = 'SuspiciousOps'; Descending = $true }, File
$topPriority = $topPriority | Sort-Object @{ Expression = 'SuspiciousOps'; Descending = $true }, File

$summaryPath = Join-Path $OutDir "font_audit_summary.csv"
$missingPath = Join-Path $OutDir "font_audit_missing_hook.txt"
$priorityPath = Join-Path $OutDir "font_audit_top_priority.txt"

$summary | Export-Csv -Path $summaryPath -NoTypeInformation -Encoding UTF8
$filesWithoutHook | Set-Content -Path $missingPath -Encoding UTF8
$topPriority | ForEach-Object { "{0}`t{1}" -f $_.SuspiciousOps, $_.File } | Set-Content -Path $priorityPath -Encoding UTF8

Write-Output "Scanned files with text ops: $($summary.Count)"
Write-Output "Files without AppFontHook: $($filesWithoutHook.Count)"
Write-Output "Top priority entries: $($topPriority.Count)"
Write-Output "Summary: $summaryPath"
Write-Output "Missing-hook list: $missingPath"
Write-Output "Top-priority list: $priorityPath"
