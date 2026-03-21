package com.smartbasketball.app.util

import org.junit.Assert.*
import org.junit.Test

class AppLoggerTest {

    @Test
    fun `logD returns input for chaining`() {
        val input = "test string"
        val result = input.logD()
        assertEquals(input, result)
    }

    @Test
    fun `logI returns input for chaining`() {
        val input = "test string"
        val result = input.logI()
        assertEquals(input, result)
    }

    @Test
    fun `logE returns input for chaining`() {
        val input = "test string"
        val result = input.logE()
        assertEquals(input, result)
    }

    @Test
    fun `measureTime returns result`() {
        val result = measureTimeMillis {
            Thread.sleep(10)
        }
        assertTrue(result >= 10)
    }

    @Test
    fun `measureTimeNanos returns result`() {
        val result = measureTimeNanos {
            // Empty operation
        }
        assertTrue(result >= 0)
    }
}
