package com.smartbasketball.app.util

import org.junit.Assert.*
import org.junit.Test

class ConstantsTest {

    @Test
    fun `game constants have valid values`() {
        assertEquals(60, Constants.Game.DEFAULT_COUNTDOWN_TIME)
        assertEquals(20, Constants.Game.DEFAULT_FIXED_BALL_COUNT)
        assertEquals(15, Constants.Game.COUNTDOWN_STEP)
        assertEquals(5, Constants.Game.BALL_COUNT_STEP)
        assertEquals(30, Constants.Game.MIN_COUNTDOWN_TIME)
        assertEquals(180, Constants.Game.MAX_COUNTDOWN_TIME)
        assertEquals(10, Constants.Game.MIN_BALL_COUNT)
        assertEquals(50, Constants.Game.MAX_BALL_COUNT)
    }

    @Test
    fun `timeout constants have valid values`() {
        assertEquals(30, Constants.Timeout.STANDBY_TIMEOUT)
        assertEquals(30, Constants.Timeout.LEADERBOARD_TIMEOUT)
        assertEquals(5, Constants.Timeout.FACE_DETECT_TIMEOUT)
        assertEquals(500L, Constants.Timeout.GESTURE_HOLD_DURATION)
        assertEquals(5, Constants.Timeout.SYNC_INTERVAL)
    }

    @Test
    fun `recognition constants have valid values`() {
        assertEquals(0.85f, Constants.Recognition.FACE_THRESHOLD, 0.001f)
        assertEquals(0.85f, Constants.Recognition.GESTURE_THRESHOLD, 0.001f)
        assertEquals(100L, Constants.Recognition.FACE_DETECTION_INTERVAL)
    }

    @Test
    fun `ball detection constants have valid values`() {
        assertEquals(50L, Constants.BallDetection.DEFAULT_DEBOUNCE_MS)
        assertEquals(1000L, Constants.BallDetection.STUCK_THRESHOLD_MS)
        assertEquals(9600, Constants.BallDetection.DEFAULT_BAUDRATE)
    }

    @Test
    fun `camera constants have valid values`() {
        assertEquals(640, Constants.Camera.PREVIEW_WIDTH)
        assertEquals(480, Constants.Camera.PREVIEW_HEIGHT)
        assertEquals(30, Constants.Camera.PREVIEW_FPS)
    }

    @Test
    fun `network constants have valid values`() {
        assertEquals(30L, Constants.Network.CONNECT_TIMEOUT)
        assertEquals(30L, Constants.Network.READ_TIMEOUT)
        assertEquals(30L, Constants.Network.WRITE_TIMEOUT)
        assertEquals(3, Constants.Network.MAX_RETRY_COUNT)
    }

    @Test
    fun `animation durations are positive`() {
        assertTrue(Constants.Animation.SHORT > 0)
        assertTrue(Constants.Animation.MEDIUM > 0)
        assertTrue(Constants.Animation.LONG > 0)
        assertTrue(Constants.Animation.EXTRA_LONG > 0)
    }

    @Test
    fun `storage keys are not empty`() {
        assertTrue(Constants.Storage.KEY_SCHOOL_ID.isNotEmpty())
        assertTrue(Constants.Storage.KEY_DEVICE_ID.isNotEmpty())
        assertTrue(Constants.Storage.KEY_DEFAULT_MODE.isNotEmpty())
    }

    @Test
    fun `intent actions are not empty`() {
        assertTrue(Constants.Intent.ACTION_USB_PERMISSION.isNotEmpty())
        assertTrue(Constants.Intent.ACTION_DEVICE_ATTACHED.isNotEmpty())
        assertTrue(Constants.Intent.ACTION_DEVICE_DETACHED.isNotEmpty())
    }

    @Test
    fun `api endpoints are not empty`() {
        assertTrue(Constants.API.FACE_SYNC.isNotEmpty())
        assertTrue(Constants.API.GAME_RECORD_UPLOAD.isNotEmpty())
        assertTrue(Constants.API.LEADERBOARD_CONFIG.isNotEmpty())
    }
}
