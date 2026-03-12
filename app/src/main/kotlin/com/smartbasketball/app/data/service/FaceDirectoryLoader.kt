package com.smartbasketball.app.data.service

import java.io.File

class FaceDirectoryLoader {
    fun getFacesDirectory(): File? = null
    fun hasFacesDirectory(): Boolean = false
    fun isInitialized(): Boolean = false
    suspend fun loadFacesFromDirectory(): LoadResult = LoadResult(0, 0, 0)
    suspend fun reloadAllFaces(): LoadResult = LoadResult(0, 0, 0)
    suspend fun initializeFromAssets(): Boolean = true
    
    data class LoadResult(
        val loadedCount: Int,
        val failedCount: Int,
        val skippedCount: Int
    )
}
