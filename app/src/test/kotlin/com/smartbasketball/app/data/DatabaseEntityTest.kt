package com.smartbasketball.app.data.local

import org.junit.Assert.*
import org.junit.Test

class DatabaseEntityTest {

    @Test
    fun `UserEntity equality based on id`() {
        val user1 = UserEntity(
            id = "user_001",
            name = "张三",
            role = "STUDENT",
            grade = "三年级",
            className = "二班",
            faceFeature = byteArrayOf(1, 2, 3),
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val user2 = UserEntity(
            id = "user_001",
            name = "李四",
            role = "TEACHER",
            grade = null,
            className = null,
            faceFeature = byteArrayOf(4, 5, 6),
            createdAt = 3000L,
            updatedAt = 4000L
        )

        assertEquals(user1, user2)
        assertEquals(user1.hashCode(), user2.hashCode())
    }

    @Test
    fun `UserEntity inequality with different id`() {
        val user1 = UserEntity(
            id = "user_001",
            name = "张三",
            role = "STUDENT",
            grade = "三年级",
            className = "二班",
            faceFeature = byteArrayOf(1, 2, 3),
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val user2 = UserEntity(
            id = "user_002",
            name = "张三",
            role = "STUDENT",
            grade = "三年级",
            className = "二班",
            faceFeature = byteArrayOf(1, 2, 3),
            createdAt = 1000L,
            updatedAt = 2000L
        )

        assertNotEquals(user1, user2)
    }

    @Test
    fun `GameRecordEntity stores all fields correctly`() {
        val record = GameRecordEntity(
            id = "record_001",
            userId = "user_001",
            userName = "张三",
            mode = "FIXED_COUNT",
            startTime = 1000L,
            endTime = 2000L,
            totalBalls = 20,
            madeBalls = 15,
            missedBalls = 5,
            accuracy = 0.75f,
            duration = 1000L,
            isSynced = false
        )

        assertEquals("record_001", record.id)
        assertEquals("user_001", record.userId)
        assertEquals("张三", record.userName)
        assertEquals("FIXED_COUNT", record.mode)
        assertEquals(20, record.totalBalls)
        assertEquals(15, record.madeBalls)
        assertEquals(5, record.missedBalls)
        assertEquals(0.75f, record.accuracy, 0.001f)
        assertFalse(record.isSynced)
    }

    @Test
    fun `SyncQueueEntity stores all fields correctly`() {
        val queueItem = SyncQueueEntity(
            id = "queue_001",
            type = "GAME_RECORD",
            data = "test data",
            retryCount = 2,
            createdAt = 1000L,
            lastRetryAt = 2000L
        )

        assertEquals("queue_001", queueItem.id)
        assertEquals("GAME_RECORD", queueItem.type)
        assertEquals("test data", queueItem.data)
        assertEquals(2, queueItem.retryCount)
        assertEquals(1000L, queueItem.createdAt)
        assertEquals(2000L, queueItem.lastRetryAt)
    }

    @Test
    fun `SyncQueueEntity with null lastRetryAt`() {
        val queueItem = SyncQueueEntity(
            id = "queue_001",
            type = "GAME_RECORD",
            data = "test data",
            retryCount = 0,
            createdAt = 1000L,
            lastRetryAt = null
        )

        assertNull(queueItem.lastRetryAt)
    }

    @Test
    fun `UserRole enum values are correct`() {
        assertEquals(3, UserRole.values().size)
        assertTrue(UserRole.values().contains(UserRole.STUDENT))
        assertTrue(UserRole.values().contains(UserRole.TEACHER))
        assertTrue(UserRole.values().contains(UserRole.VISITOR))
    }

    @Test
    fun `GameMode enum values are correct`() {
        assertEquals(2, GameMode.values().size)
        assertTrue(GameMode.values().contains(GameMode.COUNTDOWN))
        assertTrue(GameMode.values().contains(GameMode.FIXED_COUNT))
    }

    @Test
    fun `UserEntity with null optional fields`() {
        val user = UserEntity(
            id = "user_001",
            name = "游客",
            role = "VISITOR",
            grade = null,
            className = null,
            faceFeature = byteArrayOf(),
            createdAt = 1000L,
            updatedAt = 2000L
        )

        assertNull(user.grade)
        assertNull(user.className)
        assertEquals(0, user.faceFeature.size)
    }

    @Test
    fun `GameRecordEntity calculation fields`() {
        val record = GameRecordEntity(
            id = "record_001",
            userId = "user_001",
            userName = "张三",
            mode = "COUNTDOWN",
            startTime = 1000L,
            endTime = 7000L,
            totalBalls = 10,
            madeBalls = 7,
            missedBalls = 3,
            accuracy = 0.7f,
            duration = 6000L,
            isSynced = true
        )

        val calculatedAccuracy = record.madeBalls.toFloat() / record.totalBalls
        assertEquals(0.7f, calculatedAccuracy, 0.001f)
        assertTrue(record.isSynced)
        assertEquals(6000L, record.duration)
    }
}
