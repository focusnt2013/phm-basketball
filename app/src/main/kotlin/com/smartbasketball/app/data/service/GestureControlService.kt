package com.smartbasketball.app.data.service

import android.graphics.Bitmap

enum class GestureType {
    NONE,
    NOD,
    SHAKE
}

data class GestureResult(
    val type: GestureType,
    val confidence: Float
)

interface GestureControlService {
    suspend fun initialize(): Boolean
    suspend fun recognizeGesture(bitmap: Bitmap): GestureResult
    fun isInitialized(): Boolean
    fun release()
}
