# 主界面人脸识别流程开发记录

本文档记录APP主界面人脸识别流程的开发内容，属于正式的业务逻辑开发。

## 开发日期
2026-03-11

---

## 功能概述

在主界面添加抓拍照片弹窗预览功能，用于测试人脸识别流程。后续将对接服务器端API进行人脸识别。

### 业务背景

APP的人脸识别流程：
1. 开机画面 → 登录（已登录可跳过）→ 主界面（摄像头实时抓拍）
2. 主界面检测人脸 → 人脸在抓拍圈内 → 抓拍照片
3. 抓拍照片用于人脸识别（当前先显示弹窗，后续对接服务器API）
4. 识别成功后 → 手势确认（点头开始，摇头退出）→ 投篮游戏
5. 游戏结束 → 上报成绩 → 返回主界面继续识别

---

## 开发内容

### 1. 修改 GameScreens.kt

#### 1.1 添加导入

文件顶部添加以下导入：

```kotlin
import android.graphics.Bitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.delay
```

#### 1.2 MainScreen 中添加状态变量

在 `MainScreen` Composable 函数开头添加：

```kotlin
// ========== 抓拍弹窗相关状态 ==========
var showCapturePreview by remember { mutableStateOf(false) }
var capturedFaceBitmap by remember { mutableStateOf<Bitmap?>(null) }
var captureStartTime by remember { mutableLongStateOf(0L) }
var showCaptureDialogEnabled by remember { mutableStateOf(true) }

// 抓拍阻塞标志：弹窗显示后3秒内阻塞新的抓拍
val isCaptureBlocked = remember(showCapturePreview, captureStartTime) {
    showCapturePreview && (System.currentTimeMillis() - captureStartTime) < 3000L
}

// 定时器：3秒后自动关闭弹窗
LaunchedEffect(showCapturePreview) {
    if (showCapturePreview) {
        delay(3000L)
        showCapturePreview = false
        capturedFaceBitmap = null
        AppLogger.d("MainScreen: 抓拍弹窗自动关闭")
    }
}
```

#### 1.3 裁剪人脸函数

在 `MainScreen` 中添加裁剪函数：

```kotlin
// 裁剪人脸图片函数
private fun cropFace(bitmap: Bitmap, face: Face): Bitmap {
    val boundingBox = face.boundingBox
    return Bitmap.createBitmap(
        bitmap,
        boundingBox.left.coerceAtLeast(0),
        boundingBox.top.coerceAtLeast(0),
        boundingBox.width().coerceAtMost(bitmap.width - boundingBox.left),
        boundingBox.height().coerceAtMost(bitmap.height - boundingBox.top)
    )
}
```

#### 1.4 修改 handleFaceDetected 逻辑

在 `handleFaceDetected` 函数入口处添加阻塞判断，并在人脸在抓拍圈内时先显示弹窗：

```kotlin
val handleFaceDetected: (android.graphics.Bitmap, Face, Int, Int) -> Unit =
    { bitmap, face, bitmapWidth, bitmapHeight ->
        // 抓拍阻塞判断
        if (isCaptureBlocked) {
            AppLogger.d("MainScreen: 抓拍被阻塞中，忽略")
            return@remember
        }

        // ... 原有判断逻辑保持不变 ...

        if (isFaceInCircle(...)) {
            AppLogger.d("MainScreen: face in circle")
            
            // 裁剪人脸图片
            val croppedFace = cropFace(bitmap, face)
            AppLogger.d("MainScreen: 裁剪人脸图片 ${croppedFace.width}x${croppedFace.height}")
            
            if (showCaptureDialogEnabled) {
                // 显示弹窗模式
                capturedFaceBitmap = croppedFace
                showCapturePreview = true
                captureStartTime = System.currentTimeMillis()
                AppLogger.d("MainScreen: 显示抓拍弹窗")
                
                // TODO: 后续对接服务器API时，注释掉上面的弹窗逻辑，改为调用API
                // onFaceDetected(bitmap, face, bitmapWidth, bitmapHeight)
            } else {
                // 直接调用识别（后续对接API）
                onFaceDetected(bitmap, face, bitmapWidth, bitmapHeight)
            }
        }
    }
```

#### 1.5 添加弹窗组件

在 MainScreen 的 Box 容器中，FaceCaptureOverlay 之后添加：

```kotlin
// ========== 抓拍预览弹窗 ==========
if (showCapturePreview && capturedFaceBitmap != null) {
    CapturePreviewDialog(
        bitmap = capturedFaceBitmap!!,
        onDismiss = {
            showCapturePreview = false
            capturedFaceBitmap = null
        }
    )
}
```

#### 1.6 CapturePreviewDialog 组件

在 `GameScreens.kt` 文件末尾添加：

```kotlin
@Composable
fun CapturePreviewDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "抓拍预览",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "抓拍照片",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "识别中...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}
```

---

## 弹窗开关说明

| 变量 | 值 | 说明 |
|------|-----|------|
| `showCaptureDialogEnabled` | `true` | 显示弹窗（测试/开发模式） |
| `showCaptureDialogEnabled` | `false` | 直接调用识别（生产模式） |

