package com.smartbasketball.app.ui.game.viewmodel

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartbasketball.app.data.local.SchoolStorage
import com.smartbasketball.app.data.remote.BasketballApi
import com.smartbasketball.app.domain.model.GameMode
import com.smartbasketball.app.domain.model.GameState
import com.smartbasketball.app.domain.model.GameSession
import com.smartbasketball.app.domain.model.UserRole
import com.smartbasketball.app.domain.model.GameRecord
import com.smartbasketball.app.util.AppLogger
import com.smartbasketball.app.util.GestureType
import com.google.mlkit.vision.face.Face
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val basketballApi: BasketballApi,
    private val schoolStorage: SchoolStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        initializeApp()
    }

    private fun initializeApp() {
        AppLogger.d("========== GameViewModel: initializeApp 开始执行 ==========")
        _uiState.update { it.copy(gameState = GameState.STARTUP, cameraReady = false, showStartupOverlay = true) }
        
        viewModelScope.launch {
            checkLoginStatus()
        }
    }

    fun onCameraReady() {
        _uiState.update { it.copy(cameraReady = true) }
    }
    
    fun clearRecognitionState() {
        _uiState.update {
            it.copy(
                isRecognizing = false,
                recognitionProgress = 0f,
                isRecognitionSuccess = false,
                isRecognitionFailed = false,
                recognitionError = null,
                recognizedUserName = null,
                recognizedUserRole = null,
                recognizedUserTitle = null,
                recognizedUserId = null
            )
        }
        AppLogger.d("GameViewModel: 识别状态已清空")
    }

    private suspend fun checkLoginStatus() {
        delay(500)
        val schoolId = schoolStorage.getSchoolId()
        val schoolTitle = schoolStorage.getSchoolTitle()
        if (schoolId != null && schoolTitle != null) {
            AppLogger.d("GameViewModel: 已登录, schoolTitle=$schoolTitle")
            _uiState.update { 
                it.copy(
                    schoolTitle = schoolTitle,
                    gameState = GameState.STANDBY
                ) 
            }
        } else {
            AppLogger.d("GameViewModel: 未登录，显示登录界面")
            _uiState.update { it.copy(gameState = GameState.LOGIN) }
        }
    }

    fun hideStartupOverlay() {
        AppLogger.d("GameViewModel: hideStartupOverlay")
        _uiState.update { it.copy(showStartupOverlay = false) }
    }
    
    // 点头检测相关变量
    private var lastHeadAngleX = 0f
    private var lastGestureTime = 0L
    private var nodCount = 0
    private val nodThreshold = 8f
    
    fun processCameraFrame(bitmap: Bitmap, face: Face) {
        // 如果已识别成功，检测点头动作
        if (_uiState.value.isRecognitionSuccess && _uiState.value.gameState != GameState.GAME_COUNTDOWN) {
            detectNod(face)
            return
        }
        
        // 如果正在识别中，不处理
        if (_uiState.value.isRecognizing) {
            return
        }
        
        AppLogger.d("GameViewModel: 检测到人脸，开始识别")
        
        viewModelScope.launch {
            recognizeFace(bitmap)
        }
    }
    
    private fun detectNod(face: Face) {
        val currentTime = System.currentTimeMillis()
        
        // 获取头部倾斜角度（eulerAngleX 表示左右倾斜）
        val headAngleX = face.headEulerAngleX
        
        val deltaX = headAngleX - lastHeadAngleX
        lastHeadAngleX = headAngleX
        
        // 忽略时间过短的情况
        if (currentTime - lastGestureTime < 200) {
            return
        }
        lastGestureTime = currentTime
        
        AppLogger.d("GameViewModel: detectNod - headAngleX=$headAngleX, deltaX=$deltaX, nodCount=$nodCount")
        
        // 检测点头动作（头部左右倾斜变化）
        if (kotlin.math.abs(deltaX) > nodThreshold) {
            nodCount++
            if (nodCount >= 2) {
                // 检测到点头，开始游戏
                AppLogger.d("========== 检测到点头动作，开始游戏 ==========")
                nodCount = 0
                startGame()
            }
        } else {
            nodCount = maxOf(0, nodCount - 1)
        }
    }
    
    fun startGame() {
        _uiState.update {
            it.copy(
                gameState = GameState.GAME_PLAYING,
                countdownNumber = null,
                madeBalls = 0,
                missedBalls = 0
            )
        }
    }
    
    private suspend fun recognizeFace(bitmap: Bitmap) {
        val schoolId = schoolStorage.getSchoolId() ?: return
        
        // 将Bitmap转换为JPG字节数组
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val imageBytes = outputStream.toByteArray()
        
        // 更新状态为识别中
        _uiState.update { 
            it.copy(
                isRecognizing = true,
                recognitionProgress = 0.1f + (Math.random() * 0.2f).toFloat(), // 初始模拟进度10-30%
                recognitionError = null,
                isRecognitionSuccess = false,
                isRecognitionFailed = false
            ) 
        }
        
        // 模拟进度增长
        viewModelScope.launch {
            for (i in 1..6) {
                delay(100)
                if (_uiState.value.isRecognizing) {
                    val progress = (0.3f + i * 0.08f + (Math.random() * 0.1f).toFloat()).coerceAtMost(0.75f)
                    _uiState.update { it.copy(recognitionProgress = progress) }
                }
            }
        }
        
        AppLogger.d("GameViewModel: 开始调用识别API, 图片尺寸: ${bitmap.width}x${bitmap.height}")
        
        val result = basketballApi.recognizeFace(schoolId, "jpg", BasketballApi.TEMP_IMEI, imageBytes)
        
        result.onSuccess { recogResult ->
            AppLogger.d("GameViewModel: 识别结果: errcode=${recogResult.errcode}, simi=${recogResult.similarity}, name=${recogResult.userName}")
            
            if (recogResult.errcode == 0 && recogResult.similarity >= 0.8f) {
                // 识别成功
                _uiState.update {
                    it.copy(
                        isRecognizing = false,
                        recognizedUserName = recogResult.userName,
                        recognizedUserRole = recogResult.userRole,
                        recognizedUserTitle = recogResult.userTitle,
                        recognizedUserId = recogResult.userId,
                        recognitionProgress = recogResult.similarity,
                        isRecognitionSuccess = true,
                        isRecognitionFailed = false,
                        recognitionError = null
                    )
                }
            } else {
                // 识别失败 - 但仍显示相似度
                val errorMsg = if (recogResult.errcode != 0) {
                    recogResult.errmsg ?: "识别失败"
                } else {
                    "匹配度不足"
                }
                
                AppLogger.d("GameViewModel: 识别失败: $errorMsg, 相似度=${recogResult.similarity}")
                
                _uiState.update {
                    it.copy(
                        isRecognizing = false,
                        recognitionProgress = recogResult.similarity, // 显示实际相似度
                        isRecognitionSuccess = false,
                        isRecognitionFailed = true,
                        recognitionError = errorMsg
                    )
                }
                
                // 3秒后重试
                delay(3000)
                _uiState.update {
                    it.copy(
                        isRecognitionFailed = false,
                        recognitionError = null
                    )
                }
            }
        }.onFailure { e ->
            AppLogger.e("GameViewModel: 识别异常: ${e.message}")
            
            _uiState.update {
                it.copy(
                    isRecognizing = false,
                    isRecognitionFailed = true,
                    recognitionError = "网络异常: ${e.message}"
                )
            }
            
            // 3秒后重试
            delay(3000)
            _uiState.update {
                it.copy(
                    isRecognitionFailed = false,
                    recognitionError = null
                )
            }
        }
    }
    
    fun login(schoolId: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(startupMessage = "正在登录...") }
            try {
                // 简化处理：直接保存学校ID作为登录成功
                // 暂时使用本地存储，后续完善后端API调用
                val schoolJson = org.json.JSONObject().apply {
                    put("_id", schoolId)
                    put("title", "学校-$schoolId")
                }
                schoolStorage.saveSchool(schoolJson)
                
                _uiState.update {
                    it.copy(
                        schoolTitle = "学校-$schoolId",
                        gameState = GameState.STANDBY,
                        startupMessage = "登录成功"
                    )
                }
                onResult(true, "登录成功")
            } catch (e: Exception) {
                AppLogger.e("登录异常: ${e.message}")
                _uiState.update {
                    it.copy(
                        loginFailed = true,
                        startupMessage = "登录异常"
                    )
                }
                onResult(false, "登录异常: ${e.message}")
            }
        }
    }
    
    fun logout() {
        schoolStorage.clearSchool()
        _uiState.update {
            it.copy(
                gameState = GameState.LOGIN,
                schoolTitle = null,
                userName = null,
                userId = null,
                userRole = null,
                userTitle = null,
                loginFailed = false
            ) 
        }
    }
    
    fun clearLoginState() {
        _uiState.update { it.copy(loginFailed = false) }
    }
}

