package com.smartbasketball.app

import com.smartbasketball.app.domain.model.User
import com.smartbasketball.app.domain.model.UserRole
import org.junit.Assert.*
import org.junit.Test

class UserModelTest {

    @Test
    fun `user model with all fields`() {
        val user = User(
            id = "user_001",
            name = "张三",
            role = UserRole.STUDENT,
            grade = "三年级",
            className = "二班",
            faceFeature = floatArrayOf(0.1f, 0.2f, 0.3f)
        )

        assertEquals("user_001", user.id)
        assertEquals("张三", user.name)
        assertEquals(UserRole.STUDENT, user.role)
        assertEquals("三年级", user.grade)
        assertEquals("二班", user.className)
        assertNotNull(user.faceFeature)
        assertEquals(3, user.faceFeature?.size)
    }

    @Test
    fun `user model with minimal fields`() {
        val user = User(
            id = "visitor_001",
            name = "游客",
            role = UserRole.VISITOR
        )

        assertEquals("visitor_001", user.id)
        assertEquals("游客", user.name)
        assertEquals(UserRole.VISITOR, user.role)
        assertNull(user.grade)
        assertNull(user.className)
        assertNull(user.faceFeature)
    }

    @Test
    fun `user equality based on id`() {
        val user1 = User(
            id = "user_001",
            name = "张三",
            role = UserRole.STUDENT
        )

        val user2 = User(
            id = "user_001",
            name = "李四",
            role = UserRole.TEACHER
        )

        assertEquals(user1, user2)
        assertEquals(user1.hashCode(), user2.hashCode())
    }

    @Test
    fun `user inequality based on different id`() {
        val user1 = User(
            id = "user_001",
            name = "张三",
            role = UserRole.STUDENT
        )

        val user2 = User(
            id = "user_002",
            name = "张三",
            role = UserRole.STUDENT
        )

        assertNotEquals(user1, user2)
    }

    @Test
    fun `user role enum values`() {
        assertEquals(3, UserRole.values().size)
        assertTrue(UserRole.values().contains(UserRole.STUDENT))
        assertTrue(UserRole.values().contains(UserRole.TEACHER))
        assertTrue(UserRole.values().contains(UserRole.VISITOR))
    }
}