---

## 阻塞逻辑流程

```
摄像头检测到人脸
       ↓
检查人脸是否在抓拍圈内
       ↓ (是)
检查 isCaptureBlocked（3秒内是否已抓拍）
       ↓ (否)
裁剪人脸照片 → 显示弹窗
       ↓
3秒内再次检测到人脸 → 直接忽略（阻塞）
       ↓
3秒后弹窗自动消失
       ↓
恢复正常抓拍流程
```

---

## 后续对接服务器API

当用户提供服务器端API接口方法后，需要修改 `handleFaceDetected` 中的逻辑：

1. 将 `showCaptureDialogEnabled` 设为 `false`，或添加服务器API调用
2. 在弹窗显示期间，将裁剪后的人脸图片（`croppedFace`）发送到服务器
3. 根据服务器返回结果决定后续流程

### 示例代码（待实现）

```kotlin
if (showCaptureDialogEnabled) {
    // 显示弹窗模式（当前测试用）
    capturedFaceBitmap = croppedFace
    showCapturePreview = true
    captureStartTime = System.currentTimeMillis()
} else {
    // TODO: 对接服务器API
    viewModel.recognizeFaceOnServer(croppedFace) { result ->
        when (result) {
            is RecognizeResult.Success -> {
                // 识别成功，等待用户点头确认
            }
            is RecognizeResult.Fail -> {
                // 识别失败，重新检测
            }
        }
    }
}
```

### 预期服务器API响应格式

服务器API应返回：
- 用户ID
- 用户姓名
- 用户角色（学生/老师）
- 用户称号

### 识别成功后的流程

1. 服务器返回用户信息
2. UI显示识别成功，等待用户手势确认
3. 用户点头 → 开始投篮游戏
4. 用户摇头 → 清除识别结果，重新检测

---

## 相关代码文件

| 文件 | 作用 |
|------|------|
| `GameScreens.kt` | 主界面UI，包含弹窗和抓拍逻辑 |
| `CameraPreview.kt` | 摄像头预览和人脸检测 |
| `FaceCaptureOverlay.kt` | 人脸抓拍圈UI |
| `GameViewModel.kt` | 业务逻辑，手势识别、游戏流程 |

---

## 调试日志标签

在 logcat 中使用以下标签过滤日志：

```
adb logcat -d | grep "SmartBasketball"
```

关键日志前缀：
- `MainScreen: handleFaceDetected` - 人脸检测回调
- `MainScreen: face in circle` - 人脸在抓拍圈内
- `MainScreen: 裁剪人脸图片` - 人脸裁剪
- `MainScreen: 显示抓拍弹窗` - 弹窗显示
- `MainScreen: 抓拍被阻塞中` - 抓拍被阻塞

---

## 测试用例

### 测试用例1：无人在镜头前

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-001 |
| **用例名称** | 正常开启APP，无人脸场景 |
| **前置条件** | APP未启动，摄像头前无人 |
| **测试步骤** | 1. 启动APP<br>2. 进入主界面<br>3. 观察摄像头画面 |
| **预期结果** | - 摄像头正常开启<br>- 显示抓拍圈<br>- 无抓拍弹窗显示<br>- 日志显示"未检测到人脸" |
| **实际结果** | [待测试] |
| **测试日期** | [待测试] |

---

## 人脸检测模型说明

### 使用的人脸检测方案

APP使用 **Google ML Kit Face Detection** 进行实时人脸检测：

| 项目 | 信息 |
|------|------|
| **框架** | Google ML Kit |
| **模型** | Face Detection (内置预训练模型) |
| **检测模式** | PERFORMANCE_MODE_ACCURATE（精确模式） |
| **关键点检测** | LANDMARK_MODE_ALL（检测所有关键点） |
| **最小人脸尺寸** | 0.15f（人脸占图像高度的15%） |
| **置信度阈值** | 默认0.5 |

### 检测流程

```
摄像头帧 → ML Kit Face Detection → 检测到人脸?
                                      ↓
                                    是 ↓ 否
                                      ↓
                              回调onFaceDetected  回调onFaceLost
                                      ↓
                              检查人脸是否在抓拍圈内
                                      ↓
                              是 → 裁剪人脸 → 显示弹窗
                              否 → 忽略
```

### 与服务器API的关系

- **当前阶段**：ML Kit只负责检测是否有人脸（检测到=有效人脸）
- **后续阶段**：抓拍的照片将发送到服务器API进行身份识别
- **ML Kit作用**：快速判断画面中是否有人脸，无需加载重型模型

---

**更新日期**: 2026-03-11

---

# 当前开发状态 (2026-03-11)

## 最新状态

| 项目 | 状态 |
|------|------|
| **代码编译** | ✅ 成功 |
| **APK位置** | `app/build/outputs/apk/debug/app-debug.apk` |
| **设备连接** | ❌ 待连接（ADB检测不到设备） |
| **测试进度** | 待执行测试用例1 |

---

## 下一步操作

