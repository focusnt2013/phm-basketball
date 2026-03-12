package com.smartbasketball.app.ui.game

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import com.smartbasketball.app.R
import com.smartbasketball.app.ui.theme.BasketballOrange
import com.smartbasketball.app.ui.theme.SuccessGreen
import com.smartbasketball.app.util.AppLogger
import com.smartbasketball.app.util.FaceProgress
import com.smartbasketball.app.util.RecognitionState
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.math.sqrt

val LightBackground = Color(0xFF1A1A1A)
val DarkText = Color(0xFFFFFFFF)
val GrayText = Color(0xFFAAAAAA)

@Composable
fun GameScreen(
    viewModel: com.smartbasketball.app.ui.game.viewmodel.GameViewModel,
    gameState: com.smartbasketball.app.domain.model.GameState,
    isLoggedIn: Boolean = false,
    schoolTitle: String? = null,
    userName: String?,
    userRole: String?,
    userTitle: String? = null,
    gameMode: com.smartbasketball.app.domain.model.GameMode,
    gameSession: com.smartbasketball.app.domain.model.GameSession?,
    remainingTime: Int?,
    remainingBalls: Int?,
    countdownNumber: Int?,
    lastRecord: com.smartbasketball.app.domain.model.GameRecord?,
    startupMessage: String = "正在初始化系统...",
    syncMessage: String = "",
    validUserCount: Int = 0,
    cameraReady: Boolean = false,
    showStartupOverlay: Boolean = false,
    showGamePlay: Boolean = false,
    faceProgress: FaceProgress? = null,
    madeBalls: Int = 0,
    missedBalls: Int = 0,
    currentAnimation: com.smartbasketball.app.ui.game.viewmodel.ShotAnimation? = null,
    onBackToRank: () -> Unit,
    onGestureDetected: (com.smartbasketball.app.data.service.GestureType) -> Unit,
    onLoginSuccess: () -> Unit,
    onLogout: () -> Unit = {},
    loginResult: Pair<Boolean, String>? = null,
    onLoginResultShown: () -> Unit = {},
    onLogin: (String, String) -> Unit = { _, _ -> },
    onFaceDetected: (android.graphics.Bitmap, com.google.mlkit.vision.face.Face, Int, Int) -> Unit = { _, _, _, _ -> },
    onFaceLost: () -> Unit = {},
    onCameraReady: () -> Unit = {},
    onStartupOverlayHidden: () -> Unit = {},
    onCancelRecognition: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        when {
            showGamePlay || gameState == com.smartbasketball.app.domain.model.GameState.GAME_PLAYING -> GamePlayScreen(
                countdownNumber = countdownNumber,
                remainingTime = remainingTime,
                madeBalls = madeBalls,
                missedBalls = missedBalls,
                userName = userName,
                userRole = userRole ?: "",
                userTitle = userTitle,
                currentAnimation = currentAnimation
            )
            gameState == com.smartbasketball.app.domain.model.GameState.LOGIN -> LoginScreen(
                onLogin = onLogin,
                loginResult = loginResult,
                onLoginResultShown = onLoginResultShown
            )
            else -> MainScreen(
                viewModel = viewModel,
                schoolTitle = schoolTitle,
                onLogout = onLogout,
                syncMessage = syncMessage,
                validUserCount = validUserCount,
                faceProgress = faceProgress,
                onFaceDetected = onFaceDetected,
                onFaceLost = onFaceLost,
                onCameraReady = onCameraReady,
                onStartupOverlayHidden = onStartupOverlayHidden,
                onCancelRecognition = onCancelRecognition,
                countdownNumber = countdownNumber,
                gameState = gameState,
                remainingTime = remainingTime,
                madeBalls = madeBalls,
                missedBalls = missedBalls
            )
        }
    }
}

