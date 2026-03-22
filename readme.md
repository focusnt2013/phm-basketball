# 智智慧投篮机系统

## 产品定义

本系统基于传统街机投篮机改造。实现"人脸无感登录 + 手势隔空启动 + 数字化计分以及学校排行榜场景呈现"。支持"固定时间"与"固定球数"双模式，提供接口与客户平台交互。

## 产品要求

1. **网络要求**：支持物联网卡联网
2. **身份登记**：支持接收外部人脸信息登记请求，支持增删改接口
3. **身份识别**：摄像头捕捉人脸 -> 本地库比对 -> 确认身份 -> 开启引导
4. **模式确认**：手势切换模式，计分板显示当前模式（倒计时/定数，默认为定数）
5. **手势启动**：识别右手高举动作 -> 倒计时321 -> 开始供球
6. **数据采集**：实时记录出手数、命中数 -> 计算命中率
7. **云端同步**：游戏结束，触发数据上传至配置平台，若网络不通暂存本地，联网后补传
8. **运动榜单**：游戏结束后，上传数据接口后端平台返回运动榜单链接，呈现到游戏机屏幕上

## 技术架构

### Android客户端技术栈

| 类别 | 技术选型 |
|------|----------|
| 开发语言 | Kotlin 1.9+ |
| UI框架 | Jetpack Compose |
| 架构模式 | MVVM + Clean Architecture |
| 最低Android版本 | Android 8.0 (API 26) |
| 网络框架 | Retrofit + OkHttp |
| 数据库 | Room |
| 本地存储 | DataStore |
| 依赖注入 | Hilt |
| 手势识别 | MediaPipe Hands |
| 人脸识别 | 百度智能云SDK |
| 后台任务 | WorkManager |

### 项目结构

```
app/src/main/kotlin/com/smartbasketball/app/
├── SmartBasketballApp.kt         # Application类
├── domain/model/                  # 数据模型
│   ├── User.kt                    # 用户模型
│   └── GameModels.kt             # 游戏状态模型
├── data/
│   ├── local/                     # Room数据库
│   ├── repository/                # 数据仓库
│   ├── service/                   # 服务层
│   │   ├── FaceRecognitionService.kt
│   │   ├── GestureControlService.kt
│   │   ├── BallCountService.kt
│   │   ├── VoiceService.kt
│   │   ├── WebViewManager.kt
│   │   └── DataSyncService.kt
│   ├── api/                       # 网络API
│   └── preferences/               # 配置管理
├── di/                            # 依赖注入
├── hardware/                      # 硬件层
│   ├── CameraManager.kt
│   ├── HardwareDetector.kt
│   └── UsbPermissionHelper.kt
└── ui/                            # UI层
    ├── MainActivity.kt
    ├── GameViewModel.kt
    └── game/
        ├── GameScreens.kt
        └── viewmodel/
            └── GameViewModel.kt
```

## 硬件配置

### 光电传感器
- **型号**：世宇KL-GY40JMFS1
- **类型**：红外反射式光电传感器
- **输出类型**：NPN
- **工作电压**：10-30V DC

### 信号采集
- **模块**：USB转TTL模块（CH340芯片）
- **波特率**：9600
- **隔离方式**：光耦隔离

### GPIO投篮感应器配置
- **GPIO编号**: 81 (SHOOT_SENSOR_PIN)
- **采集方式**: JNI直连 /sys/class/gpio/
- **轮询间隔**: 20ms
- **防抖时间**: 300ms
- **命中判定窗口**: 500ms

#### 命中判定算法
- 感应器上升沿触发 → 记录T1
- 500ms内再次触发 → 命中
- 超过500ms无触发 → 未命中

#### ROOT权限方案
- **方案A (推荐)**: 系统签名APP，安装为系统应用（/system/app/），待厂商提供platform签名密钥
- **方案B (调试用)**: 运行时通过su命令申请ROOT权限，首次启动需用户授权

## 功能模块

### 1. 用户身份模块
- 人脸识别登录
- 用户信息展示
- 游客模式支持
- 角色区分（学生/老师/游客）

### 2. 手势交互模块
- 右手高举：启动游戏
- 握拳：切换游戏模式
- MediaPipe Hands实时检测

### 3. 进球检测模块
- USB串口通信
- NPN光电传感器信号采集
- 软件去抖处理（默认50ms）
- 进球计数统计

### 4. 游戏核心模块
- 倒计时模式（默认60秒）
- 定数模式（默认20球）
- 实时计分
- 命中率计算

### 5. 数据同步模块
- 本地数据缓存
- 断网自动存储
- 联网自动补传
- WorkManager定时同步

### 6. 榜单展示模块
- 场景榜单（默认显示）
- 游戏结束榜单
- WebView网页加载

### 7. 语音播报模块
- 进球提示
- 模式切换播报
- 成绩播报

## 开发环境

### 必要工具
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Gradle 8.2+
- Android SDK 34

### 构建命令

```bash
# 同步Gradle
./gradlew clean build

# 运行测试
./gradlew test

# 生成APK
./gradlew assembleRelease
```

## 部署步骤

1. 编译APK：`./gradlew assembleRelease`
2. 安装到设备：`adb install app/build/outputs/apk/release/app-release.apk`
3. 配置USB设备权限
4. 配置网络参数

## 后端API接口（待规划）

| 接口 | 说明 |
|------|------|
| /api/user/face/sync | 同步用户人脸信息 |
| /api/game/record/upload | 上传游戏记录 |
| /api/config/leaderboard | 获取榜单配置 |

## 版本历史

### v1.0.0 (2024-)
- 初始版本
- 基础功能实现
- 人脸识别集成
- 手势控制集成
- 进球检测集成
- 数据同步实现