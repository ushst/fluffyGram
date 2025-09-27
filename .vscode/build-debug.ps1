# Настройка Java окружения для FluffyGram
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-23.0.1.11-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
$env:GRADLE_OPTS = "-Dorg.gradle.java.home=`"$env:JAVA_HOME`""

# Сборка и установка Debug версии
Write-Host "🔨 Building FluffyGram Debug..." -ForegroundColor Yellow
./gradlew :TMessagesProj_App:assembleAfatDebug :TMessagesProj_App:installAfatDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Debug build completed successfully!" -ForegroundColor Green
    Write-Host "🚀 Launching app..." -ForegroundColor Cyan
    adb shell am start -n org.ushastoe.fluffy/org.telegram.ui.LaunchActivity
    Write-Host "📱 App launched!" -ForegroundColor Green
} else {
    Write-Host "❌ Build failed!" -ForegroundColor Red
}