@Composable
fun StartupScreenOverlay(
    visible: Boolean = true,
    onReady: () -> Unit = {}
) {
    AppLogger.d("StartupScreenOverlay: composable called, visible=$visible")
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (visible) Color.White else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        if (visible) {
            AsyncImage(
                model = "https://phmdfs.focusnt.com/group1/M00/03/68/rBnDrWfAxTaAXJS5AADdn5F_lFc018.png",
                contentDescription = "Logo",
                modifier = Modifier
                    .size(200.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    loginResult: Pair<Boolean, String>?,
    onLoginResultShown: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var schoolId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var inputKey by remember { mutableIntStateOf(0) }
    
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(step) {
        inputKey++
        keyboardController?.show()
    }

    LaunchedEffect(schoolId) {
        if (schoolId.length == 6 && step == 1) {
            step = 2
        }
    }

    LaunchedEffect(loginResult) {
        if (loginResult != null) {
            isLoading = false
            if (!loginResult.first) {
                errorMessage = loginResult.second
                step = 1
                schoolId = ""
                password = ""
            }
        }
    }

    LaunchedEffect(password) {
        if (password.length == 6 && step == 2 && !isLoading && errorMessage == null) {
            isLoading = true
            errorMessage = null
            onLogin(schoolId, password)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(LightBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        // 上一步按钮 - 仅在密码输入界面显示
        if (step == 2) {
            TextButton(
                onClick = { 
                    step = 1
                    schoolId = ""
                    password = ""
                    errorMessage = null
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Text("← 上一步", fontSize = 16.sp, color = GrayText)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 120.dp)
        ) {
            Text(
                text = "学校注册/登录",
                fontSize = 36.sp,
                color = DarkText,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (step == 1) {
                SixDigitInputBox(
                    key = inputKey,
                    value = schoolId,
                    onValueChange = { newValue ->
                        errorMessage = null
                        if (newValue.length <= schoolId.length || (newValue.length <= 6 && newValue.all { c -> c.isLetterOrDigit() })) {
                            schoolId = newValue.uppercase()
                        }
                    },
                    isPassword = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "请输入学校ID",
                    fontSize = 18.sp,
                    color = GrayText
                )

                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error,
                        fontSize = 16.sp,
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                SixDigitInputBox(
                    key = inputKey,
                    value = password,
                    onValueChange = { newValue ->
                        errorMessage = null
                        if (newValue.length <= password.length || (newValue.length <= 6 && newValue.all { c -> c.isDigit() })) {
                            password = newValue
                        }
                    },
                    isPassword = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "请输入登录密码",
                    fontSize = 18.sp,
                    color = GrayText
                )
            }
        }

        loginResult?.let { (isSuccess, resultMessage) ->
            if (isSuccess) {
                AlertDialog(
                    onDismissRequest = { onLoginResultShown() },
                    title = {
                        Text(
                            text = "登录成功",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = resultMessage,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { onLoginResultShown() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("确定")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SixDigitInputBox(
    key: Int = 0,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(key) {
        focusRequester.requestFocus()
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(60.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in 0 until 6) {
                val digit = value.getOrNull(i)
                
                Box(
                    modifier = Modifier
                        .width(45.dp)
                        .height(56.dp)
                        .background(
                            color = Color(0xFF2A2A2A),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (i < value.length) BasketballOrange else Color.Gray,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (digit != null) {
                            if (isPassword) "●" else digit.toString()
                        } else "",
                        fontSize = 28.sp,
                        color = DarkText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .focusRequester(focusRequester),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 0.sp,
                color = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.NumberPassword else KeyboardType.Ascii
            ),
            cursorBrush = Brush.verticalGradient(
                colors = listOf(BasketballOrange, BasketballOrange)
            ),
            decorationBox = { innerTextField ->
                innerTextField()
            }
        )
    }
}

private fun isFaceInCircle(face: Face, bitmapWidth: Int, bitmapHeight: Int, screenWidth: Int, screenHeight: Int): Boolean {
    val faceCenterX = face.boundingBox.centerX().toFloat()
    val faceCenterY = face.boundingBox.centerY().toFloat()
    
    val scaleX = screenWidth.toFloat() / bitmapWidth
    val scaleY = screenHeight.toFloat() / bitmapHeight
    
    val scaledFaceCenterX = faceCenterX * scaleX
    val scaledFaceCenterY = faceCenterY * scaleY
    
    val circleCenterX = screenWidth / 2f
    val circleCenterY = screenHeight / 2f
    val circleRadius = screenHeight * 0.375f
    
    val distance = sqrt((scaledFaceCenterX - circleCenterX).pow(2) + (scaledFaceCenterY - circleCenterY).pow(2))
    
    AppLogger.d("isFaceInCircle: faceCenter=($faceCenterX, $faceCenterY), bitmap=($bitmapWidth, $bitmapHeight), screen=($screenWidth, $screenHeight)")
    AppLogger.d("isFaceInCircle: scaledFaceCenter=($scaledFaceCenterX, $scaledFaceCenterY), circleCenter=($circleCenterX, $circleCenterY), radius=$circleRadius, distance=$distance")
    
    return distance <= circleRadius * 0.9f
}

@Composable
fun MainScreen(
    viewModel: com.smartbasketball.app.ui.game.viewmodel.GameViewModel,
    schoolTitle: String?,
    onLogout: () -> Unit,
    syncMessage: String = "",
    validUserCount: Int = 0,
    faceProgress: FaceProgress? = null,
    onFaceDetected: (android.graphics.Bitmap, com.google.mlkit.vision.face.Face, Int, Int) -> Unit = { _, _, _, _ -> },
    onFaceLost: () -> Unit = {},
    onCameraReady: () -> Unit = {},
    onStartupOverlayHidden: () -> Unit = {},
    onCancelRecognition: () -> Unit = {},
    countdownNumber: Int? = null,
    gameState: com.smartbasketball.app.domain.model.GameState = com.smartbasketball.app.domain.model.GameState.STARTUP,
    remainingTime: Int? = null,
    madeBalls: Int = 0,
    missedBalls: Int = 0
) {
    var screenSize by remember { mutableStateOf(Pair(0, 0)) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val currentGameState = uiState.gameState
    val showStartupOverlayState = rememberUpdatedState(uiState.showStartupOverlay)
    val isRecognitionSuccessState = rememberUpdatedState(uiState.isRecognitionSuccess)
    
    // 点头/摇头检测相关变量
    var lastHeadAngleX by remember { mutableFloatStateOf(0f) }
    var lastHeadAngleY by remember { mutableFloatStateOf(0f) }
    var lastGestureTime by remember { mutableLongStateOf(0L) }
    var nodCount by remember { mutableIntStateOf(0) }
    var shakeCount by remember { mutableIntStateOf(0) }
    val nodThreshold = 8f
    
    // ========== 识别状态 ==========
    // 识别进度从GameViewModel获取
    val isRecognizing = uiState.isRecognizing
    val recognitionProgress = uiState.recognitionProgress
    val isRecognitionSuccess = uiState.isRecognitionSuccess
    val isRecognitionFailed = uiState.isRecognitionFailed
    val recognizedUserName = uiState.recognizedUserName
    val recognizedUserRole = uiState.recognizedUserRole
    val recognizedUserTitle = uiState.recognizedUserTitle
    val recognitionError = uiState.recognitionError
    
    // 进度条颜色：成功=青色，失败=红色
    val progressColor = when {
        isRecognitionSuccess -> Color(0xFF00FFFF)  // 青色
        isRecognitionFailed -> Color(0xFFFF0000)   // 红色
        isRecognizing -> Color(0xFF00FFFF)         // 青色（识别中）
        else -> Color(0xFF00FFFF)                 // 默认青色
    }
    
    // 数据类：帧候选（必须在使用前定义）
    data class FrameCandidate(
        val bitmap: Bitmap,
        val face: Face,
        val qualityScore: Float,
        val originalWidth: Int,
        val originalHeight: Int
    )
    
    // 多帧采集状态
    var captureStartTime by remember { mutableLongStateOf(0L) }
    var frameCandidates by remember { mutableStateOf(listOf<FrameCandidate>()) }
    var bestFrame by remember { mutableStateOf<FrameCandidate?>(null) }
    var captureTriggered by remember { mutableStateOf(false) }  // 防止重复触发识别
    
    // 采集参数
    val CAPTURE_DURATION_MS = 1000L  // 采集窗口1秒
    val MIN_CAPTURE_TIME_MS = 500L    // 最小采集时间0.5秒
    val FRAME_INTERVAL_MS = 50L       // 50ms一帧
    val MAX_FRAME_COUNT = 20          // 最多采集20帧
    
    // 裁剪人脸图片函数（只裁剪人脸区域，扩大20%边距）
    fun cropFaceWithMargin(bitmap: Bitmap, face: Face): Bitmap {
        val boundingBox = face.boundingBox
        
        // 计算扩大20%边距后的边界
        val width = boundingBox.width()
        val height = boundingBox.height()
        val marginX = (width * 0.2f / 2).toInt()
        val marginY = (height * 0.2f / 2).toInt()
        
        var left = (boundingBox.left - marginX).coerceAtLeast(0)
        var top = (boundingBox.top - marginY).coerceAtLeast(0)
        var right = (boundingBox.right + marginX).coerceAtMost(bitmap.width)
        var bottom = (boundingBox.bottom + marginY).coerceAtMost(bitmap.height)
        
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }
    
    // 计算帧质量分数
    fun calculateFrameQuality(face: Face, bitmapWidth: Int, bitmapHeight: Int): Float {
        // 1. 人脸面积占比（越大越清晰）
        val faceArea = face.boundingBox.width() * face.boundingBox.height()
        val screenArea = bitmapWidth * bitmapHeight
        val sizeScore = (faceArea.toFloat() / screenArea).coerceIn(0f, 1f)
        
        // 2. 居中度（越居中越好）
        val faceCenterX = (face.boundingBox.left + face.boundingBox.right) / 2f
        val faceCenterY = (face.boundingBox.top + face.boundingBox.bottom) / 2f
        val screenCenterX = bitmapWidth / 2f
        val screenCenterY = bitmapHeight / 2f
        val distance = kotlin.math.sqrt(
            (faceCenterX - screenCenterX) * (faceCenterX - screenCenterX) +
            (faceCenterY - screenCenterY) * (faceCenterY - screenCenterY)
        )
        val maxDistance = kotlin.math.sqrt(screenCenterX * screenCenterX + screenCenterY * screenCenterY)
        val centerScore = 1f - (distance / maxDistance)
        
        // 3. 置信度（使用人脸框的置信度，如果没有则用默认0.7）
        val confidenceScore = 0.7f
        
        // 综合分数：面积30% + 居中30% + 置信度40%
        return sizeScore * 0.3f + centerScore * 0.3f + confidenceScore * 0.4f
    }
    
    // 检查人脸是否在抓拍圈内（简化判断）
    fun isFaceInCircle(face: Face, bitmapWidth: Int, bitmapHeight: Int, screenWidth: Int, screenHeight: Int): Boolean {
        val boundingBox = face.boundingBox
        val faceCenterX = (boundingBox.left + boundingBox.right) / 2f
        val faceCenterY = (boundingBox.top + boundingBox.bottom) / 2f
        
        val scaleX = screenWidth.toFloat() / bitmapWidth
        val scaleY = screenHeight.toFloat() / bitmapHeight
        val scaledFaceCenterX = faceCenterX * scaleX
        val scaledFaceCenterY = faceCenterY * scaleY
        
        val circleCenterX = screenWidth / 2f
        val circleCenterY = screenHeight / 2f
        val radius = screenHeight / 2.67f
        
        val distance = kotlin.math.sqrt(
            (scaledFaceCenterX - circleCenterX) * (scaledFaceCenterX - circleCenterX) +
            (scaledFaceCenterY - circleCenterY) * (scaledFaceCenterY - circleCenterY)
        )
        
        return distance <= radius
    }
    
    // 采集最佳帧
    fun captureBestFrame() {
        if (frameCandidates.isNotEmpty()) {
            bestFrame = frameCandidates.maxByOrNull { it.qualityScore }
            AppLogger.d("MainScreen: 采集完成，最佳帧质量=${bestFrame?.qualityScore}")
        }
    }
    
    // 检测点头/摇头手势
    fun detectGesture(face: Face) {
        val currentTime = System.currentTimeMillis()
        val headAngleX = face.headEulerAngleX
        val headAngleY = face.headEulerAngleY
        
        val deltaX = headAngleX - lastHeadAngleX
        val deltaY = headAngleY - lastHeadAngleY
        
        // 忽略时间过短的情况
        if (currentTime - lastGestureTime < 200) {
            return
        }
        
        AppLogger.d("MainScreen: detectGesture - headAngleX=$headAngleX, headAngleY=$headAngleY, deltaX=$deltaX, deltaY=$deltaY")
        
        lastHeadAngleX = headAngleX
        lastHeadAngleY = headAngleY
        lastGestureTime = currentTime
        
        // 检测点头（头部左右倾斜变化）
        if (kotlin.math.abs(deltaX) > nodThreshold) {
            nodCount++
            if (nodCount >= 2) {
                AppLogger.d("========== 检测到点头动作 ==========")
                nodCount = 0
                shakeCount = 0
                // 点头确认，开始游戏
                viewModel.startGame()
            }
        } else {
            nodCount = maxOf(0, nodCount - 1)
        }
        
        // 检测摇头（头部上下倾斜变化）
        if (kotlin.math.abs(deltaY) > nodThreshold) {
            shakeCount++
            if (shakeCount >= 3) {
                AppLogger.d("========== 检测到摇头动作，清空状态 ==========")
                shakeCount = 0
                nodCount = 0
                // 摇头，清空识别状态
                viewModel.clearRecognitionState()
                captureStartTime = 0L
                frameCandidates = emptyList()
                bestFrame = null
                captureTriggered = false
            }
        } else {
            shakeCount = maxOf(0, shakeCount - 1)
        }
    }
    
    val handleFaceDetected: (android.graphics.Bitmap, Face, Int, Int) -> Unit =
        handleFaceDetectedLabel@{ bitmap, face, bitmapWidth, bitmapHeight ->
            // 如果在投篮游戏中，不处理
            if (currentGameState == com.smartbasketball.app.domain.model.GameState.GAME_PLAYING ||
                currentGameState == com.smartbasketball.app.domain.model.GameState.GAME_COUNTDOWN) {
                return@handleFaceDetectedLabel
            }
            
            // 获取最新状态
            val latestIsRecognizing = uiState.isRecognizing
            val latestIsRecognitionSuccess = isRecognitionSuccessState.value
            
            // 如果正在识别中，跳过
            if (latestIsRecognizing) {
                AppLogger.d("MainScreen: 识别中，跳过")
                return@handleFaceDetectedLabel
            }
            
            // 检查屏幕尺寸
            if (screenSize.first <= 0 || screenSize.second <= 0) {
                AppLogger.d("MainScreen: screenSize not ready: $screenSize")
                return@handleFaceDetectedLabel
            }
            
            // 检查人脸是否在识别区内
            if (!isFaceInCircle(face, bitmapWidth, bitmapHeight, screenSize.first, screenSize.second)) {
                AppLogger.d("MainScreen: 人脸不在识别区域内")
                // 人脸移出识别区，清空识别状态
                viewModel.clearRecognitionState()
                captureStartTime = 0L
                frameCandidates = emptyList()
                bestFrame = null
                captureTriggered = false
                nodCount = 0
                shakeCount = 0
                return@handleFaceDetectedLabel
            }
            
            // 如果已识别成功，检测点头/摇头
            if (latestIsRecognitionSuccess) {
                AppLogger.d("MainScreen: 已识别成功，检测手势")
                detectGesture(face)
                return@handleFaceDetectedLabel
            }
            
            // 未识别/识别失败，继续采帧流程
            val showStartupOverlay = showStartupOverlayState.value
            AppLogger.d("MainScreen: handleFaceDetected called, showStartupOverlay=$showStartupOverlay")

            // 检测到人脸时，自动关闭遮罩
            if (showStartupOverlay) {
                AppLogger.d("MainScreen: 检测到人脸，自动关闭遮罩")
                onStartupOverlayHidden()
            }

            // 采帧流程
            val currentTime = System.currentTimeMillis()
            
            if (captureStartTime == 0L) {
                // 开始新的采集窗口
                captureStartTime = currentTime
                frameCandidates = emptyList()
                bestFrame = null
                captureTriggered = false
                AppLogger.d("MainScreen: 开始采集，captureStartTime=$captureStartTime")
            }
            
            // 采集帧
            val elapsed = currentTime - captureStartTime
            if (elapsed < CAPTURE_DURATION_MS && frameCandidates.size < MAX_FRAME_COUNT) {
                val qualityScore = calculateFrameQuality(face, bitmapWidth, bitmapHeight)
                val candidate = FrameCandidate(bitmap, face, qualityScore, bitmapWidth, bitmapHeight)
                frameCandidates = frameCandidates + candidate
                AppLogger.d("MainScreen: 采集第${frameCandidates.size}帧，质量=$qualityScore")
            }
            
            // 检查是否完成采集
            if (captureStartTime > 0 && elapsed >= MIN_CAPTURE_TIME_MS && bestFrame == null) {
                captureBestFrame()
            }
            
            // 触发识别
            if (bestFrame != null && !latestIsRecognizing && !captureTriggered) {
                captureTriggered = true
                AppLogger.d("MainScreen: 触发识别，最佳帧质量=${bestFrame?.qualityScore}")
                val croppedFace = cropFaceWithMargin(bestFrame!!.bitmap, bestFrame!!.face)
                onFaceDetected(croppedFace, bestFrame!!.face, croppedFace.width, croppedFace.height)
                // 重置采集状态
                captureStartTime = 0L
                frameCandidates = emptyList()
                bestFrame = null
            }
        }
    
    // 原始裁剪函数（不留边距）
    fun cropFace(bitmap: Bitmap, face: Face): Bitmap {
        val boundingBox = face.boundingBox
        return Bitmap.createBitmap(
            bitmap,
            boundingBox.left.coerceAtLeast(0),
            boundingBox.top.coerceAtLeast(0),
            boundingBox.width().coerceAtMost(bitmap.width - boundingBox.left),
            boundingBox.height().coerceAtMost(bitmap.height - boundingBox.top)
        )
    }
    
    // 评估帧质量
    fun evaluateFrameQuality(face: Face, bitmapWidth: Int, bitmapHeight: Int): Float {
        // 1. 人脸面积占比（越大越清晰）
        val faceArea = face.boundingBox.width() * face.boundingBox.height()
        val screenArea = bitmapWidth * bitmapHeight
        val sizeScore = (faceArea.toFloat() / screenArea).coerceIn(0f, 1f)
        
        // 2. 居中度（越居中越好）
        val faceCenterX = (face.boundingBox.left + face.boundingBox.right) / 2f
        val faceCenterY = (face.boundingBox.top + face.boundingBox.bottom) / 2f
        val screenCenterX = bitmapWidth / 2f
        val screenCenterY = bitmapHeight / 2f
        val distance = kotlin.math.sqrt(
            (faceCenterX - screenCenterX) * (faceCenterX - screenCenterX) +
            (faceCenterY - screenCenterY) * (faceCenterY - screenCenterY)
        )
        val maxDistance = kotlin.math.sqrt(screenCenterX * screenCenterX + screenCenterY * screenCenterY)
        val centerScore = 1f - (distance / maxDistance)
        
        // 3. 置信度（使用人脸框的置信度，如果没有则用默认0.7）
        val confidenceScore = 0.7f
        
        // 综合分数：面积30% + 居中30% + 置信度40%
        return sizeScore * 0.3f + centerScore * 0.3f + confidenceScore * 0.4f
    }
    
    val handleCameraReady = remember {
        {
            AppLogger.d("MainScreen: handleCameraReady called")
            onCameraReady()
            AppLogger.d("MainScreen: calling onStartupOverlayHidden to hide overlay")
            onStartupOverlayHidden()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .onSizeChanged { screenSize = Pair(it.width, it.height) }
    ) {
        // 摄像头预览 - 使用key确保稳定
        key("camera_preview") {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onFaceDetected = handleFaceDetected,
                onFaceLost = onFaceLost,
                onCameraReady = handleCameraReady
            )
        }

        // 人脸捕获遮罩层 - 显示识别进度
        FaceCaptureOverlay(
            modifier = Modifier.fillMaxSize(),
            isRecognizing = isRecognizing,
            isSuccess = isRecognitionSuccess,
            matchProgress = if (isRecognizing || isRecognitionSuccess) recognitionProgress else 0f,
            confidence = 0f,
            userName = recognizedUserName,
            userRole = recognizedUserRole,
            userTitle = recognizedUserTitle
        )

        // 顶部标题 - 左上角
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 24.dp, start = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "智慧投篮机",
                fontSize = 36.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            if (schoolTitle != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = schoolTitle,
                    fontSize = 18.sp,
                    color = BasketballOrange
                )
            }
        }

        // 右上角注销按钮
        Button(
            onClick = onLogout,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.8f),
                contentColor = DarkText
            )
        ) {
            Text("注销", fontSize = 16.sp)
        }

        // 底部提示区域 - 圆圈下方
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "等待识别...",
                fontSize = 20.sp,
                color = Color(0xFFFF9800)
            )
        }

        // 底部提示文字 - 始终显示在界面底部居中
        Text(
            text = "请将脸部对准圆形区域",
            fontSize = 20.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        )

        // 开机画面遮罩 - 使用透明度控制显示隐藏
        if (uiState.showStartupOverlay) {
            StartupScreenOverlay(
                visible = uiState.showStartupOverlay,
                onReady = {
                    onCameraReady()
                    onStartupOverlayHidden()
                }
            )
        }

        // 游戏倒计时界面
        if (currentGameState == com.smartbasketball.app.domain.model.GameState.GAME_COUNTDOWN && countdownNumber != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countdownNumber.toString(),
                    fontSize = 120.sp,
                    color = BasketballOrange,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
fun CapturePreviewDialog(
    bitmap: Bitmap,
    originalSize: Pair<Int, Int>?,
    isRecognizing: Boolean,
    isRecognitionSuccess: Boolean,
    isRecognitionFailed: Boolean,
    recognizedUserName: String?,
    recognizedUserRole: String?,
    recognizedUserTitle: String?,
    recognitionError: String?,
    recognitionProgress: Float,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressColor = when {
        isRecognitionSuccess -> Color(0xFF00FFFF)  // 青色
        isRecognitionFailed -> Color(0xFFFF0000)   // 红色
        isRecognizing -> Color(0xFF00FFFF)         // 青色（识别中）
        else -> Color(0xFF00FFFF)
    }

    // 计算照片尺寸
    val widthPx = bitmap.width
    val heightPx = bitmap.height
    val dimensionText = "${widthPx} x ${heightPx} px"
    val originalDimensionText = originalSize?.let { "${it.first} x ${it.second}" } ?: ""

    // 将Bitmap压缩为JPG，计算文件大小
    val outputStream = java.io.ByteArrayOutputStream()
    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
    val jpgBytes = outputStream.toByteArray()
    val jpgSizeKB = jpgBytes.size / 1024.0
    val jpgSizeText = if (jpgSizeKB >= 1024) {
        "%.2f MB".format(jpgSizeKB / 1024.0)
    } else {
        "%.0f KB".format(jpgSizeKB)
    }

    // 将像素转换为dp
    val density = androidx.compose.ui.platform.LocalContext.current.resources.displayMetrics.density
    val widthDp = (widthPx / density).dp
    val heightDp = (heightPx / density).dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(widthDp + 40.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRecognitionSuccess) "识别成功" else if (isRecognitionFailed) "识别失败" else "抓拍预览",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecognitionSuccess) Color(0xFF00FFFF) else if (isRecognitionFailed) Color.Red else Color.Black
                    )

                    // 显示照片尺寸和JPG大小
                    Text(
                        text = "$dimensionText | JPG: $jpgSizeText",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val imageWidth = widthDp.coerceAtMost(200.dp)
                    val heightRatio = if (widthDp.value > 0) heightDp.value / widthDp.value else 1f
                    val imageHeight = (200.dp * heightRatio).coerceAtMost(200.dp)

                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "抓拍照片",
                        modifier = Modifier
                            .size(imageWidth, imageHeight)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 进度条
                    val progressValue =
                        if (isRecognizing || isRecognitionSuccess) recognitionProgress.coerceIn(
                            0f,
                            1f
                        ) else 0f
                    LinearProgressIndicator(
                        progress = progressValue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = progressColor,
                        trackColor = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 识别状态文本
                    when {
                        isRecognitionSuccess -> {
                            Text(
                                text = "${recognizedUserTitle ?: ""} ${recognizedUserName ?: ""}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FFFF)
                            )
                            recognizedUserRole?.let { role ->
                                Text(
                                    text = "($role)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = "匹配度: ${(recognitionProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        isRecognitionFailed -> {
                            Text(
                                text = recognitionError ?: "识别失败",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Red
                            )
                            Text(
                                text = "3秒后重试...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        isRecognizing -> {
                            Text(
                                text = "识别中...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }

                        else -> {
                            Text(
                                text = "等待识别...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // 右下角显示原图尺寸（灰化不显眼）
                if (originalDimensionText.isNotEmpty()) {
                    Text(
                        text = "原图: $originalDimensionText",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun GamePlayScreen(
    countdownNumber: Int?,
    remainingTime: Int?,
    madeBalls: Int,
    missedBalls: Int,
    userName: String?,
    userRole: String?,
    userTitle: String?,
    currentAnimation: com.smartbasketball.app.ui.game.viewmodel.ShotAnimation?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 投篮背景图
        Image(
            painter = painterResource(R.drawable.shoot),
            contentDescription = "投篮背景",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // 半透明遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        // 游戏倒计时（3-2-1）
        if (countdownNumber != null && countdownNumber > 0) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "游戏即将开始",
                    fontSize = 48.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(60.dp))
                Text(
                    text = countdownNumber.toString(),
                    fontSize = 240.sp,
                    color = BasketballOrange,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(60.dp))
                Text(
                    text = userName ?: "",
                    fontSize = 40.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 游戏进行中
        if (countdownNumber == null || countdownNumber <= 0) {
            // 右上角显示用户信息
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = userName ?: "",
                    fontSize = 32.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${userRole ?: ""} ${userTitle ?: ""}",
                    fontSize = 24.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }

            // 左下角显示倒计时
            Text(
                text = "${remainingTime ?: 0}秒",
                fontSize = 28.sp,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            )

            // 中心：进球计数
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = madeBalls.toString(),
                    fontSize = 200.sp,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "进球",
                    fontSize = 32.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            // 底部显示投篮次数
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val total = madeBalls + missedBalls
                val accuracy = if (total > 0) (madeBalls * 100 / total) else 0
                Text(
                    text = "投篮次数: ${total}  命中率: ${accuracy}%",
                    fontSize = 32.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}