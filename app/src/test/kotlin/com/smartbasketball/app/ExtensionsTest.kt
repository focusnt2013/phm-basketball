package com.smartbasketball.app.util

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class ExtensionsTest {

    @Test
    fun `formatBytes formats bytes correctly`() {
        assertEquals("500 B", formatBytes(500))
        assertEquals("1 KB", formatBytes(1024))
        assertEquals("1 MB", formatBytes(1024 * 1024))
        assertEquals("1 GB", formatBytes(1024 * 1024 * 1024))
    }

    @Test
    fun `toFormattedDateTime formats correctly`() {
        val timestamp = 1700000000000L
        val formatted = timestamp.toFormattedDateTime()
        assertTrue(formatted.contains("-"))
        assertTrue(formatted.contains(":"))
    }

    @Test
    fun `toRelativeTime returns correct strings`() {
        val now = System.currentTimeMillis()

        assertEquals("刚刚", (now - 1000).toRelativeTime())
        assertEquals("1分钟前", (now - 60 * 1000).toRelativeTime())
        assertEquals("1小时前", (now - 60 * 60 * 1000).toRelativeTime())
        assertEquals("1天前", (now - 24 * 60 * 60 * 1000).toRelativeTime())
    }

    @Test
    fun `toDurationString formats milliseconds correctly`() {
        assertEquals("00:00", 0L.toDurationString())
        assertEquals("00:30", 30000L.toDurationString())
        assertEquals("01:00", 60000L.toDurationString())
        assertEquals("01:30", 90000L.toDurationString())
    }

    @Test
    fun `toPercentString formats correctly`() {
        assertEquals("85%", 0.85f.toPercentString())
        assertEquals("100%", 1.0f.toPercentString())
        assertEquals("0%", 0.0f.toPercentString())
    }

    @Test
    fun `clamp works correctly for floats`() {
        assertEquals(5f, 3f.clamp(5f, 10f), 0.001f)
        assertEquals(5f, 5f.clamp(5f, 10f), 0.001f)
        assertEquals(5f, 7f.clamp(5f, 10f), 0.001f)
        assertEquals(10f, 15f.clamp(5f, 10f), 0.001f)
    }

    @Test
    fun `clamp works correctly for ints`() {
        assertEquals(5, 3.clamp(5, 10))
        assertEquals(5, 5.clamp(5, 10))
        assertEquals(5, 7.clamp(5, 10))
        assertEquals(10, 15.clamp(5, 10))
    }

    @Test
    fun `safeGet returns correct elements`() {
        val list = listOf("a", "b", "c")

        assertEquals("a", list.safeGet(0))
        assertEquals("b", list.safeGet(1))
        assertEquals("c", list.safeGet(2))
        assertNull(list.safeGet(3))
        assertNull(list.safeGet(-1))
    }

    @Test
    fun `secondOrNull and thirdOrNull work correctly`() {
        val list = listOf("a", "b", "c")
        assertEquals("b", list.secondOrNull())
        assertEquals("c", list.thirdOrNull())

        val shortList = listOf("a")
        assertNull(shortList.secondOrNull())
    }

    @Test
    fun `isValidName validates correctly`() {
        assertTrue("John".isValidName())
        assertTrue("John Doe".isValidName())
        assertFalse("".isValidName())
        assertFalse("   ".isValidName())
        assertFalse("A".isValidName())
        assertFalse("A".repeat(60).isValidName())
    }

    @Test
    fun `truncate works correctly`() {
        assertEquals("abc...", "abcdef".truncate(6, "..."))
        assertEquals("abcdef", "abcdef".truncate(10, "..."))
        assertEquals("abc...", "abcdef".truncate(3, "..."))
    }

    @Test
    fun `generateId generates unique ids`() {
        val ids = (1..100).map { generateId() }
        assertEquals(100, ids.toSet().size)
    }

    @Test
    fun `generateTimestampId contains timestamp`() {
        val before = System.currentTimeMillis()
        val id = generateTimestampId()
        val after = System.currentTimeMillis()

        val timestamp = id.substringBefore("_").toLongOrNull()
        assertNotNull(timestamp)
        assertTrue(timestamp!! >= before)
        assertTrue(timestamp <= after)
    }

    @Test
    fun `hasFlag works correctly`() {
        val flags = 0b0101 // 5

        assertTrue(flags.hasFlag(0b0001))
        assertTrue(flags.hasFlag(0b0100))
        assertFalse(flags.hasFlag(0b0010))
    }

    @Test
    fun `addFlag and removeFlag work correctly`() {
        var flags = 0b0001

        flags = flags.addFlag(0b0010)
        assertEquals(0b0011, flags)

        flags = flags.removeFlag(0b0001)
        assertEquals(0b0010, flags)
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}
