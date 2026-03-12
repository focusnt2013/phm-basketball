package com.smartbasketball.app.data.service

interface DataSyncService {
    suspend fun initialize(): Boolean
    fun release()
}
