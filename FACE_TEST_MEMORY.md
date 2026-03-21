# 人脸特征一致性测试恢复指南

本文档记录了如何恢复APP中的人脸特征一致性测试功能。

## 测试功能概述

该测试用于验证不同用户的人脸特征相似度是否足够低（能够区分不同人），以及同一用户不同照片的相似度是否足够高。

**测试用户列表**（10人）：
- 陈阳、李榕洲、刘老师、谢君浩、戴熙妍
- 李承凯、李晨浩、许成韬、江逸朋、姜立哲

---

## 恢复步骤

### 步骤1: 恢复 MainActivity.kt

#### 1.1 添加导入语句

在文件顶部添加以下导入：

```kotlin
import com.smartbasketball.app.ui.game.TestFaceImagesScreen
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
```

#### 1.2 添加StateFlow变量

在 `val faceProgress by viewModel.faceProgress.collectAsStateWithLifecycle()` 后添加：

```kotlin
val showTestFaceScreen by viewModel.showTestFaceScreen.collectAsStateWithLifecycle()
val testFaceImages by viewModel.testFaceImages.collectAsStateWithLifecycle()
val testFaceOriginalUrls by viewModel.testFaceOriginalUrls.collectAsStateWithLifecycle()
val testFaceData by viewModel.testFaceData.collectAsStateWithLifecycle()
val similarityMatrix by viewModel.similarityMatrix.collectAsStateWithLifecycle()
val testExtractTimes by viewModel.testExtractTimes.collectAsStateWithLifecycle()
val testLogs by viewModel.testLogs.collectAsStateWithLifecycle()
```

#### 1.3 添加LaunchedEffect

在 `var showLoginResult` 声明之后、Surface之前添加：

```kotlin
// 一致性测试期间刷新图片列表和相似度矩阵
LaunchedEffect(showTestFaceScreen) {
    if (showTestFaceScreen) {
        viewModel.refreshTestFaceImages()
        viewModel.refreshTestFaceOriginalUrls()
        delay(1500)
        viewModel.refreshTestFaceData()
    }
}

// 测试过程中持续刷新（图片逐步生成，相似度逐步计算）
LaunchedEffect(showTestFaceScreen) {
    if (showTestFaceScreen) {
        while (showTestFaceScreen) {
            viewModel.refreshTestFaceImages()
            viewModel.refreshTestFaceData()
            delay(1500)
        }
    }
}
```

#### 1.4 修改Surface内容

将现有的 `GameScreen(...)` 调用改为条件判断：

```kotlin
Surface(
    modifier = Modifier.fillMaxSize(),
    color = LightBackground
) {
    // 直接显示诊断界面，一致性测试完成后再显示主界面
    if (showTestFaceScreen) {
        TestFaceImagesScreen(
            testFaces = testFaceImages,
            testFaceOriginalUrls = testFaceOriginalUrls,
            testFaceData = testFaceData,
            similarityMatrix = similarityMatrix,
            testExtractTimes = testExtractTimes,
            testLogs = testLogs,
            onBack = { viewModel.dismissTestFaceScreen() }
        )
    } else {
        GameScreen(
            viewModel = viewModel,
            gameState = uiState.gameState,
            isLoggedIn = uiState.schoolTitle != null,
            schoolTitle = uiState.schoolTitle,
            userName = uiState.userName,
            userRole = uiState.userRole,
            userTitle = uiState.userTitle,
            gameMode = uiState.gameMode,
            gameSession = uiState.gameSession,
            remainingTime = uiState.remainingTime,
            remainingBalls = uiState.remainingBalls,
            countdownNumber = uiState.countdownNumber,
            lastRecord = uiState.lastRecord,
            startupMessage = uiState.startupMessage,
            syncMessage = uiState.syncMessage,
            validUserCount = uiState.validUserCount,
            cameraReady = uiState.cameraReady,
            showStartupOverlay = uiState.showStartupOverlay,
            showGamePlay = uiState.showGamePlay,
            faceProgress = faceProgress,
            madeBalls = uiState.madeBalls,
            missedBalls = uiState.missedBalls,
            currentAnimation = uiState.currentAnimation,
            onBackToRank = { viewModel.returnToSceneRank() },
            onGestureDetected = { },
            onLoginSuccess = { },
            onLogout = { viewModel.logout() },
            loginResult = showLoginResult,
            onLoginResultShown = { showLoginResult = null },
            onLogin = { schoolId, password ->
                viewModel.login(schoolId, password) { success, message ->
                    showLoginResult = Pair(success, message)
                }
            },
            onFaceDetected = onFaceDetected,
            onFaceLost = onFaceLost,
            onCameraReady = onCameraReady,
            onStartupOverlayHidden = onStartupOverlayHidden,
            onCancelRecognition = onCancelRecognition
        )
    }
}
```

