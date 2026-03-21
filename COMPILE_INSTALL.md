# 编译安装测试环境

## 开发环境

| 项目 | 版本/信息 |
|------|----------|
| **开发机器** | Windows |
| **Gradle** | 8.2 |
| **Android Gradle Plugin** | 8.2.0 |
| **Kotlin** | 1.9.10 |
| **KSP** | 1.9.10-1.0.13 |
| **compileSdk** | 34 |
| **targetSdk** | 34 |
| **minSdk** | 26 |
| **Java** | 17 |

## 依赖框架

| 框架 | 版本 |
|------|------|
| Hilt | 2.48.1 |
| AndroidX | 最新 |
| ML Kit Face Detection | 最新 |

## 目标设备

| 项目 | 规格 |
|------|------|
| **CPU** | RK3566 4核 |
| **内存** | 32GB |
| **存储** | 1TB |
| **NPU** | 1T 算力 |
| **系统** | Android 10+ |

## 人脸识别模型

| 模型 | 大小 | 维度 | 类型 |
|------|------|------|------|
| mobileface.tflite | 5MB | 192维 | TFLite |
| w600k_r50.onnx | 174MB | 512维 | ONNX |

## 编译命令

```bash
# 编译Debug APK
.\gradlew.bat assembleDebug

# 安装到设备
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## 测试流程

1. 编译Debug APK: `.\gradlew.bat assembleDebug`
2. 安装APK: `adb install -r app\build\outputs\apk\debug\app-debug.apk`
3. 运行应用，在Logcat中查看人脸识别相关日志

## 日志过滤

```
# 查看人脸识别相关日志
tag:FaceRecognitionManager OR tag:FaceFeatureManager
```
