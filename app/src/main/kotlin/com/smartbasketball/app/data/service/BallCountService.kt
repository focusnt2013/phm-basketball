package com.smartbasketball.app.data.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BallCountService {
    suspend fun initialize(): Boolean
    fun startCounting(): Flow<Int>
    fun getBallCount(): Int
    fun resetCount(): Int
    fun isConnected(): Boolean
    fun release()
    fun getLastSignalTime(): Long
    fun isBallStuck(): Boolean
}

object BallCountConstants {
    const val DEFAULT_DEBOUNCE_MS = 50L
    const val STUCK_THRESHOLD_MS = 1000L
    const val DEFAULT_BAUDRATE = 9600
}
