package com.smartbasketball.app.ui

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartbasketball.app.ui.game.GameScreen
import com.smartbasketball.app.ui.game.viewmodel.GameViewModel
import com.smartbasketball.app.ui.theme.SmartBasketballTheme
import com.smartbasketball.app.ui.game.LightBackground
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置窗口背景为白色，避免启动时黑屏
        window.setBackgroundDrawableResource(android.R.color.white)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        setContent {
            SmartBasketballTheme {
                val viewModel: GameViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                var showLoginResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

                val onFaceDetected = remember(viewModel) { { bitmap: android.graphics.Bitmap, face: com.google.mlkit.vision.face.Face, _: Int, _: Int ->
                    viewModel.processCameraFrame(bitmap, face)
                } }
                val onFaceLost = remember { { viewModel.clearRecognitionState() } }
                val onCameraReady = remember { { viewModel.onCameraReady() } }
                val onStartupOverlayHidden = remember { { viewModel.hideStartupOverlay() } }
                val onCancelRecognition = remember { { } }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LightBackground
                ) {
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
                        madeBalls = uiState.madeBalls,
                        missedBalls = uiState.missedBalls,
                        currentAnimation = uiState.currentAnimation,
                        onBackToRank = { },
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
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }
}
