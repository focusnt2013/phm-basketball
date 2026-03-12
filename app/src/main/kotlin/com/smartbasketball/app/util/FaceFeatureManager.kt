package com.smartbasketball.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.smartbasketball.app.data.local.UserDao
import com.smartbasketball.app.data.local.UserEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class FaceProgress(
    val total: Int,
    val processed: Int,
    val currentUser: String,
    val isComplete: Boolean = false,
    val error: String? = null
)

@Singleton
class FaceFeatureManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: UserDao,
    private val faceEmbeddingManager: FaceEmbeddingManager
) {
    private val _progress = MutableStateFlow(FaceProgress(0, 0, ""))
    val progress: StateFlow<FaceProgress> = _progress.asStateFlow()

    private val processingUsers = ConcurrentHashMap.newKeySet<String>()

    suspend fun processUsers(users: List<UserEntity>) {
        AppLogger.d("========== 开始处理人脸特征 ==========")
        AppLogger.d("总用户数: ${users.size}")
        
        val usersNeedingProcessing = users.filter { user ->
            user.faceUrl != null && (user.faceFeature == null || user.featureGeneratedAt < user.faceTs)
        }
        
        AppLogger.d("需要生成特征的 用户数: ${usersNeedingProcessing.size}")

        if (usersNeedingProcessing.isEmpty()) {
            AppLogger.d("所有用户特征已存在，无需重新生成")
            _progress.value = FaceProgress(0, 0, "", isComplete = true)
            return
        }

        try {
            faceEmbeddingManager.loadModel()
        } catch (e: Exception) {
            AppLogger.e("加载人脸识别模型失败: ${e.message}, 将跳过特征生成")
            _progress.value = FaceProgress(0, 0, "", isComplete = true, error = "模型加载失败")
            return
        }

        _progress.value = FaceProgress(usersNeedingProcessing.size, 0, "")

        usersNeedingProcessing.forEachIndexed { index, user ->
            if (processingUsers.contains(user.id)) return@forEachIndexed

            processingUsers.add(user.id)
            val displayName = "${user.role}${user.title}${user.name}"
            _progress.value = _progress.value.copy(
                processed = index,
                currentUser = displayName
            )

            try {
                generateFeatureForUser(user)
            } catch (e: Exception) {
                AppLogger.e("生成人脸特征失败 for ${user.name}: ${e.message}, 跳过该用户")
            } finally {
                processingUsers.remove(user.id)
            }
        }

        _progress.value = _progress.value.copy(
            processed = usersNeedingProcessing.size,
            currentUser = "",
            isComplete = true
        )
    }
    
    suspend fun regenerateAllFeatures(onComplete: (() -> Unit)? = null) {
        AppLogger.d("========== 强制重新生成所有用户的人脸特征 ==========")
        
        try {
            faceEmbeddingManager.loadModel()
        } catch (e: Exception) {
            AppLogger.e("加载模型失败: ${e.message}")
            return
        }
        
        userDao.clearAllFaceFeatures()
        AppLogger.d("已清除所有用户的人脸特征")
        
        val allUsers = userDao.getAllUsersList()
        val usersWithFaceUrl = allUsers.filter { it.faceUrl != null }
        
        AppLogger.d("需要生成特征的 用户数: ${usersWithFaceUrl.size}")
        
        if (usersWithFaceUrl.isEmpty()) {
            AppLogger.d("没有需要生成特征的用户")
            onComplete?.invoke()
            return
        }
        
        _progress.value = FaceProgress(usersWithFaceUrl.size, 0, "")
        
        usersWithFaceUrl.forEachIndexed { index, user ->
            processingUsers.add(user.id)
            val displayName = "${user.role}${user.title}${user.name}"
            _progress.value = _progress.value.copy(
                processed = index,
                currentUser = displayName
            )
            
            try {
                generateFeatureForUser(user)
            } catch (e: Exception) {
                AppLogger.e("重新生成特征失败 for ${user.name}: ${e.message}, 跳过")
            } finally {
                processingUsers.remove(user.id)
            }
        }
        
        _progress.value = _progress.value.copy(
            processed = usersWithFaceUrl.size,
            currentUser = "",
            isComplete = true
        )
        AppLogger.d("========== 强制重新生成完成 ==========")
        onComplete?.invoke()
    }
    
    suspend fun retryProcessUsersWithoutFeature(onComplete: (() -> Unit)? = null) {
        AppLogger.d("========== 重新处理未生成特征的用户 ==========")
        
        try {
            faceEmbeddingManager.loadModel()
        } catch (e: Exception) {
            AppLogger.e("加载模型失败: ${e.message}")
            return
        }
        
        var allUsers = userDao.getAllUsersList()
        
        for (user in allUsers) {
            if (user.faceUrl != null) {
                val shouldRegenerate = user.faceFeature == null || 
                    (user.faceFeature != null && isFeatureCorrupted(user.faceFeature))
                
                if (shouldRegenerate) {
                    if (user.faceFeature != null && isFeatureCorrupted(user.faceFeature)) {
                        AppLogger.w("检测到损坏的特征 for ${user.name}, 清除并重新生成")
                        userDao.clearFaceFeature(user.id)
                    }
                }
            }
        }
        
        allUsers = userDao.getAllUsersList()
        
        val usersWithoutFeature = allUsers.filter { user ->
            user.faceUrl != null && user.faceFeature == null
        }
        
        AppLogger.d("需要重新生成特征的用户数: ${usersWithoutFeature.size}")
        
        if (usersWithoutFeature.isEmpty()) {
            AppLogger.d("所有用户已生成特征")
            onComplete?.invoke()
            return
        }
        
        _progress.value = FaceProgress(usersWithoutFeature.size, 0, "")
        
        usersWithoutFeature.forEachIndexed { index, user ->
            processingUsers.add(user.id)
            val displayName = "${user.role}${user.title}${user.name}"
            _progress.value = _progress.value.copy(
                processed = index,
                currentUser = displayName
            )
            
            try {
                generateFeatureForUser(user)
            } catch (e: Exception) {
                AppLogger.e("重新生成特征失败 for ${user.name}: ${e.message}, 跳过")
            } finally {
                processingUsers.remove(user.id)
            }
        }
        
        _progress.value = _progress.value.copy(
            processed = usersWithoutFeature.size,
            currentUser = "",
            isComplete = true
        )
        AppLogger.d("========== 重新处理完成 ==========")
        onComplete?.invoke()
    }

    private suspend fun generateFeatureForUser(user: UserEntity) {
        val faceUrl = user.faceUrl ?: return

        val bitmap = downloadBitmap(faceUrl) ?: run {
            AppLogger.e("下载头像失败: $faceUrl for ${user.name}")
            return
        }

        val feature = faceEmbeddingManager.extractFaceFeature(bitmap) ?: run {
            AppLogger.e("提取人脸特征失败 for ${user.name}")
            return
        }

        val featureBytes = floatArrayToByteArray(feature)
        
        val now = System.currentTimeMillis()
        userDao.updateFaceFeature(
            userId = user.id,
            feature = featureBytes,
            generatedAt = now,
            updatedAt = now
        )

        AppLogger.d("人脸特征生成成功: ${user.name}, 维度=${feature.size}")
    }

    private suspend fun downloadBitmap(urlString: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.setUseCaches(false)
            connection.connect()

            val inputStream = connection.inputStream
            val byteArray = inputStream.readBytes()
            inputStream.close()
            connection.disconnect()

            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } catch (e: Exception) {
            AppLogger.e("下载图片失败: $urlString, ${e.message}")
            null
        }
    }
    
    suspend fun extractFaceFeatureFromUrl(faceUrl: String): FloatArray? {
        val bitmap = downloadBitmap(faceUrl) ?: run {
            AppLogger.e("extractFaceFeatureFromUrl: 下载头像失败 for $faceUrl")
            return null
        }
        
        return faceEmbeddingManager.extractFaceFeature(bitmap)
    }

    suspend fun extractFaceFeatureFromUrlWithName(faceUrl: String, userName: String): FloatArray? {
        AppLogger.d("========== extractFaceFeatureFromUrlWithName: 开始处理用户 $userName ==========")
        
        val bitmap = downloadBitmap(faceUrl) ?: run {
            AppLogger.e("extractFaceFeatureFromUrlWithName: 下载头像失败 for $faceUrl")
            return null
        }
        AppLogger.d("extractFaceFeatureFromUrlWithName: 图片下载成功 ${bitmap.width}x${bitmap.height}")
        
        val feature = faceEmbeddingManager.extractFaceFeature(bitmap) ?: run {
            AppLogger.e("extractFaceFeatureFromUrlWithName: 特征提取失败 for $userName")
            return null
        }
        
        AppLogger.d("========== extractFaceFeatureFromUrlWithName: 用户 $userName 处理完成，特征维度=${feature.size} ==========")
        
        return feature
    }

    private fun floatArrayToByteArray(floatArray: FloatArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(floatArray.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
        for (value in floatArray) {
            byteBuffer.putFloat(value)
        }
        return byteBuffer.array()
    }

    private fun isFeatureCorrupted(feature: ByteArray): Boolean {
        if (feature.size < 16) return true
        
        val actualDimension = feature.size / 4
        
        if (actualDimension != 256) {
            AppLogger.w("Feature dimension mismatch: expected 256, got $actualDimension")
            return true
        }
        
        try {
            val buffer = ByteBuffer.wrap(feature).order(java.nio.ByteOrder.nativeOrder())
            for (i in 0 until 5) {
                val value = buffer.getFloat(i * 4)
                if (value.isNaN() || value.isInfinite() || kotlin.math.abs(value) > 1e10f) {
                    return true
                }
            }
            return false
        } catch (e: Exception) {
            return true
        }
    }

    fun getTestFaceImages(): List<Pair<String, String>> {
        val cacheDir = File(context.cacheDir, "test_faces")
        if (!cacheDir.exists()) {
            return emptyList()
        }
        
        return cacheDir.listFiles()
            ?.filter { it.extension == "jpg" }
            ?.map { it.nameWithoutExtension to it.absolutePath }
            ?: emptyList()
    }

    fun clearTestFaceImages() {
        val cacheDir = File(context.cacheDir, "test_faces")
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }

    fun reset() {
        processingUsers.clear()
        _progress.value = FaceProgress(0, 0, "")
    }

    fun close() {
        faceEmbeddingManager.close()
    }
}
