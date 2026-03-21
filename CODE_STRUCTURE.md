# 智智慧投篮机 - 代码结构文档

## 1. 项目概览

| 项目 | 说明 |
|------|------|
| 包名 | com.smartbasketball.app |
| 架构 | MVVM + Clean Architecture |
| UI框架 | Jetpack Compose |
| 依赖注入 | Hilt |
| 数据库 | Room |
| 最小SDK | API 26 (Android 8.0) |
| 目标SDK | API 34 (Android 14) |

## 2. 目录结构

```
app/src/main/kotlin/com/smartbasketball/app/
├── SmartBasketballApp.kt          # Application入口
├── receiver/
│   └── BootCompletedReceiver.kt  # 开机启动接收器
├── domain/
│   └── model/
│       ├── GameModels.kt          # 游戏状态、模式、会话
│       └── User.kt               # 用户模型、角色
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt        # Room数据库
│   │   ├── Dao.kt               # 数据访问对象
│   │   └── Database.kt           # 数据库实体
│   ├── api/
│   │   ├── ApiClient.kt         # HTTP客户端
│   │   ├── ApiModels.kt         # API数据模型
│   │   └── BasketballApiService.kt  # API服务接口
│   ├── repository/
│   │   ├── GameRecordRepository.kt   # 游戏记录仓库
│   │   └── UserRepository.kt        # 用户仓库
│   ├── service/
│   │   ├── BallCountService.kt      # 进球检测服务接口
│   │   ├── DataSyncService.kt       # 数据同步服务接口
│   │   ├── FaceDirectoryLoader.kt   # 人脸目录加载器
│   │   ├── FaceRecognitionService.kt # 人脸识别服务接口
│   │   ├── GestureControlService.kt  # 手势控制服务接口
│   │   ├── VoiceService.kt          # 语音播报服务
│   │   └── WebViewManager.kt        # WebView管理器
│   └── service/impl/
│       ├── BallCountServiceImpl.kt   # 进球检测实现
│       ├── DataSyncServiceImpl.kt    # 数据同步实现
│       ├── FaceRecognitionServiceImpl.kt  # 人脸识别实现
│       └── GestureControlServiceImpl.kt   # 手势控制实现
├── di/
│   ├── DatabaseModule.kt       # 数据库DI模块
│   ├── NetworkModule.kt        # 网络DI模块
│   ├── RepositoryModule.kt     # 仓库DI模块
│   └── ServiceModule.kt        # 服务DI模块
├── hardware/
│   ├── CameraManager.kt        # 摄像头管理
│   ├── HardwareDetector.kt     # 硬件检测
│   ├── UsbPermissionHelper.kt  # USB权限辅助
│   ├── service/
│   │   └── BallCountServiceImpl.kt  # 进球计数硬件服务
│   └── ui/
│       └── CameraPreview.kt   # 摄像头预览组件
├── ui/
│   ├── MainActivity.kt        # 主Activity
│   ├── GameViewModel.kt       # 游戏ViewModel (旧版)
│   ├── SettingsScreen.kt     # 设置页面
│   ├── SettingsViewModel.kt  # 设置ViewModel
│   ├── game/
│   │   ├── GameScreens.kt    # 游戏界面Compose
│   │   └── viewmodel/
│   │       └── GameViewModel.kt  # 游戏ViewModel (新版)
│   ├── components/
│   │   ├── Dialogs.kt        # 对话框组件
│   │   ├── GameComponents.kt # 游戏通用组件
│   │   └── LeaderboardScreen.kt  # 排行榜页面
│   ├── navigation/
│   │   └── Navigation.kt     # 导航配置
│   └── theme/
│       ├── Color.kt          # 颜色定义
│       └── Theme.kt          # 主题配置
└── util/
    ├── AppLogger.kt          # 日志工具
    ├── Constants.kt         # 常量定义
    ├── Extensions.kt        # 扩展函数
    └── NetworkMonitor.kt    # 网络监控
```

