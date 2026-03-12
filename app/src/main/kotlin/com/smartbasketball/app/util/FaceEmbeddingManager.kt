package com.smartbasketball.app.util

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.smartbasketball.app.data.local.UserDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

data class ModelLoadState(
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val error: String? = null
)

@Singleton
class FaceEmbeddingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: UserDao
) {
    private var isModelLoaded = false
    private var faceLandmarker: FaceLandmarker? = null

    private val _loadState = MutableStateFlow(ModelLoadState())
    val loadState: StateFlow<ModelLoadState> = _loadState.asStateFlow()

    companion object {
        private const val TAG = "FaceEmbeddingManager"
    }

    suspend fun loadModel() = withContext(Dispatchers.IO) {
        if (isModelLoaded) return@withContext
        
        _loadState.value = ModelLoadState(isLoading = true)
        
        try {
            initMediaPipeFaceLandmarker()
            AppLogger.d("$TAG: MediaPipe FaceLandmarker模型加载完成")
            isModelLoaded = true
            _loadState.value = ModelLoadState(isLoaded = true)
        } catch (e: Exception) {
            val errorMsg = "模型加载失败: ${e.message}"
            AppLogger.e("$TAG: $errorMsg")
            _loadState.value = ModelLoadState(error = errorMsg)
            throw e
        }
    }

    private fun initMediaPipeFaceLandmarker() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("face_landmarker.task")
            .build()
        
        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setNumFaces(1)
            .setMinFaceDetectionConfidence(0.5f)
            .setMinFacePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .build()
        
        faceLandmarker = FaceLandmarker.createFromOptions(context, options)
        AppLogger.d("$TAG: FaceLandmarker初始化完成")
    }

    fun isLoaded(): Boolean = isModelLoaded

    fun close() {
        faceLandmarker?.close()
        faceLandmarker = null
        isModelLoaded = false
    }

    suspend fun ensureModelLoaded() {
        if (!isModelLoaded) {
            loadModel()
        }
    }

    suspend fun extractFaceFeature(bitmap: Bitmap): FloatArray? = withContext(Dispatchers.IO) {
        try {
            if (faceLandmarker == null) {
                loadModel()
            }

            val argbBitmap = if (bitmap.config != android.graphics.Bitmap.Config.ARGB_8888) {
                bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true) ?: bitmap
            } else {
                bitmap
            }
            
            val mpImage = BitmapImageBuilder(argbBitmap).build()
            
            val result = faceLandmarker?.detect(mpImage)
            
            if (result == null || result.faceLandmarks().isEmpty()) {
                AppLogger.w("$TAG: 未检测到人脸")
                return@withContext null
            }
            
            val landmarks = result.faceLandmarks()[0]
            AppLogger.d("$TAG: 检测到 ${landmarks.size} 个关键点")
            
            // 从关键点提取256维特征向量
            val embedding = extractEmbeddingFromLandmarks(landmarks)
            
            AppLogger.d("$TAG: 特征提取完成，维度=${embedding.size}")
            embedding
        } catch (e: Exception) {
            AppLogger.e("$TAG: 特征提取失败: ${e.message}")
            null
        }
    }

    private fun extractEmbeddingFromLandmarks(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): FloatArray {
        val embedding = FloatArray(256)
        var index = 0
        
        // 1. 添加关键点坐标（前90个点 × 3维 = 270维，取前256维）
        for (i in 0 until minOf(landmarks.size, 85)) {
            if (index >= 256) break
            
            val landmark = landmarks[i]
            embedding[index++] = landmark.x()
            if (index >= 256) break
            embedding[index++] = landmark.y()
            if (index >= 256) break
            embedding[index++] = landmark.z()
        }
        
        // 填充剩余维度
        while (index < 256) {
            embedding[index++] = 0f
        }
        
        // L2归一化
        normalize(embedding)
        
        return embedding
    }

    private fun normalize(vector: FloatArray): FloatArray {
        var sum = 0f
        for (v in vector) {
            sum += v * v
        }
        val magnitude = sqrt(sum)
        
        if (magnitude > 0) {
            for (i in vector.indices) {
                vector[i] = vector[i] / magnitude
            }
        }
        
        return vector
    }

    fun generateEmbedding(face: Any, bitmap: Bitmap): FloatArray? {
        var result: FloatArray? = null
        val scope = MainScope()
        scope.launch {
            result = extractFaceFeature(bitmap)
        }
        Thread.sleep(500) // 等待异步执行完成
        return result
    }

    fun cosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        if (embedding1.size != embedding2.size) return 0f

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
            norm1 += embedding1[i] * embedding1[i]
            norm2 += embedding2[i] * embedding2[i]
        }

        val similarity = if (norm1 > 0 && norm2 > 0) {
            dotProduct / (sqrt(norm1) * sqrt(norm2))
        } else {
            0f
        }

        return similarity
    }
}
