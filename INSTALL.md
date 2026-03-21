# 智智慧投篮机 - 开发环境安装指南

## 当前环境状态

### 已检测到的工具
- ❌ JDK: Java 1.8 (需要JDK 17)
- ❌ Android SDK: 未安装
- ❌ Gradle: 未配置

### 需要的工具版本
| 工具 | 需要的版本 | 最低版本 |
|------|------------|----------|
| JDK | 17+ | 17 |
| Android SDK | 34 | 26 |
| Gradle | 8.2+ | 8.0 |
| Android Studio | Hedgehog (2023.1.1)+ | 2021.1 |

## 安装步骤

### 步骤1: 安装JDK 17

#### Windows
1. 下载JDK 17: https://adoptium.net/releases.html?variant=openjdk17
2. 选择 "Eclipse Temurin JDK 17 (LTS) - Windows x64"
3. 运行安装程序
4. 设置环境变量:
   ```
   JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot
   PATH = %JAVA_HOME%\bin;%PATH%
   ```

#### 验证安装
```bash
java -version
# 应该显示: openjdk version "17.x.x"
javac -version
# 应该显示: javac 17.x.x
```

### 步骤2: 安装Android SDK

#### 使用命令行安装
```bash
# 下载命令行工具
curl -o commandlinetools.zip https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip

# 解压到Android SDK目录
mkdir C:\Android\cmdline-tools
unzip commandlinetools.zip -d C:\Android\cmdline-tools
mv C:\Android\cmdline-tools\cmdline-tools C:\Android\cmdline-tools\latest

# 设置环境变量
setx ANDROID_HOME=C:\Android
setx PATH=%PATH%;%ANDROID_HOME%\cmdline-tools\latest\bin;%ANDROID_HOME%\platform-tools

# 接受许可证
yes | sdkmanager --licenses

# 安装必要的SDK包
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
```

#### 使用Android Studio安装
1. 下载Android Studio: https://developer.android.com/studio
2. 运行安装程序
3. SDK会自动安装到 `C:\Users\<用户名>\AppData\Local\Android\Sdk`

### 步骤3: 配置Gradle

#### 使用Gradle Wrapper
项目已包含Gradle Wrapper，无需单独安装。

#### 如果需要全局安装
```bash
# 下载Gradle 8.2
curl -o gradle-8.2-bin.zip https://services.gradle.org/distributions/gradle-8.2-bin.zip

# 解压
unzip gradle-8.2-bin.zip -d C:\Gradle

# 设置环境变量
setx GRADLE_HOME=C:\Gradle\gradle-8.2
setx PATH=%PATH%;%GRADLE_HOME%\bin
```

### 步骤4: 配置USB驱动

#### Windows
1. 打开"设备管理器"
2. 连接Android设备
3. 右键点击"Android Device" -> "更新驱动程序"
4. 选择"浏览我的电脑"
5. 选择Android SDK目录中的USB驱动:
   ```
   %ANDROID_HOME%\extras\google\usb_driver
   ```

#### 启用USB调试
1. 在Android设备上打开"设置"
2. 关于手机 -> 连续点击"版本号"7次
3. 返回设置 -> 开发者选项
4. 启用"USB调试"
5. 允许USB调试授权

### 步骤5: 测试环境

#### 检查所有工具
```bash
# 检查Java
java -version

# 检查SDK Manager
sdkmanager --version

# 检查ADB
adb version

# 检查Gradle
gradle -v
```

#### 预期输出
```
java version "17.0.x"
Android SDK Manager, version x.x.x
Android Debug Bridge version x.x.x
Gradle 8.2
```

### 步骤6: 构建项目

```bash
# 进入项目目录
cd D:\focusnt\phm\trunk\CODE\phm-basketball

# 清理和构建
gradlew clean
gradlew assembleDebug

# 安装到设备
adb install app\build\outputs\apk\debug\app-debug.apk
```

## 常见问题

### Q1: Java版本不兼容
```bash
# 检查当前Java版本
java -version

# 如果不是17，修改JAVA_HOME指向JDK 17
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot
```

### Q2: Android SDK未找到
```bash
# 检查ANDROID_HOME
echo %ANDROID_HOME%

# 如果为空，设置正确的路径
setx ANDROID_HOME=C:\Users\<用户名>\AppData\Local\Android\Sdk
```

### Q3: 设备未连接
```bash
# 查看已连接的设备
adb devices

# 如果列表为空:
# 1. 检查USB线
# 2. 启用USB调试
# 3. 安装正确的USB驱动
```

### Q4: 权限错误
```bash
# Windows: 以管理员身份运行命令提示符
# Linux/Mac: 使用sudo
sudo adb devices
```

## 一键安装脚本

创建一个 `install.bat` 文件:

```batch
@echo off
chcp 65001 >nul
echo ========================================
echo   智智慧投篮机 - 开发环境一键安装
echo ========================================

echo [1/4] 检查环境...
echo.

echo [2/4] 安装JDK 17...
rem 下载和安装JDK 17
curl -L -o jdk17.zip "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9.9.9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9.9.9.zip"
mkdir C:\Program Files\Eclipse Adoptium
tar -xf jdk17.zip -C "C:\Program Files\Eclipse Adoptium"
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9.9-hotspot"
setx PATH "%JAVA_HOME%\bin;%PATH%"
echo ✓ JDK 17安装完成

echo.
echo [3/4] 安装Android SDK...
rem 下载Android命令行工具
curl -L -o cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip
mkdir C:\Android
tar -xf cmdline-tools.zip -d C:\Android
mkdir C:\Android\cmdline-tools\latest
move C:\Android\cmdline-tools\cmdline-tools C:\Android\cmdline-tools\latest\nul 2>nul
setx ANDROID_HOME C:\Android
setx PATH "%PATH%;%ANDROID_HOME%\cmdline-tools\latest\bin;%ANDROID_HOME%\platform-tools"
echo ✓ Android SDK安装完成

echo.
echo [4/4] 安装SDK包...
yes | sdkmanager --licenses
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
echo ✓ SDK包安装完成

echo.
echo ========================================
echo   安装完成！
echo ========================================
echo.
echo 请重启命令提示符以应用环境变量
echo 然后运行: gradlew clean build
pause
```

## 验证清单

安装完成后，验证以下各项:

- [ ] Java 17已安装并可用
- [ ] Android SDK 34已安装
- [ ] ADB工具可用
- [ ] Gradle Wrapper可用
- [ ] USB驱动已安装
- [ ] 设备可以连接并调试

## 下一步

环境安装完成后，运行以下命令构建项目:

```bash
cd D:\focusnt\phm\trunk\CODE\phm-basketball
gradlew clean build
adb install app\build\outputs\apk\debug\app-debug.apk
```
