package com.smartbasketball.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.smartbasketball.app.domain.model.GameState
import com.smartbasketball.app.ui.SettingsScreen
import com.smartbasketball.app.ui.game.viewmodel.GameViewModel
import com.smartbasketball.app.ui.game.GameScreen
import com.smartbasketball.app.ui.components.LeaderboardScreen

sealed class Screen(val route: String) {
    object Game : Screen("game")
    object Settings : Screen("settings")
    object Leaderboard : Screen("leaderboard")
    object Error : Screen("error/{message}") {
        fun createRoute(message: String) = "error/${message}"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Game.route
    ) {
        composable(Screen.Game.route) {
            GameScreen(
                viewModel = viewModel,
                gameState = uiState.gameState,
                isLoggedIn = uiState.schoolTitle != null,
                schoolTitle = uiState.schoolTitle,
                userName = uiState.userName,
                userRole = uiState.userRole,
                gameMode = uiState.gameMode,
                gameSession = uiState.gameSession,
                remainingTime = uiState.remainingTime,
                remainingBalls = uiState.remainingBalls,
                countdownNumber = uiState.countdownNumber,
                lastRecord = uiState.lastRecord,
                startupMessage = uiState.startupMessage,
                showGamePlay = uiState.showGamePlay,
                madeBalls = uiState.madeBalls,
                missedBalls = uiState.missedBalls,
                onBackToRank = { },
                onGestureDetected = { },
                onLoginSuccess = { },
                onLogin = { _, _ -> }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Leaderboard.route) {
            LeaderboardScreen(
                schoolId = "test_school",
                userId = uiState.userName,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

object NavArguments {
    const val SCHOOL_ID = "school_id"
    const val USER_ID = "user_id"
    const val ERROR_MESSAGE = "message"
}

object NavResult {
    const val GAME_RECORD_ID = "game_record_id"
    const val USER_ID = "user_id"
}
