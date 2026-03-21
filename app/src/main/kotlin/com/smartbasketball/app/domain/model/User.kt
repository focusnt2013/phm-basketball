package com.smartbasketball.app.domain.model

data class User(
    val id: String,
    val name: String,
    val role: UserRole,
    val grade: String? = null,
    val className: String? = null,
    val faceFeature: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as User
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

enum class UserRole {
    STUDENT,
    TEACHER,
    VISITOR
}
