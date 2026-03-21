package com.smartbasketball.app.analytics

import org.junit.Assert.*
import org.junit.Test

class AnalyticsTest {

    @Test
    fun `AnalyticsEvent creation`() {
        val event = AnalyticsEvent(
            type = EventType.GAME_STARTED,
            category = EventCategory.GAME,
            label = "test_game",
            value = 100L
        )

        assertEquals(EventType.GAME_STARTED, event.type)
        assertEquals(EventCategory.GAME, event.category)
        assertEquals("test_game", event.label)
        assertEquals(100L, event.value)
    }

    @Test
    fun `AnalyticsState default values`() {
        val state = AnalyticsState()

        assertEquals(0L, state.sessionStartTime)
        assertEquals(0L, state.sessionDuration)
        assertFalse(state.isActive)
        assertEquals(0, state.totalGamesPlayed)
        assertEquals(0, state.totalGoals)
        assertEquals(0, state.totalAttempts)
        assertEquals(0f, state.averageAccuracy, 0.001f)
    }

    @Test
    fun `EventType has all expected values`() {
        val types = EventType.values()
        assertTrue(types.contains(EventType.APP_STARTED))
        assertTrue(types.contains(EventType.GAME_STARTED))
        assertTrue(types.contains(EventType.GAME_ENDED))
        assertTrue(types.contains(EventType.FACE_RECOGNITION_SUCCESS))
        assertTrue(types.contains(EventType.GESTURE_DETECTED))
        assertTrue(types.contains(EventType.BALL_DETECTED))
        assertTrue(types.contains(EventType.SYNC_SUCCESS))
    }

    @Test
    fun `EventCategory has all expected values`() {
        val categories = EventCategory.values()
        assertTrue(categories.contains(EventCategory.LIFECYCLE))
        assertTrue(categories.contains(EventCategory.GAME))
        assertTrue(categories.contains(EventCategory.USER))
        assertTrue(categories.contains(EventCategory.INTERACTION))
        assertTrue(categories.contains(EventCategory.PERFORMANCE))
        assertTrue(categories.contains(EventCategory.NETWORK))
        assertTrue(categories.contains(EventCategory.ERROR))
    }

    @Test
    fun `PerformanceMetrics creation`() {
        val metrics = PerformanceMetrics(
            label = "face_detection_latency",
            value = 150.5f,
            unit = "ms"
        )

        assertEquals("face_detection_latency", metrics.label)
        assertEquals(150.5f, metrics.value, 0.001f)
        assertEquals("ms", metrics.unit)
    }

    @Test
    fun `SessionReport creation`() {
        val report = SessionReport(
            sessionId = "test_session_123",
            startTime = 1000L,
            endTime = 2000L,
            duration = 1000L,
            totalGames = 5,
            totalGoals = 50,
            totalAttempts = 100,
            averageAccuracy = 0.5f,
            eventCount = 20,
            topEvents = emptyList(),
            performanceMetrics = PerformanceSummary(10, 50L)
        )

        assertEquals("test_session_123", report.sessionId)
        assertEquals(5, report.totalGames)
        assertEquals(0.5f, report.averageAccuracy, 0.001f)
    }

    @Test
    fun `EventSummary sorting`() {
        val summaries = listOf(
            EventSummary(EventType.GAME_ENDED, 10, 1000L),
            EventSummary(EventType.BALL_DETECTED, 50, 2000L),
            EventSummary(EventType.FACE_RECOGNITION_SUCCESS, 5, 500L)
        )

        val sorted = summaries.sortedByDescending { it.count }
        assertEquals(EventType.BALL_DETECTED, sorted[0].type)
        assertEquals(EventType.GAME_ENDED, sorted[1].type)
        assertEquals(EventType.FACE_RECOGNITION_SUCCESS, sorted[2].type)
    }

    @Test
    fun `Accuracy calculation`() {
        val totalGoals = 30
        val totalAttempts = 50
        val accuracy = totalGoals.toFloat() / totalAttempts

        assertEquals(0.6f, accuracy, 0.001f)
    }

    @Test
    fun `Zero division handling for accuracy`() {
        val totalGoals = 0
        val totalAttempts = 0
        val accuracy = if (totalAttempts > 0) totalGoals.toFloat() / totalAttempts else 0f

        assertEquals(0f, accuracy, 0.001f)
    }
}
