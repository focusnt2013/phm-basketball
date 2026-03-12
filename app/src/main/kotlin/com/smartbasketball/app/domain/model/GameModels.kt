package com.smartbasketball.app.domain.model

enum class GameMode {
    COUNTDOWN,
    FIXED_COUNT
}

enum class GameState {
    STARTUP,
    LOGIN,
    SCENE_RANK,
    FACE_DETECT,
    FACE_RECOGNIZED,
    FACE_VISITOR,
    STANDBY,
    MODE_SELECT,
    GAME_COUNTDOWN,
    GAME_STARTING,
    GAME_PLAYING,
    GAME_ENDED,
    LEADERBOARD
}

data class GameConfig(
    val defaultMode: GameMode = GameMode.FIXED_COUNT,
    val countdownTimeSeconds: Int = 60,
    val fixedBallCount: Int = 20,
    val faceThreshold: Float = 0.85f,
    val gestureThreshold: Float = 0.85f,
    val ballDebounceMs: Int = 50,
    val standbyTimeoutSeconds: Int = 30,
    val leaderboardTimeoutSeconds: Int = 30,
    val faceDetectTimeoutSeconds: Int = 5
)

data class GameRecord(
    val id: String,
    val userId: String?,
    val userName: String,
    val mode: GameMode,
    val startTime: Long,
    val endTime: Long,
    val totalBalls: Int,
    val madeBalls: Int,
    val missedBalls: Int,
    val accuracy: Float,
    val duration: Long,
    val isSynced: Boolean = false
)

data class GameSession(
    val mode: GameMode,
    val startTime: Long,
    var totalBalls: Int = 0,
    var madeBalls: Int = 0,
    var missedBalls: Int = 0,
    var isPlaying: Boolean = false
) {
    val accuracy: Float
        get() = if (totalBalls > 0) madeBalls.toFloat() / totalBalls else 0f
}