data class GameUiState(
    val gameState: GameState = GameState.STARTUP,
    val gameMode: GameMode = GameMode.FIXED_COUNT,
    val userName: String? = null,
    val userId: String? = null,
    val userRole: String? = null,
    val userTitle: String? = null,
    val grade: String? = null,
    val className: String? = null,
    val gameSession: GameSession? = null,
    val remainingTime: Int? = null,
    val remainingBalls: Int? = null,
    val countdownNumber: Int? = null,
    val lastRecord: GameRecord? = null,
    val startupMessage: String = "正在启动...",
    val schoolTitle: String? = null,
    val loginFailed: Boolean = false,
    val syncMessage: String = "",
    val validUserCount: Int = 0,
    val cameraReady: Boolean = false,
    val showStartupOverlay: Boolean = true,
    val showGamePlay: Boolean = false,
    val madeBalls: Int = 0,
    val missedBalls: Int = 0,
    val currentAnimation: ShotAnimation? = null,
    // 人脸识别状态
    val isRecognizing: Boolean = false,
    val recognizedUserName: String? = null,
    val recognizedUserRole: String? = null,
    val recognizedUserTitle: String? = null,
    val recognizedUserId: String? = null,
    val recognitionError: String? = null,
    val recognitionProgress: Float = 0f,
    val isRecognitionSuccess: Boolean = false,
    val isRecognitionFailed: Boolean = false
)

enum class ShotAnimation {
    MADE,
    MISSED
}
