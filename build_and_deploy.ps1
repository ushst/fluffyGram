# FluffyGram Build & Deploy Script
# Автор: GitHub Copilot
# Описание: Скрипт для сборки, установки и запуска FluffyGram

param(
    [switch]$Debug,
    [switch]$Release = $true,
    [switch]$Launch = $true,
    [switch]$Clean
)

# Настройка Java окружения
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-23.0.1.11-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
$env:GRADLE_OPTS = "-Dorg.gradle.java.home=`"$env:JAVA_HOME`""

Write-Host "🚀 FluffyGram Build & Deploy Script" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan

# Проверка Java
Write-Host "☕ Checking Java version..." -ForegroundColor Yellow
java -version

# Очистка если запрошена
if ($Clean) {
    Write-Host "🧹 Cleaning build..." -ForegroundColor Yellow
    ./gradlew clean
}

# Сборка и установка
if ($Debug) {
    Write-Host "🔨 Building DEBUG version..." -ForegroundColor Green
    ./gradlew :TMessagesProj_App:assembleAfatDebug :TMessagesProj_App:installAfatDebug
} else {
    Write-Host "🔨 Building RELEASE version..." -ForegroundColor Green
    ./gradlew :TMessagesProj:assembleRelease :TMessagesProj_App:assembleAfatRelease :TMessagesProj_App:installAfatRelease
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Build and install completed successfully!" -ForegroundColor Green
    
    if ($Launch) {
        Write-Host "🚀 Launching app..." -ForegroundColor Cyan
        adb shell am start -n org.ushastoe.fluffy/org.telegram.ui.LaunchActivity
        Write-Host "📱 App launched on device!" -ForegroundColor Green
    }
} else {
    Write-Host "❌ Build failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "🎉 Done!" -ForegroundColor Magenta