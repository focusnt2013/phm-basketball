package com.smartbasketball.app

import com.smartbasketball.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class GameSessionTest {

    @Test
    fun `accuracy calculation with made balls`() {
        val session = GameSession(
            mode = GameMode.FIXED_COUNT,
            startTime = System.currentTimeMillis(),
            totalBalls = 20,
            madeBalls = 15,
            missedBalls = 5,
            isPlaying = true
        )

        assertEquals(0.75f, session.accuracy, 0.001f)
    }

    @Test
    fun `accuracy calculation with zero balls`() {
        val session = GameSession(
            mode = GameMode.COUNTDOWN,
            startTime = System.currentTimeMillis(),
            totalBalls = 0,
            madeBalls = 0,
            missedBalls = 0,
            isPlaying = true
        )

        assertEquals(0f, session.accuracy, 0.001f)
    }

    @Test
    fun `game session default values`() {
        val session = GameSession(
            mode = GameMode.COUNTDOWN,
            startTime = 1000L
        )

        assertEquals(0, session.totalBalls)
        assertEquals(0, session.madeBalls)
        assertEquals(0, session.missedBalls)
        assertFalse(session.isPlaying)
    }

    @Test
    fun `game mode enum values`() {
        assertEquals(2, GameMode.values().size)
        assertEquals(GameMode.COUNTDOWN, GameMode.valueOf("COUNTDOWN"))
        assertEquals(GameMode.FIXED_COUNT, GameMode.valueOf("FIXED_COUNT"))
    }

    @Test
    fun `game state enum values`() {
        assertEquals(10, GameState.values().size)
        assertTrue(GameState.values().contains(GameState.SCENE_RANK))
        assertTrue(GameState.values().contains(GameState.GAME_PLAYING))
        assertTrue(GameState.values().contains(GameState.GAME_ENDED))
    }
}

class GameConfigTest {

    @Test
    fun `default game config values`() {
        val config = GameConfig()

        assertEquals(GameMode.FIXED_COUNT, config.defaultMode)
        assertEquals(60, config.countdownTimeSeconds)
        assertEquals(20, config.fixedBallCount)
        assertEquals(0.85f, config.faceThreshold, 0.001f)
        assertEquals(0.85f, config.gestureThreshold, 0.001f)
        assertEquals(50, config.ballDebounceMs)
        assertEquals(30, config.standbyTimeoutSeconds)
        assertEquals(30, config.leaderboardTimeoutSeconds)
        assertEquals(5, config.faceDetectTimeoutSeconds)
    }

    @Test
    fun `custom game config values`() {
        val config = GameConfig(
            defaultMode = GameMode.COUNTDOWN,
            countdownTimeSeconds = 120,
            fixedBallCount = 30,
            faceThreshold = 0.9f,
            gestureThreshold = 0.8f,
            ballDebounceMs = 100,
            standbyTimeoutSeconds = 60,
            leaderboardTimeoutSeconds = 60,
            faceDetectTimeoutSeconds = 10
        )

        assertEquals(GameMode.COUNTDOWN, config.defaultMode)
        assertEquals(120, config.countdownTimeSeconds)
        assertEquals(30, config.fixedBallCount)
        assertEquals(0.9f, config.faceThreshold, 0.001f)
        assertEquals(0.8f, config.gestureThreshold, 0.001f)
        assertEquals(100, config.ballDebounceMs)
        assertEquals(60, config.standbyTimeoutSeconds)
        assertEquals(60, config.leaderboardTimeoutSeconds)
        assertEquals(10, config.faceDetectTimeoutSeconds)
    }
}
