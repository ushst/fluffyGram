@echo off
echo 🚀 FluffyGram Quick Build & Deploy
echo ═══════════════════════════════════

REM Настройка Java окружения
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-23.0.1.11-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "GRADLE_OPTS=-Dorg.gradle.java.home=\"%JAVA_HOME%\""

echo ☕ Java version:
java -version

echo.
echo 🔨 Building and installing FluffyGram...
gradlew.bat :TMessagesProj:assembleRelease :TMessagesProj_App:assembleAfatRelease :TMessagesProj_App:installAfatRelease

if %ERRORLEVEL% EQU 0 (
    echo ✅ Build completed successfully!
    echo 🚀 Launching app...
    adb shell am start -n org.ushastoe.fluffy/org.telegram.ui.LaunchActivity
    echo 📱 App launched!
    echo 🎉 Done!
) else (
    echo ❌ Build failed!
    pause
    exit /b %ERRORLEVEL%
)

pause