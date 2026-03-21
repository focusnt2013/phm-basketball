@echo off
chcp 65001 >nul
echo ========================================
echo   智智慧投篮机 Android 构建脚本
echo ========================================
echo.

setlocal

REM 设置项目路径
set PROJECT_DIR=%~dp0
set PROJECT_DIR=%PROJECT_DIR:~0,-1%
cd "%PROJECT_DIR%"

REM 检查Gradle是否可用
echo [1/6] 检查构建环境...
echo 项目路径: %PROJECT_DIR%

if exist "gradlew.bat" (
    echo ✓ Gradle Wrapper 已找到
) else (
    echo ✗ Gradle Wrapper 未找到，请先运行 'gradle wrapper'
    goto :error
)

if exist "gradle\wrapper\gradle-wrapper.jar" (
    echo ✓ Gradle Wrapper JAR 已找到
) else (
    echo ✗ Gradle Wrapper JAR 未找到
    echo 正在下载...
    call gradle wrapper --gradle-version 8.2
)

echo.
echo [2/6] 清理构建...
call gradlew clean --no-daemon
if %ERRORLEVEL% NEQ 0 goto :error

echo.
echo [3/6] 运行代码检查...
call gradlew lint --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo 警告: Lint检查发现问题，但继续构建...
)

echo.
echo [4/6] 运行单元测试...
call gradlew testDebugUnitTest --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo ✗ 单元测试失败
    goto :error
)
echo ✓ 单元测试通过

echo.
echo [5/6] 构建调试版本...
call gradlew assembleDebug --no-daemon
if %ERRORLEVEL% NEQ 0 goto :error
echo ✓ 调试版本构建成功

echo.
echo [6/6] 构建发布版本...
call gradlew assembleRelease --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo ✗ 发布版本构建失败
    goto :error
)
echo ✓ 发布版本构建成功

echo.
echo ========================================
echo   构建完成！
echo ========================================
echo.
echo 输出文件:
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo   调试版本: app\build\outputs\apk\debug\app-debug.apk
)
if exist "app\build\outputs\apk\release\app-release.apk" (
    echo   发布版本: app\build\outputs\apk\release\app-release.apk
)
echo.
echo 后续步骤:
echo   1. 运行 'adb install app\build\outputs\apk\debug\app-debug.apk' 安装调试版本
echo   2. 配置服务器地址: 修改 app/src/main/kotlin/.../NetworkModule.kt 中的 BASE_URL
echo   3. 集成百度AI SDK: 按照 SDK 文档配置 API Key
echo   4. 集成MediaPipe: 添加手势识别模型文件到 assets 目录
echo.

goto :end

:error
echo.
echo ========================================
echo   构建失败！
echo ========================================
echo.
echo 请检查错误信息并修复后重试。
echo 常见问题:
echo   - Gradle版本不匹配: 检查 gradle-wrapper.properties 中的版本
echo   - 依赖下载失败: 检查网络连接
echo   - 内存不足: 增加GRADLE_OPTS内存限制
echo.

:end
endlocal
pause
