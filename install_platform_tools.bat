@echo off
chcp 65001 >nul
cls
echo ========================================
echo   智智慧投篮机 - ADB工具安装向导
echo ========================================

setlocal enabledelayedexpansion

set "INSTALL_DIR=%USERPROFILE%\Android"
set "TEMP_DIR=%TEMP%\adb_install"

echo.
echo [1/5] 创建安装目录...
mkdir "%INSTALL_DIR%" 2>nul
mkdir "%TEMP_DIR%" 2>nul
echo   完成

echo.
echo [2/5] 下载ADB工具包...
set "ADB_URL=https://dl.google.com/android/repository/platform-tools-latest-windows.zip"
echo   下载中(约5MB)...
curl -L -o "%TEMP_DIR%\platform-tools.zip" "%ADB_URL%" 2>nul
if exist "%TEMP_DIR%\platform-tools.zip" (
    echo   ✓ 下载完成
) else (
    echo   ✗ 下载失败
    goto :error
)

echo.
echo [3/5] 解压ADB工具...
tar -xf "%TEMP_DIR%\platform-tools.zip" -d "%INSTALL_DIR%" 2>nul
if exist "%INSTALL_DIR%\platform-tools\adb.exe" (
    echo   ✓ 解压完成
) else (
    echo   ✗ 解压失败
    goto :error
)

echo.
echo [4/5] 配置环境变量...
setx PATH "%PATH%;%INSTALL_DIR%\platform-tools" >nul 2>&1
set "PATH=%PATH%;%INSTALL_DIR%\platform-tools"
echo   ✓ 环境变量已更新

echo.
echo [5/5] 验证安装...
adb version >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✓ ADB安装成功
    adb version
) else (
    echo   ✗ ADB验证失败
    goto :error
)

echo.
echo ========================================
echo   ADB安装完成！
echo ========================================
echo.
echo ADB路径: %INSTALL_DIR%\platform-tools
echo.
echo 后续步骤:
echo   1. 连接Android设备
echo   2. 在设备上启用USB调试
echo   3. 运行: adb devices
echo   4. 允许设备调试授权
echo   5. 运行: gradlew assembleDebug
echo.
echo 或者运行快速构建: quick_build.bat
echo.

goto :end

:error
echo.
echo ========================================
echo   安装失败！
echo ========================================
echo.
echo 请检查网络连接后重试。
echo 手动下载链接:
echo   %ADB_URL%
echo.

:end
endlocal
pause
