# 智智慧投篮机 - 快速参考

## 常用命令

### 环境安装
```bash
# 安装ADB工具
install_platform_tools.bat

# 安装完整开发环境
install_adb.bat
```

### 项目构建
```bash
# 清理并构建
gradlew clean build

# 仅构建调试版本
gradlew assembleDebug

# 仅构建发布版本
gradlew assembleRelease

# 运行测试
gradlew test

# 代码检查
gradlew lint
```

### 设备操作
```bash
# 查看已连接设备
adb devices

# 安装APK
adb install app\build\outputs\apk\debug\app-debug.apk

# 重新安装APK
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 卸载APK
adb uninstall com.smartbasketball.app

# 查看日志
adb logcat

# 清除应用数据
adb shell pm clear com.smartbasketball.app

# 重新启动应用
adb shell am force-stop com.smartbasketball.app
adb shell am start -n com.smartbasketball.app/.MainActivity
```

### 日志过滤
```bash
# 查看应用日志
adb logcat -s SmartBasketball

# 查看错误日志
adb logcat *:E

# 保存日志到文件
adb logcat -d > logcat.txt

# 清除日志缓存
adb logcat -c
```

## 快速操作流程

### 首次设置
1. 运行 `install_platform_tools.bat` 安装ADB
2. 连接设备，启用USB调试
3. 运行 `adb devices` 确认设备连接
4. 运行 `gradlew assembleDebug`

### 日常开发
1. 连接设备
2. 运行 `gradlew assembleDebug`
3. 运行 `adb install -r app\build\outputs\apk\debug\app-debug.apk`

### 调试问题
```bash
# 查看实时日志
adb logcat -s SmartBasketball

# 重启应用
adb shell am force-stop com.smartbasketball.app
adb shell am start -n com.smartbasketball.app/.MainActivity

# 查看崩溃日志
adb logcat *:E | findstr "smartbasketball"

# 检查应用状态
adb shell dumpsys activity activities | findstr "smartbasketball"
```

## 目录结构

```
D:\focusnt\phm\trunk\CODE\phm-basketball\
├── app/                          # 主应用模块
│   ├── build/                    # 构建输出
│   │   ├── outputs/
│   │   │   ├── apk/debug/       # 调试APK
│   │   │   └── apk/release/      # 发布APK
│   │   └── reports/              # 测试报告
│   │       ├── tests/            # 测试报告
│   │       └── lint/             # Lint报告
│   ├── src/main/                 # 源代码
│   └── src/test/                 # 测试代码
├── gradle/                       # Gradle配置
└── build.bat                     # 构建脚本
```

## 文件位置

| 用途 | 路径 |
|------|------|
| 调试APK | `app\build\outputs\apk\debug\app-debug.apk` |
| 发布APK | `app\build\outputs\apk\release\app-release-unsigned.apk` |
| 测试报告 | `app\build\reports\tests\testDebugUnitTest\index.html` |
| Lint报告 | `app\build\reports\lint\lint-results.html` |
| 日志文件 | `logcat.txt` |

## 常见问题

### Q: 设备未识别
```bash
# 检查设备状态
adb devices

# 重启ADB服务器
adb kill-server
adb start-server

# 检查USB模式
adb shell getprop sys.usb.config
```

### Q: 安装失败
```bash
# 卸载旧版本
adb uninstall com.smartbasketball.app

# 清除数据后安装
adb shell pm clear com.smartbasketball.app
adb install app\build\outputs\apk\debug\app-debug.apk
```

### Q: 构建错误
```bash
# 清理构建缓存
gradlew clean
del /Q /S app\build
del /Q /S build

# 重新构建
gradlew assembleDebug
```

### Q: 权限问题
```bash
# Windows: 以管理员身份运行
# Linux/Mac: 使用sudo
sudo adb devices
```

## ADB快捷操作

### 一键安装到设备
```batch
@echo off
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am force-stop com.smartbasketball.app
adb shell am start -n com.smartbasketball.app/.MainActivity
```

### 实时日志监控
```batch
@echo off
adb logcat -s SmartBasketball
```

## 环境变量

| 变量 | 说明 | 示例 |
|------|------|------|
| JAVA_HOME | JDK路径 | `C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9.9-hotspot` |
| ANDROID_HOME | SDK路径 | `C:\Users\用户名\AppData\Local\Android\Sdk` |
| PATH | 添加 | `%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools` |

## 技术支持

如遇到问题，请:
1. 查看Lint报告: `app\build\reports\lint\lint-results.html`
2. 查看测试报告: `app\build\reports\tests\testDebugUnitTest\index.html`
3. 查看构建日志: 在Android Studio中查看Build窗口
4. 提交Issue: 在项目仓库中创建Issue