---

### 步骤2: 恢复 GameViewModel.kt

#### 2.1 添加StateFlow变量

在 `_recognitionState` 声明之后添加：

```kotlin
private val _showTestFaceScreen = MutableStateFlow(true)
val showTestFaceScreen: StateFlow<Boolean> = _showTestFaceScreen.asStateFlow()

private val _testLogs = MutableStateFlow("")
val testLogs: StateFlow<String> = _testLogs.asStateFlow()

fun addTestLog(log: String) {
    val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
    _testLogs.value = "$timestamp $log"
}

fun clearTestLogs() {
    _testLogs.value = ""
}
```

#### 2.2 添加observeModelLoadState函数

在 `init` 块中添加对 `observeModelLoadState()` 的调用，并添加该函数：

```kotlin
init {
    initializeApp()
    observeFaceProgress()
    observeRecognitionState()
    observeModelLoadState()  // 添加这行
}

private fun observeModelLoadState() {
    viewModelScope.launch {
        faceRecognitionManager.modelLoadState.collect { state ->
            if (state.isLoading) {
                addTestLog("模型加载中...")
            }
            if (state.isLoaded) {
                addTestLog("模型加载成功!")
            }
            if (state.error != null) {
                addTestLog("模型加载失败: ${state.error}")
            }
        }
    }
    viewModelScope.launch {
        faceRecognitionManager.testStatus.collect { status ->
            if (status.isNotEmpty()) {
                addTestLog(status)
            }
        }
    }
}
```

#### 2.3 修改initializeApp函数

将 `initializeApp` 中的内容改为：

```kotlin
private fun initializeApp() {
    AppLogger.d("========== GameViewModel: initializeApp 开始执行 ==========")
    _uiState.update { it.copy(gameState = GameState.STARTUP, cameraReady = false, showStartupOverlay = true) }
    
    viewModelScope.launch {
        loadValidUserCount()
        faceRecognitionManager.loadUsersToCache()
        
        // 延迟3秒后执行特征一致性测试（独立于loadUsersToCache）
        delay(3000)
        AppLogger.d("========== GameViewModel: 准备启动特征一致性测试 ==========")
        faceRecognitionManager.runFeatureConsistencyTestAsync()
        _showTestFaceScreen.value = true
        
        checkLoginStatus()
    }
}
```

#### 2.4 添加测试相关函数

保留以下函数（如果存在则无需添加）：

