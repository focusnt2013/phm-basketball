@echo off
chcp 65001 >nul
cls
echo ========================================
echo   智智慧投篮机 - Android构建工具
echo ========================================

setlocal enabledelayedexpansion

REM 设置项目路径
set "PROJECT_DIR=%~dp0"
set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

echo.
echo [1/8] 检查环境...
echo.

REM 检查Java
echo   检查Java...
java -version 2>&1 | findstr "17" >nul
if %errorlevel% equ 0 (
    echo   ✓ Java 17已安装
    set "JAVA_OK=1"
) else (
    echo   ✗ Java 17未安装或版本不正确
    echo   请运行: install_adb.bat
    goto :error
)

REM 检查Android SDK
echo   检查Android SDK...
if defined ANDROID_HOME (
    if exist "%ANDROID_HOME%\platforms\android-34" (
        echo   ✓ Android SDK 34已安装
        set "SDK_OK=1"
    ) else (
        echo   ✗ Android SDK包未安装
        echo   请运行: sdkmanager "platforms;android-34"
        goto :error
    )
) else (
    echo   ✗ ANDROID_HOME未设置
    echo   请运行: install_adb.bat
    goto :error
)

REM 检查ADB
echo   检查ADB...
adb version >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✓ ADB已安装
    set "ADB_OK=1"
) else (
    echo   ✗ ADB未安装
    goto :error
)

echo.
echo [2/8] 清理构建...
cd /d "%PROJECT_DIR%"
call gradlew clean --no-daemon
if %errorlevel% neq 0 (
    echo   ✗ 清理失败
    goto :error
)
echo   ✓ 清理完成

echo.
echo [3/8] 运行Lint检查...
call gradlew lintDebug --no-daemon
if %errorlevel% neq 0 (
    echo   ⚠ Lint检查发现问题，但继续构建...
)
echo   ✓ Lint检查完成

echo.
echo [4/8] 运行单元测试...
call gradlew testDebugUnitTest --no-daemon
if %errorlevel% neq 0 (
    echo   ✗ 单元测试失败
    goto :error
)
echo   ✓ 单元测试通过

echo.
echo [5/8] 构建调试版本...
call gradlew assembleDebug --no-daemon
if %errorlevel% neq 0 (
    echo   ✗ 调试版本构建失败
    goto :error
)
echo   ✓ 调试版本构建成功

echo.
echo [6/8] 构建发布版本...
call gradlew assembleRelease --no-daemon
if %errorlevel% neq 0 (
    echo   ✗ 发布版本构建失败
    goto :error
)
echo   ✓ 发布版本构建成功

echo.
echo [7/8] 检测Android设备...
echo   正在搜索设备...
adb devices | findstr "device$" >nul
if %errorlevel% equ 0 (
    echo   ✓ 已检测到Android设备
    set "DEVICE_FOUND=1"
    
    echo.
    echo [8/8] 安装APK到设备...
    for /f "tokens=2" %%a in ('adb devices ^| findstr "device$"') do (
        echo   安装到设备: %%a
        adb -s %%a install -r "app\build\outputs\apk\debug\app-debug.apk"
        if %errorlevel% equ 0 (
            echo   ✓ APK安装成功
        ) else (
            echo   ✗ APK安装失败
        )
    )
) else (
    echo   ⚠ 未检测到Android设备
    echo   请连接设备并启用USB调试后运行: adb install app\build\outputs\apk\debug\app-debug.apk
)

echo.
echo ========================================
echo   构建完成！
echo ========================================
echo.
echo 输出文件:
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo   ✓ 调试版本: app\build\outputs\apk\debug\app-debug.apk
)
if exist "app\build\outputs\apk\release\app-release-unsigned.apk" (
    echo   ✓ 发布版本: app\build\outputs\apk\release\app-release-unsigned.apk
)
echo.
echo 后续操作:
echo   1. 查看测试报告: app\build\reports\tests\testDebugUnitTest\index.html
echo   2. 查看APK大小: dir app\build\outputs\apk\debug\app-debug.apk
echo   3. 查看Lint报告: app\build\reports\lint\lint-results.html
echo.
echo 手动安装APK:
echo   adb install app\build\outputs\apk\debug\app-debug.apk
echo.

goto :end

:error
echo.
echo ========================================
echo   构建失败！
echo ========================================
echo.
echo 请检查错误信息并重试。
echo 常见问题:
echo   - Java版本不正确: 请安装JDK 17
echo   - Android SDK未安装: 请运行install_adb.bat
echo   - 单元测试失败: 检查测试用例
echo   - 设备未连接: 连接设备并启用USB调试
echo.

:end
endlocal
pause
