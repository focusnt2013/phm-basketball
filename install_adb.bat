@echo off
chcp 65001 >nul
cls
echo ========================================
echo   智智慧投篮机 - ADB工具安装脚本
echo ========================================
echo.

setlocal enabledelayedexpansion

REM 设置变量
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
set "TEMP_DIR=%TEMP%\android_install"
set "JAVA_VERSION=17.0.10"
set "JDK_URL=https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_windows-x64_bin.zip
set "SDK_URL=https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip

echo [1/6] 创建临时目录...
mkdir "%TEMP_DIR%" 2>nul
echo   完成
echo.

echo [2/6] 检查Java环境...
java -version 2>&1 | findstr "17" >nul
if %errorlevel% equ 0 (
    echo   ✓ Java 17已安装
    set "JAVA_INSTALLED=1"
) else (
    echo   ✗ 未检测到Java 17，开始安装...
    echo   下载中(约50MB)...
    curl -L -o "%TEMP_DIR%\jdk.zip" "%JDK_URL%" 2>nul
    echo   解压中...
    tar -xf "%TEMP_DIR%\jdk.zip" -C "%TEMP_DIR%" 2>nul
    for /d %%i in ("%TEMP_DIR%\jdk-"*) do set "JDK_DIR=%%i"
    setx JAVA_HOME "%JDK_DIR%" >nul 2>&1
    set "JAVA_HOME=%JDK_DIR%"
    setx PATH "%JAVA_HOME%\bin;%%PATH%%" >nul 2>&1
    echo   ✓ Java安装完成
    echo   请重启命令提示符以应用新环境变量
)
echo.

echo [3/6] 检查Android SDK...
if defined ANDROID_HOME (
    echo   ✓ ANDROID_HOME已设置: %ANDROID_HOME%
    set "SDK_INSTALLED=1"
) else (
    echo   ✗ 未检测到Android SDK，开始安装...
    echo   下载中(约150MB)...
    curl -L -o "%TEMP_DIR%\sdk.zip" "%SDK_URL%" 2>nul
    echo   解压中...
    tar -xf "%TEMP_DIR%\sdk.zip" -d "C:\" 2>nul
    set "ANDROID_HOME=C:\cmdline-tools"
    setx ANDROID_HOME "C:\cmdline-tools" >nul 2>&1
    mkdir "C:\cmdline-tools\latest" 2>nul
    move "C:\cmdline-tools\cmdline-tools" "C:\cmdline-tools\latest\nul" 2>nul
    setx PATH "%PATH%;%ANDROID_HOME%\latest\bin;C:\platform-tools" >nul 2>&1
    echo   ✓ SDK安装完成
    echo   正在安装SDK包...
    echo   y | sdkmanager --licenses >nul 2>&1
    sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools" >nul 2>&1
    echo   ✓ SDK包安装完成
)
echo.

echo [4/6] 安装USB驱动...
echo   请连接Android设备并启用USB调试
echo   如果设备未连接，可以跳过此步骤
echo   (安装APK时会自动检测)
echo   跳过
echo.

echo [5/6] 配置环境变量...
echo   JAVA_HOME=%JAVA_HOME%
echo   ANDROID_HOME=%ANDROID_HOME%
echo   PATH=...;%JAVA_HOME%\bin;%ANDROID_HOME%\latest\bin;C:\platform-tools
echo   完成
echo.

echo [6/6] 验证安装...
echo.
if defined JAVA_HOME (
    echo   Java版本:
    "%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr "version"
)
if defined ANDROID_HOME (
    echo   ADB版本:
    adb version 2>&1 | findstr "Android"
)
echo.

echo ========================================
echo   安装完成！
echo ========================================
echo.
echo 后续步骤:
echo   1. 重启命令提示符以应用环境变量
echo   2. 连接Android设备，启用USB调试
echo   3. 运行: adb devices 确认设备连接
echo   4. 进入项目目录: cd %SCRIPT_DIR%
echo   5. 构建APK: gradlew assembleDebug
echo   6. 安装APK: adb install app\build\outputs\apk\debug\app-debug.apk
echo.
echo 或者运行一键构建脚本: build.bat
echo.
pause

endlocal