```kotlin
// 获取测试人脸图片列表（裁剪后的人脸）
fun getTestFaceImages(): List<Pair<String, String>> {
    return faceFeatureManager.getTestFaceImages()
testFaceImages =}

private val _ MutableStateFlow<List<Pair<String, String>>>(emptyList())
val testFaceImages: StateFlow<List<Pair<String, String>>> = _testFaceImages.asStateFlow()

fun refreshTestFaceImages() {
    _testFaceImages.value = faceFeatureManager.getTestFaceImages()
}

// 获取测试用户的原图URL列表
suspend fun getTestFaceOriginalUrls(): List<Pair<String, String>> {
    val testUserNames = listOf(
        "陈阳", "李榕洲", "刘老师", "谢君浩", "戴熙妍",
        "李承凯", "李晨浩", "许成韬", "江逸朋", "姜立哲"
    )
    val result = mutableListOf<Pair<String, String>>()
    for (name in testUserNames) {
        val user = userDao.getUserByName(name)
        if (user?.faceUrl != null) {
            result.add(name to user.faceUrl)
        }
    }
    return result
}

private val _testFaceOriginalUrls = MutableStateFlow<List<Pair<String, String>>>(emptyList())
val testFaceOriginalUrls: StateFlow<List<Pair<String, String>>> = _testFaceOriginalUrls.asStateFlow()

fun refreshTestFaceOriginalUrls() {
    viewModelScope.launch {
        _testFaceOriginalUrls.value = getTestFaceOriginalUrls()
    }
}

// 关闭测试人脸图片界面
fun dismissTestFaceScreen() {
    _showTestFaceScreen.value = false
}

// 获取测试人脸特征数据（用于显示相似度）
fun getTestFaceData(): List<Triple<String, FloatArray, Float>> {
    return faceRecognitionManager.getTestFaceData()
}

private val _testFaceData = MutableStateFlow<List<Triple<String, FloatArray, Float>>>(emptyList())
val testFaceData: StateFlow<List<Triple<String, FloatArray, Float>>> = _testFaceData.asStateFlow()

// 相似度矩阵
private val _similarityMatrix = MutableStateFlow<List<List<Float>>>(emptyList())
val similarityMatrix: StateFlow<List<List<Float>>> = _similarityMatrix.asStateFlow()

// 特征提取时间
private val _testExtractTimes = MutableStateFlow<Map<String, Long>>(emptyMap())
val testExtractTimes: StateFlow<Map<String, Long>> = _testExtractTimes.asStateFlow()

fun refreshTestFaceData() {
    _testFaceData.value = faceRecognitionManager.getTestFaceData()
    _similarityMatrix.value = faceRecognitionManager.getSimilarityMatrix()
    _testExtractTimes.value = faceRecognitionManager.getTestExtractTimes()
}

// 兼容方法
fun getTestFaces(): List<Pair<String, String>> = getTestFaceImages()

// 显示/隐藏测试人脸调试界面
private val _showTestFaces = MutableStateFlow(false)
val showTestFaces: StateFlow<Boolean> = _showTestFaces.asStateFlow()

fun toggleTestFaces() {
    _showTestFaces.value = !_showTestFaces.value
}
```

---

## 测试用户数据要求

测试需要数据库中存在以下10个用户的头像URL：

| 姓名 | 角色 |
|------|------|
| 陈阳 | 学生 |
| 李榕洲 | 学生 |
| 刘老师 | 老师 |
| 谢君浩 | 学生 |
| 戴熙妍 | 学生 |
| 李承凯 | 学生 |
| 李晨浩 | 学生 |
| 许成韬 | 学生 |
| 江逸朋 | 学生 |
| 姜立哲 | 学生 |

这些用户需要在同步人脸数据时从服务器获取。

---

## 预期测试结果

### 正常情况（特征有效）
- **同一人**：相似度应 >80%
- **不同人**：相似度应 <50%

### 异常情况（特征无效）
- 所有用户之间相似度都很高（>70%）→ 说明特征提取有问题

---

## 关键类和方法

| 类 | 方法 | 作用 |
|---|------|------|
| `FaceRecognitionManager` | `runFeatureConsistencyTestAsync()` | 启动异步特征一致性测试 |
| `FaceRecognitionManager` | `getTestFaceData()` | 获取测试人脸特征数据 |
| `FaceRecognitionManager` | `getSimilarityMatrix()` | 获取相似度矩阵 |
| `FaceRecognitionManager` | `getTestExtractTimes()` | 获取特征提取耗时 |
| `FaceRecognitionManager` | `testStatus` | 测试状态Flow |
| `FaceEmbeddingManager` | `loadState` | 模型加载状态 |
| `FaceFeatureManager` | `extractFaceFeatureFromUrlWithName()` | 下载图片并提取特征 |

---

## 调试日志标签

在 logcat 中使用以下标签过滤日志：

```
adb logcat -d | grep "SmartBasketball.*特征"
```

关键日志前缀：
- `ScrfdDetector` - SCRFD人脸检测
- `extractFaceFeature` - 特征提取
- `generateEmbedding` - Embedding生成
- `相似度:` - 相似度计算
- `特征测试:` - 一致性测试进度

---

**文档版本**: v1.0
**创建日期**: 2026-03-11
**最后更新**: 2026-03-11
