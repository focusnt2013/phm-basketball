package com.smartbasketball.app.data.service

import android.graphics.Bitmap
import com.smartbasketball.app.domain.model.User
import com.smartbasketball.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface FaceRecognitionService {
    suspend fun initialize(): Boolean
    suspend fun detectAndRecognize(bitmap: Bitmap): User?
    suspend fun registerFace(userId: String, name: String, bitmap: Bitmap, role: UserRole): Boolean
    suspend fun deleteFace(userId: String): Boolean
    suspend fun clearAllFaces(): Boolean
    fun getRegisteredUserCount(): Int
    fun isInitialized(): Boolean
}