## 3. 核心模块说明

### 3.1 领域层 (domain/model)

| 文件 | 说明 |
|------|------|
| `GameModels.kt` | 游戏状态机 (STARTUP → SCENE_RANK → FACE_DETECT → STANDBY → GAME_STARTING → GAME_PLAYING → GAME_ENDED → LEADERBOARD) |
| `User.kt` | 用户模型，包含 id, name, role (STUDENT/TEACHER/VISITOR), faceFeature |

### 3.2 数据层 (data)

| 模块 | 说明 |
|------|------|
| **UserRepository** | 用户数据仓库，管理人脸特征数据 |
| **GameRecordRepository** | 游戏记录仓库，管理投篮成绩 |
| **FaceRecognitionService** | 人脸识别服务 (百度SDK) |
| **BallCountService** | 进球检测服务 (光电传感器) |
| **GestureControlService** | 手势控制服务 (点头/摇头检测) |
| **DataSyncService** | 数据同步服务 (待实现API对接) |

### 3.3 硬件层 (hardware)

| 模块 | 说明 |
|------|------|
| **CameraManager** | USB摄像头管理 |
| **BallCountServiceImpl** | 光电传感器串口通信 (9600波特率) |
| **HardwareDetector** | USB设备检测 |

### 3.4 UI层 (ui)

| 界面 | 状态 |
|------|------|
| **SceneRankContent** | 排行榜页面 |
| **FaceDetectContent** | 人脸识别页面 |
| **StandbyContent** | 待机页面 (显示用户信息、游戏模式) |
| **CountdownContent** | 倒计时页面 |
| **GamePlayingContent** | 游戏进行中页面 |
| **GameEndContent** | 游戏结束页面 |
| **LeaderboardContent** | 排行榜页面 |

## 4. 游戏流程

```
+-----------+
|   STARTUP |  <- 初始化系统、加载人脸
+-----+-----+
      |
      v
+-----------+
| SCENE_RANK|  <- 排行榜，等待人脸识别
+-----+-----+
      | (检测到人脸)
      v
+-----------+
| FACE_DETECT| <- 人脸识别中
+-----+-----+
      | (识别成功/失败)
      v
+-----------+
|   STANDBY |  <- 确认用户，显示模式选择
+-----+-----+
      | (点头确认开始)
      v
+-----------+
|GAME_STARTING| <- 3-2-1倒计时
+-----+-----+
      |
      v
+-----------+
|GAME_PLAYING | <- 游戏进行中 (进球检测)
+-----+-----+
      | (时间到/球用完)
      v
+-----------+
| GAME_ENDED |  <- 显示成绩
+-----+-----+
      |
      v
+-----------+
| LEADERBOARD| <- 排行榜
+-----+-----+
      |
      v
  (返回SCENE_RANK)
```

## 5. 硬件接口

| 设备 | 接口 | 参数 |
|------|------|------|
| USB摄像头 | UVC | 1080P |
| 光电传感器 | USB转TTL串口 | 9600波特率, NPN输出 |
| HDMI显示 | 即插即用 | - |

## 6. 待实现功能

| 功能 | 状态 | 说明 |
|------|------|------|
| 点头/摇头确认开始 | 待开发 | 使用帧差法检测头部动作 |
| 光电传感器进球检测 | 待开发 | 串口读取NPN信号，防抖50ms |
| API数据同步 | 待开发 | 等待API_SPEC.md |

## 7. 人脸数据格式

人脸数据存储在 `faces/` 目录，包含JSON配置文件：

```json
{
    "_id": "68e6357ab9fad63a2f99f00e",
    "name": "陈欣钰",
    "face": "https://example.com/photo.jpg",
    "class_label": "初一1班",
    "gender": "女"
}
```

- 目录结构: `faces/{role}/{_id}.json`
- role: students, teachers, operators
- 照片文件: `{_id}.jpg`
