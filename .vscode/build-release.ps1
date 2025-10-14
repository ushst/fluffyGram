# Настройка Java окружения для FluffyGram
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
$env:GRADLE_OPTS = "-Dorg.gradle.java.home=`"$env:JAVA_HOME`""

# Сборка и установка Release версии
Write-Host "Сборка FluffyGram Release..." -ForegroundColor Green
./gradlew :TMessagesProj:assembleRelease :TMessagesProj_App:assembleAfatRelease :TMessagesProj_App:installAfatRelease

if ($LASTEXITCODE -eq 0) {
    Write-Host "Сборка завершена успешно!" -ForegroundColor Green
    Write-Host "Запуск приложения..." -ForegroundColor Cyan
    adb shell am start -n org.ushastoe.fluffy/org.telegram.ui.LaunchActivity
    Write-Host "Приложение запущено!" -ForegroundColor Green
} else {
    Write-Host "Сборка не удалась!" -ForegroundColor Red
}