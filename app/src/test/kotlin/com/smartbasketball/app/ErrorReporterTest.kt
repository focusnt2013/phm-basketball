package com.smartbasketball.app.service

import org.junit.Assert.*
import org.junit.Test

class ErrorReporterTest {

    @Test
    fun `ErrorSeverity has all expected values`() {
        val severities = ErrorSeverity.values()
        assertEquals(4, severities.size)
        assertTrue(severities.contains(ErrorSeverity.LOW))
        assertTrue(severities.contains(ErrorSeverity.MEDIUM))
        assertTrue(severities.contains(ErrorSeverity.HIGH))
        assertTrue(severities.contains(ErrorSeverity.CRITICAL))
    }

    @Test
    fun `ErrorCategory has all expected values`() {
        val categories = ErrorCategory.values()
        assertEquals(10, categories.size)
        assertTrue(categories.contains(ErrorCategory.GENERAL))
        assertTrue(categories.contains(ErrorCategory.NETWORK))
        assertTrue(categories.contains(ErrorCategory.IO))
        assertTrue(categories.contains(ErrorCategory.MEMORY))
        assertTrue(categories.contains(ErrorCategory.THREAD))
    }

    @Test
    fun `ErrorEvent stores correct values`() {
        val event = ErrorEvent(
            id = "ERR_123",
            timestamp = System.currentTimeMillis(),
            message = "Test error",
            tag = "TestTag",
            stackTrace = "Stack trace here",
            severity = ErrorSeverity.HIGH,
            category = ErrorCategory.GENERAL,
            deviceInfo = "Test Device"
        )

        assertEquals("ERR_123", event.id)
        assertEquals("Test error", event.message)
        assertEquals(ErrorSeverity.HIGH, event.severity)
        assertEquals(ErrorCategory.GENERAL, event.category)
    }

    @Test
    fun `ErrorException message extraction works`() {
        val exception = IllegalArgumentException("Test message")
        val stackTrace = exception.getStackTraceString()

        assertTrue(stackTrace.contains("Test message"))
        assertTrue(stackTrace.contains("ErrorExceptionKt"))
    }

    @Test
    fun `Error severity order is correct`() {
        val order = listOf(
            ErrorSeverity.LOW,
            ErrorSeverity.MEDIUM,
            ErrorSeverity.HIGH,
            ErrorSeverity.CRITICAL
        )

        assertEquals(4, order.size)
        assertTrue(order.indexOf(ErrorSeverity.LOW) < order.indexOf(ErrorSeverity.MEDIUM))
        assertTrue(order.indexOf(ErrorSeverity.MEDIUM) < order.indexOf(ErrorSeverity.HIGH))
        assertTrue(order.indexOf(ErrorSeverity.HIGH) < order.indexOf(ErrorSeverity.CRITICAL))
    }
}