1. 等待用户重启电脑后，重新检查设备连接
2. 连接成功后执行 `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. 用户启动APP
4. 观察logcat日志，验证测试用例1

---

## 测试用例1状态

- **用例编号**: TC-001
- **用例名称**: 正常开启APP，无人脸场景
- **状态**: 待执行
- **预期**: 摄像头开启，显示抓拍圈，无弹窗，日志显示"未检测到人脸"

---

## 编译命令

```bash
# 编译
gradlew.bat assembleDebug

# 安装
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 查看日志
adb logcat -s SmartBasketball
```

---

## 人脸识别完整流程需求文档

### 一、整体流程架构

```
CameraPreview (持续检测人脸)
    ↓
onFaceDetected 回调
    ↓
MainScreen.handleFaceDetected (采帧逻辑)
    ↓
    ├── 状态1: isRecognizing = true (API调用中)
    │       → 直接跳过，不处理
    │
    ├── 状态2: isRecognitionSuccess = true (识别成功)
    │       → 继续采帧 + 检测点头/摇头手势
    │           ├── 点头 → 进入投篮游戏
    │           └── 摇头 → 清空状态
    │
    └── 状态3: isRecognizing = false && isRecognitionSuccess = false
            → 正常采帧流程
                ├── captureStartTime = 0 (新采集开始)
                │   → 采集500ms内最多10帧
                │   → 选最佳帧 → 调用API
                │   → API返回:
                │       ├── simi < 0.8 → 重置captureStartTime，重新采集
                │       └── simi >= 0.8 → isRecognitionSuccess = true
                │
                └── captureStartTime > 0 (采集中)
                    → 继续采集帧
                    → 满足条件后触发API
```

### 二、关键状态变量

| 变量 | 含义 |
|------|------|
| isRecognizing | 正在调用API识别中 |
| isRecognitionSuccess | 识别成功（simi >= 0.8） |
| captureStartTime | 采帧开始时间（0表示未在采集） |
| frameCandidates | 采集的帧列表 |
| bestFrame | 最佳帧 |

### 三、采帧流程逻辑

1. **采帧流程一直存在** - 在人脸识别界面下持续运行
2. **识别成功前**：每500ms采集10帧 → 选最佳帧 → 调用API → 等待响应 → 重复
3. **识别成功后**：
   - 采帧继续运行
   - 但不再触发API调用
   - 改为检测点头/摇头手势

### 四、人脸离开识别区判断

- 每次 handleFaceDetected 调用时都检查：`!isFaceInCircle(...)`
- 如果人脸不在识别区内 → 清空所有状态

### 五、点头/摇头检测逻辑

- **点头**：eulerAngleX（头部左右倾斜）连续2次变化 > 阈值(8)
- **摇头**：eulerAngleY（头部上下倾斜）连续多次大变化

### 六、测试用例

#### 测试用例1：进入识别区圆圈，马上离开
- **步骤**：
  1. 人脸进入识别区圆圈
  2. 立即将人脸移出识别区
- **预期结果**：
  - 无任何反应
  - 识别状态保持初始状态

#### 测试用例2：进入识别区圆圈，完成识别后，离开圆圈
- **步骤**：
  1. 人脸进入识别区圆圈
  2. 完成人脸识别（simi >= 0.8）
  3. 识别成功后，将人脸移出识别区圆圈
- **预期结果**：
  - 识别成功后显示用户信息
  - 人脸离开后清空所有状态
  - 回到初始识别状态

#### 测试用例3：完成识别后，摇头
- **步骤**：
  1. 人脸进入识别区圆圈
  2. 完成人脸识别（simi >= 0.8）
  3. 识别成功后，摇头
- **预期结果**：
  - 识别成功后显示用户信息 + "请点头确认开始游戏"
  - 检测到摇头动作后清空状态
  - 回到初始识别状态

#### 测试用例4：完成识别后，点头
- **步骤**：
  1. 人脸进入识别区圆圈
  2. 完成人脸识别（simi >= 0.8）
  3. 识别成功后，点头
- **预期结果**：
  - 识别成功后显示用户信息 + "请点头确认开始游戏"
  - 检测到点头动作后进入投篮游戏
  - 显示3-2-1倒计时 → 开始投篮

### 七、代码关键点

1. **handleFaceDetected 每次都会被调用**（CameraPreview持续检测）
2. **需要用 rememberUpdatedState 获取最新状态**（解决lambda捕获旧值问题）
3. **采帧流程复用**：识别前后都是采帧，只是触发逻辑不同
4. **点头/摇头检测**：在识别成功后，在采帧流程中检测

### 八、修改计划

1. ✅ 在 MainScreen 中添加 `isRecognitionSuccessState = rememberUpdatedState(uiState.isRecognitionSuccess)`

2. ✅ 在 handleFaceDetected 中：
   - 使用 `isRecognitionSuccessState.value` 获取最新值
   - 识别成功后检测点头/摇头
   - 点头 → 调用 viewModel.startGame()
   - 摇头 → 清空状态

3. ✅ 每次都检查人脸是否在识别区内
