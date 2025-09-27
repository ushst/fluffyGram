# Настройка Java окружения для FluffyGram
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-23.0.1.11-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
$env:GRADLE_OPTS = "-Dorg.gradle.java.home=`"$env:JAVA_HOME`""

# Сборка и установка Release версии
Write-Host "🔨 Building FluffyGram Release..." -ForegroundColor Green
./gradlew :TMessagesProj:assembleRelease :TMessagesProj_App:assembleAfatRelease :TMessagesProj_App:installAfatRelease

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Build completed successfully!" -ForegroundColor Green
    Write-Host "🚀 Launching app..." -ForegroundColor Cyan
    adb shell am start -n org.ushastoe.fluffy/org.telegram.ui.LaunchActivity
    Write-Host "📱 App launched!" -ForegroundColor Green
} else {
    Write-Host "❌ Build failed!" -ForegroundColor Red
}