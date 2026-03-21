package com.smartbasketball.app

import com.smartbasketball.app.data.service.GestureControlService
import com.smartbasketball.app.data.service.GestureType
import com.smartbasketball.app.data.service.VoiceService
import org.junit.Assert.*
import org.junit.Test

class GestureConstantsTest {

    @Test
    fun `gesture constants have correct values`() {
        assertEquals(0.85f, GestureConstants.THRESHOLD_DEFAULT, 0.001f)
        assertEquals(500L, GestureConstants.GESTURE_HOLD_DURATION_MS)
    }

    @Test
    fun `gesture type enum values`() {
        assertEquals(4, GestureType.values().size)
        assertTrue(GestureType.values().contains(GestureType.NONE))
        assertTrue(GestureType.values().contains(GestureType.RIGHT_HAND_RAISED))
        assertTrue(GestureType.values().contains(GestureType.FIST))
        assertTrue(GestureType.values().contains(GestureType.BOTH_HANDS_CROSSED))
    }
}

class GestureResultTest {

    @Test
    fun `gesture result stores type and confidence`() {
        val result = GestureResult(GestureType.RIGHT_HAND_RAISED, 0.95f)

        assertEquals(GestureType.RIGHT_HAND_RAISED, result.type)
        assertEquals(0.95f, result.confidence, 0.001f)
    }

    @Test
    fun `gesture result equality`() {
        val result1 = GestureResult(GestureType.FIST, 0.9f)
        val result2 = GestureResult(GestureType.FIST, 0.9f)
        val result3 = GestureResult(GestureType.FIST, 0.8f)

        assertEquals(result1, result2)
        assertNotEquals(result1, result3)
    }

    @Test
    fun `gesture result default confidence`() {
        val result = GestureResult(GestureType.NONE, 0f)

        assertEquals(0f, result.confidence, 0.001f)
        assertEquals(GestureType.NONE, result.type)
    }
}
