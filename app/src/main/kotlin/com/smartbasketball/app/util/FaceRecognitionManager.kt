package com.smartbasketball.app.util

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.smartbasketball.app.data.local.UserDao
import com.smartbasketball.app.data.local.UserEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.sqrt
import java.util.concurrent.atomic.AtomicBoolean

data class RecognitionState(
    val isRecognizing: Boolean = false,
    val recognizedUser: UserEntity? = null,
    val confidence: Float = 0f,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isWaitingGesture: Boolean = false,
    val gestureType: GestureType? = null
)

enum class GestureType {
    NOD,
    SHAKE
}

@Singleton
class FaceRecognitionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: UserDao,
    val faceEmbeddingManager: FaceEmbeddingManager,
    private val faceFeatureManager: FaceFeatureManager
) {
    private val _recognitionState = MutableStateFlow(RecognitionState())
    val recognitionState: StateFlow<RecognitionState> = _recognitionState.asStateFlow()
    
    val modelLoadState = faceEmbeddingManager.loadState
    
    private val _testStatus = MutableStateFlow("")
    val testStatus: StateFlow<String> = _testStatus.asStateFlow()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var lastRecognizedUserId: String? = null
    private var lastRecognitionTime: Long = 0
    private val recognitionCooldown = 3000L

    private var stableMatchCount = 0
    private var lastMatchedUserId: String? = null
    private val requiredStableFrames = 3  // 累计3次（不连续）相似度超过阈值就算成功
    
    private var recognitionStartTime: Long = 0
    private val recognitionTimeout = 10000L // 10秒超时，适应大规模用户库(3000+)
    
    private var cachedUsers: List<UserEntity> = emptyList()
    private var cachedFeatures: Map<String, FloatArray> = emptyMap()
    private var isCacheLoaded = false
    
    // 防止重复启动测试
    private val isTesting = AtomicBoolean(false)
    
    // 存储一致性测试中提取的特征
    private var testFeatures: List<Pair<String, FloatArray>> = emptyList()
    private var testExtractTimes: Map<String, Long> = emptyMap()
    
    // KD-Tree索引
    private var kdTreeIndex: KdTreeIndex? = null
    private var isKdTreeReady = false

    private var lastHeadAngleX: Float = 0f
    private var lastHeadAngleY: Float = 0f
    private var lastGestureTime: Long = 0
    private var nodCount: Int = 0
    private var shakeCount: Int = 0
    private val gestureHistory = mutableListOf<Float>()
    private val gestureCooldown = 200L  // 降低冷却时间到200ms

    fun reset() {
        lastRecognizedUserId = null
        lastRecognitionTime = 0
        stableMatchCount = 0
        lastMatchedUserId = null
        lastHeadAngleX = 0f
        lastHeadAngleY = 0f
        nodCount = 0
        shakeCount = 0
        gestureHistory.clear()
        _recognitionState.value = RecognitionState()
    }

    fun resetGesture() {
        nodCount = 0
        shakeCount = 0
        gestureHistory.clear()
        _recognitionState.value = _recognitionState.value.copy(
            isWaitingGesture = false,
            gestureType = null
        )
    }
    
    suspend fun loadUsersToCache() {
        val usersWithFeatures = userDao.getUsersWithFeatures()
        
        // 过滤：只有当用户有新模型特征信息时才加载（modelType, modelVersion, featureDimension都不为null）
        // 旧版本的192维特征数据没有这些字段，不予以加载
        val validUsers = usersWithFeatures.filter { user ->
            user.modelType != null && user.modelVersion != null && user.featureDimension != null
        }
        
        cachedUsers = validUsers
        AppLogger.d("loadUsersToCache: 总用户数=${usersWithFeatures.size}, 有效用户数(新特征)=${validUsers.size}")
        
        cachedFeatures = validUsers.mapNotNull { user ->
            user.faceFeature?.let { featureBytes ->
                user.id to byteArrayToFloatArray(featureBytes)
            }
        }.toMap()
        
        // 打印姓刘的用户信息
        val liuUsers = cachedUsers.filter { it.name?.contains("刘") == true }
        AppLogger.d("加载缓存: 姓刘的用户数: ${liuUsers.size}")
        
        // 打印刘老师的特征信息
        val liuLaoShi = cachedUsers.find { it.name == "刘老师" }
        if (liuLaoShi != null) {
            val feature = cachedFeatures[liuLaoShi.id]
            if (feature != null) {
                AppLogger.d("刘老师特征: id=${liuLaoShi.id}, feature前10值: ${feature.sliceArray(0..9).contentToString()}")
            } else {
                AppLogger.e("刘老师特征缺失! userId=${liuLaoShi.id}")
            }
        } else {
            AppLogger.e("未找到刘老师! cachedUsers前10: ${cachedUsers.take(10).map { it.name }}")
        }
        
        // 查找刘先生
        val liuXianSheng = cachedUsers.find { it.name == "刘先生" }
        if (liuXianSheng != null) {
            AppLogger.d("找到刘先生: ${liuXianSheng.name}, role=${liuXianSheng.role}, title=${liuXianSheng.title}, id=${liuXianSheng.id}")
            val hasFeature = cachedFeatures.containsKey(liuXianSheng.id)
            AppLogger.d("刘先生是否有特征: $hasFeature")
        } else {
            AppLogger.d("未找到刘先生")
            // 打印前10个用户看看
            AppLogger.d("前10个用户: ${cachedUsers.take(10).map { it.name }}")
        }
        
        if (liuUsers.isNotEmpty()) {
            liuUsers.take(10).forEach { user ->
                AppLogger.d("姓刘用户: ${user.name}, role=${user.role}, title=${user.title}")
            }
        }
        
        // 构建KD-Tree索引
        buildKdTreeIndex()
        
        // 不要在这里自动调用测试，让用户手动触发
        // runFeatureConsistencyTest()
        
        isCacheLoaded = true
        AppLogger.d("用户数据已加载到缓存，共 ${cachedUsers.size} 条，预转换特征 ${cachedFeatures.size} 个")
    }
    
    fun runFeatureConsistencyTestAsync() {
        if (isTesting.getAndSet(true)) {
            AppLogger.w("FaceRecognitionManager: 测试正在运行中，跳过重复调用")
            return
        }
        
        AppLogger.d("========== FaceRecognitionManager: runFeatureConsistencyTestAsync 被调用 ==========")
        scope.launch {
            try {
                runFeatureConsistencyTestInternal()
            } catch (e: Exception) {
                AppLogger.e("特征一致性测试异常: ${e.message}")
            } finally {
                isTesting.set(false)
            }
        }
    }
    
    private fun runFeatureConsistencyTest() {
        if (isTesting.getAndSet(true)) {
            AppLogger.w("FaceRecognitionManager: 测试正在运行中，跳过")
            return
        }
        scope.launch {
            try {
                runFeatureConsistencyTestInternal()
            } finally {
                isTesting.set(false)
            }
        }
    }
    
    private suspend fun runFeatureConsistencyTestInternal() {
        AppLogger.d("========== FaceRecognitionManager: runFeatureConsistencyTestInternal 开始执行 ==========")
        AppLogger.d("========== 开始新模型特征相似度测试 ==========")
        
        // 清理上次的测试缓存
        val testFacesDir = File(context.cacheDir, "test_faces")
        if (testFacesDir.exists()) {
            testFacesDir.listFiles()?.forEach { it.delete() }
            AppLogger.d("已清理测试缓存目录")
        }
        
        // 测试识别结果中相似度超过81%的10个用户
        val testUserNames = listOf(
            "陈阳", "李榕洲", "刘老师", "谢君浩", "戴熙妍", 
            "李承凯", "李晨浩", "许成韬", "江逸朋", "姜立哲"
        )
        
        // 确保MediaPipe模型已加载
        AppLogger.d("特征测试: 开始加载MediaPipe模型...")
        faceEmbeddingManager.ensureModelLoaded()
        AppLogger.d("特征测试: MediaPipe模型加载完成")
        
        // 直接从数据库获取这10个用户的头像URL
        val testUsersWithFeatures = mutableListOf<Pair<String, String>>() // name to faceUrl
        
        for (userName in testUserNames) {
            val user = userDao.getUserByName(userName)
            if (user == null) {
                AppLogger.d("特征测试: 数据库中未找到用户 $userName")
                continue
            }
            
            if (user.faceUrl == null) {
                AppLogger.d("特征测试: 用户 $userName 没有头像")
                continue
            }
            
            testUsersWithFeatures.add(userName to user.faceUrl)
            AppLogger.d("特征测试: 找到用户 $userName, faceUrl=${user.faceUrl}")
        }
        
        AppLogger.d("找到 ${testUsersWithFeatures.size} 个用户的头像URL")
        
        // 清除之前测试的人脸图片
        faceFeatureManager.clearTestFaceImages()
        
        // 用新模型提取这10个用户的特征
        val extractedFeatures = mutableListOf<Pair<String, FloatArray>>()
        val extractTimes = mutableMapOf<String, Long>()
        
        // 处理所有10个用户
        for ((userName, faceUrl) in testUsersWithFeatures) {
            try {
                AppLogger.d("特征测试: 提取用户 $userName 的特征, URL=$faceUrl")
                val startTime = System.currentTimeMillis()
                
                // 使用 faceFeatureManager 下载并提取特征（同时保存裁剪人脸）
                val feature = faceFeatureManager.extractFaceFeatureFromUrlWithName(faceUrl, userName)
                
                val endTime = System.currentTimeMillis()
                val extractTime = endTime - startTime
                extractTimes[userName] = extractTime
                
                // 立即保存当前进度，让UI可以实时显示
                testExtractTimes = extractTimes.toMap()
                
                if (feature != null) {
                    extractedFeatures.add(userName to feature)
                    testFeatures = extractedFeatures.toList()
                    val seconds = extractTime / 1000.0
                    _testStatus.value = "已提取 ${extractedFeatures.size}/10 用户特征"
                    AppLogger.d("特征测试: $userName 特征提取成功, 维度=${feature.size}, 耗时=${extractTime}ms (${String.format("%.1f", seconds)}s)")
                } else {
                    AppLogger.e("特征测试: 特征生成失败 for $userName")
                }
            } catch (e: Exception) {
                AppLogger.e("特征测试: 用户 $userName 异常: ${e.message}")
            }
        }
        
        AppLogger.d("特征测试: ===== 10个用户特征提取完成 =====")
        _testStatus.value = "10个用户特征提取完成"
    }
    
    private fun buildKdTreeIndex() {
        try {
            AppLogger.d("========== 开始构建KD-Tree索引 ==========")
            
            kdTreeIndex = KdTreeIndex(192)
            kdTreeIndex?.build(cachedFeatures)
            isKdTreeReady = true
            
            AppLogger.d("KD-Tree索引构建完成，共 ${kdTreeIndex?.getSize() ?: 0} 个节点")
            AppLogger.d("========== KD-Tree索引构建完成 ==========")
        } catch (e: Exception) {
            AppLogger.e("构建KD-Tree索引失败: ${e.message}")
            isKdTreeReady = false
        }
    }
    
    fun isCacheLoaded(): Boolean = isCacheLoaded

    private var isProcessing = false
    
    suspend fun recognizeFace(bitmap: Bitmap, face: Face) {
        AppLogger.d("========== recognizeFace: 开始人脸识别流程 ==========")
        AppLogger.d("recognizeFace: isProcessing=$isProcessing, lastRecognizedUserId=$lastRecognizedUserId")
        
        if (isProcessing) {
            AppLogger.d("recognizeFace: 正在处理中，跳过")
            return
        }
        
        val currentTime = System.currentTimeMillis()
        
        if (lastRecognizedUserId != null && (currentTime - lastRecognitionTime) < recognitionCooldown) {
            AppLogger.d("recognizeFace: 冷却期内(${(currentTime - lastRecognitionTime)}ms/${recognitionCooldown}ms)，跳过")
            return
        }

        isProcessing = true
        recognitionStartTime = System.currentTimeMillis()
        _recognitionState.value = RecognitionState(isRecognizing = true)
        AppLogger.d("recognizeFace: 状态设置为识别中，recognitionStartTime=$recognitionStartTime")

        try {
            AppLogger.d("recognizeFace: 调用 ensureModelLoaded")
            faceEmbeddingManager.ensureModelLoaded()
            
            val boundingBox = face.boundingBox
            AppLogger.d("recognizeFace: 人脸区域 left=${boundingBox.left}, top=${boundingBox.top}, width=${boundingBox.width()}, height=${boundingBox.height()}")
            
            val faceBitmap = Bitmap.createBitmap(
                bitmap,
                boundingBox.left.coerceAtLeast(0),
                boundingBox.top.coerceAtLeast(0),
                boundingBox.width().coerceAtMost(bitmap.width - boundingBox.left),
                boundingBox.height().coerceAtMost(bitmap.height - boundingBox.top)
            )
            AppLogger.d("recognizeFace: 裁剪人脸图片 ${faceBitmap.width}x${faceBitmap.height}")

            AppLogger.d("recognizeFace: 调用 generateEmbedding")
            val capturedFeature = faceEmbeddingManager.generateEmbedding(face, faceBitmap)
            AppLogger.d("recognizeFace: 特征提取结果=${if (capturedFeature != null) "成功(${capturedFeature.size}维)" else "失败"}")
            
            if (capturedFeature != null) {
                AppLogger.d("recognizeFace: 捕获特征前20值: ${capturedFeature.sliceArray(0..19).contentToString()}")
                AppLogger.d("recognizeFace: 捕获特征统计 min=${capturedFeature.minOrNull()}, max=${capturedFeature.maxOrNull()}, avg=${String.format("%.6f", capturedFeature.average())}")
                
                // 打印缓存中某个用户的特征进行对比
                if (cachedFeatures.isNotEmpty()) {
                    val firstUserId = cachedFeatures.keys.first()
                    val firstFeature = cachedFeatures[firstUserId]
                    val firstUser = cachedUsers.find { it.id == firstUserId }
                    if (firstFeature != null) {
                        val testSimilarity = faceEmbeddingManager.cosineSimilarity(capturedFeature, firstFeature)
                        AppLogger.d("recognizeFace: 与缓存第一个用户(${firstUser?.name})的相似度=${String.format("%.4f", testSimilarity)}")
                    }
                }
                
                scope.launch {
                    AppLogger.d("recognizeFace: 启动协程进行匹配")
                    matchWithDatabase(capturedFeature)
                    isProcessing = false
                    AppLogger.d("recognizeFace: 匹配完成，isProcessing=false")
                }
            } else {
                AppLogger.w("recognizeFace: 无法提取人脸特征")
                isProcessing = false
                _recognitionState.value = RecognitionState(error = "无法提取人脸特征")
            }
        } catch (e: Exception) {
            AppLogger.e("recognizeFace: 识别异常 ${e.message}")
            isProcessing = false
            _recognitionState.value = RecognitionState(error = "识别异常: ${e.message}")
        }
    }

    private suspend fun matchWithDatabase(capturedFeature: FloatArray) = withContext(Dispatchers.IO) {
        try {
            AppLogger.d("========== matchWithDatabase: 开始匹配流程 ==========")
            AppLogger.d("matchWithDatabase: isCacheLoaded=$isCacheLoaded, isKdTreeReady=$isKdTreeReady")
            AppLogger.d("matchWithDatabase: cachedUsers.size=${cachedUsers.size}, cachedFeatures.size=${cachedFeatures.size}")
            
            var bestMatch: UserEntity? = null
            var bestSimilarity = 0f
            var secondBestSimilarity = 0f
            var searchedCount = 0
            
            // 检查刘老师是否在缓存中
            val liuLaoShi = cachedUsers.find { it.name == "刘老师" }
            if (liuLaoShi != null) {
                val hasFeature = cachedFeatures.containsKey(liuLaoShi.id)
                AppLogger.d("matchWithDatabase: 刘老师在缓存中: id=${liuLaoShi.id}, hasFeature=$hasFeature")
                if (hasFeature) {
                    val liuFeature = cachedFeatures[liuLaoShi.id]
                    AppLogger.d("matchWithDatabase: 刘老师特征前10: ${liuFeature?.sliceArray(0..9)?.contentToString()}")
                    
                    // 直接计算刘老师与捕获特征的相似度
                    val directSimilarity = liuFeature?.let { faceEmbeddingManager.cosineSimilarity(capturedFeature, it) } ?: 0f
                    AppLogger.d("matchWithDatabase: 直接与刘老师比对相似度=${String.format("%.4f", directSimilarity)}")
                }
            } else {
                AppLogger.e("matchWithDatabase: 缓存中没有刘老师!")
            }
            
            // 使用KD-Tree全库搜索
            AppLogger.d("matchWithDatabase: 使用KD-Tree全库搜索")
            
            if (isKdTreeReady && kdTreeIndex != null) {
                val kdResults = kdTreeIndex?.search(capturedFeature, 50) ?: emptyList()
                searchedCount = kdResults.size
                
                // 统计相似度分布
                val over60 = kdResults.count { it.second >= 0.60f }
                val over65 = kdResults.count { it.second >= 0.65f }
                val over70 = kdResults.count { it.second >= 0.70f }
                AppLogger.d("matchWithDatabase: 相似度分布 - 总结果=${kdResults.size}, >=60%: $over60, >=65%: $over65, >=70%: $over70")
                
                // 打印前10个结果
                kdResults.take(10).forEachIndexed { index, (userId, sim) ->
                    val userName = cachedUsers.find { it.id == userId }?.name ?: userId
                    AppLogger.d("matchWithDatabase: 第${index+1}名: $userName, 相似度=${String.format("%.2f", sim * 100)}%")
                }
                
                AppLogger.d("matchWithDatabase: KD-Tree搜索返回 ${kdResults.size} 个结果")
                
                var bestUserId: String? = null
                
                for ((userId, similarity) in kdResults) {
                    if (similarity > bestSimilarity) {
                        secondBestSimilarity = bestSimilarity
                        bestSimilarity = similarity
                        bestUserId = userId
                    } else if (similarity > secondBestSimilarity) {
                        secondBestSimilarity = similarity
                    }
                }
                
                bestMatch = bestUserId?.let { cachedUsers.find { u -> u.id == bestUserId } }
            } else {
                // KD-Tree未就绪，回退到完整遍历
                AppLogger.w("matchWithDatabase: KD-Tree未就绪，回退到完整遍历")
                
                var bestUserId: String? = null
                
                for ((userId, storedFeature) in cachedFeatures) {
                    searchedCount++
                    val similarity = cosineSimilarity(capturedFeature, storedFeature)
                    
                    if (similarity > bestSimilarity) {
                        secondBestSimilarity = bestSimilarity
                        bestSimilarity = similarity
                        bestUserId = userId
                    } else if (similarity > secondBestSimilarity) {
                        secondBestSimilarity = similarity
                    }
                }
                
                bestMatch = bestUserId?.let { cachedUsers.find { u -> u.id == bestUserId } }
            }
            
            AppLogger.d("matchWithDatabase: 搜索完成，搜索了 $searchedCount 个用户")
            
            // 计算差异（用于分析）
            val similarityDiff = bestSimilarity - secondBestSimilarity
            AppLogger.d("MATCH_ANALYSIS: bestMatch=${bestMatch?.name}, bestSimilarity=${String.format("%.2f", bestSimilarity * 100)}%, secondBestSimilarity=${String.format("%.2f", secondBestSimilarity * 100)}%, diff=${String.format("%.2f", similarityDiff * 100)}%")
            
            AppLogger.d("MATCH_RESULT: 遍历完成，bestMatch=${bestMatch?.name}, bestSimilarity=${String.format("%.2f", bestSimilarity * 100)}%, stableMatchCount=$stableMatchCount, lastMatchedUserId=$lastMatchedUserId")

            val threshold = 0.50f  // 阈值50%
            val elapsedTime = System.currentTimeMillis() - recognitionStartTime
            AppLogger.d("MATCH_TIME: elapsedTime=${elapsedTime}ms, recognitionTimeout=${recognitionTimeout}ms")
            
            if (elapsedTime > recognitionTimeout) {
                AppLogger.w("MATCH_TIMEOUT: 识别超时，停止识别")
                _recognitionState.value = RecognitionState(
                    isRecognizing = false,
                    error = "识别超时"
                )
                stableMatchCount = 0
                lastMatchedUserId = null
                return@withContext
            }
            
            AppLogger.d("matchWithDatabase: 设置状态为匹配中，相似度=${String.format("%.2f", bestSimilarity * 100)}%")
            _recognitionState.value = RecognitionState(
                isRecognizing = true,
                confidence = bestSimilarity,
                error = "匹配中... ${String.format("%.0f", bestSimilarity * 100)}%"
            )
            
            // 只用相似度阈值判断，移除差异检查
            AppLogger.d("MATCH_CHECK: bestMatch=${bestMatch != null}, bestSimilarity>=threshold=${bestSimilarity >= threshold}, bestMatchName=${bestMatch?.name}")
            
            if (bestMatch != null && bestSimilarity >= threshold) {
                AppLogger.d("MATCH_SUCCESS: 最佳匹配 ${bestMatch.name} 相似度 ${String.format("%.2f", bestSimilarity * 100)}% >= 阈值 $threshold")
                
                // 累计计数：不要求连续，只要相似度超过阈值就计数+1
                stableMatchCount++
                AppLogger.d("STABLE_MATCH: 累计 ${stableMatchCount}/${requiredStableFrames} 次匹配成功")
                
                if (stableMatchCount >= requiredStableFrames) {
                    AppLogger.d("========== matchWithDatabase: 识别成功 ==========")
                    AppLogger.d("matchWithDatabase: 用户 ${bestMatch.name} 累计 $stableMatchCount 次匹配成功，相似度=${String.format("%.2f", bestSimilarity * 100)}%")
                    lastRecognizedUserId = bestMatch.id
                    lastRecognitionTime = System.currentTimeMillis()
                    stableMatchCount = 0
                    
                    AppLogger.d("matchWithDatabase: 设置识别成功状态，isSuccess=true, isWaitingGesture=true")
                    
                    _recognitionState.value = RecognitionState(
                        isRecognizing = false,
                        recognizedUser = bestMatch,
                        confidence = bestSimilarity,
                        isSuccess = true,
                        isWaitingGesture = true
                    )
                } else {
                    AppLogger.d("matchWithDatabase: 累计匹配次数不足 ${stableMatchCount}/${requiredStableFrames}，继续识别")
                }
            } else {
                AppLogger.w("matchWithDatabase: 未匹配到有效用户")
                if (bestMatch != null) {
                    val reason = if (bestSimilarity < threshold) {
                        "相似度 ${String.format("%.2f", bestSimilarity * 100)}% < 阈值 ${String.format("%.2f", threshold * 100)}%"
                    } else {
                        "未知原因"
                    }
                    AppLogger.w("matchWithDatabase: 最佳匹配 ${bestMatch.name} $reason")
                } else {
                    AppLogger.w("matchWithDatabase: 没有找到匹配的用户")
                }
                // 不重置计数，保留之前的累计次数
                AppLogger.d("matchWithDatabase: 保持累计计数 stableMatchCount=$stableMatchCount")
                lastMatchedUserId = null
            }
        } catch (e: Exception) {
            AppLogger.e("matchWithDatabase: 匹配异常 ${e.message}")
            _recognitionState.value = RecognitionState(error = "匹配失败: ${e.message}")
        }
        AppLogger.d("========== matchWithDatabase: 匹配流程结束 ==========")
    }

    private fun calculateSimilarity(feature1: FloatArray, feature2: ByteArray): Float {
        val storedFeature = byteArrayToFloatArray(feature2)
        AppLogger.d("calculateSimilarity: feature1 dimension=${feature1.size}, feature2 dimension=${storedFeature.size}")
        if (feature1.size > 5 && storedFeature.size > 5) {
            AppLogger.d("calculateSimilarity: feature1 first 5: ${feature1.sliceArray(0..4).contentToString()}")
            AppLogger.d("calculateSimilarity: storedFeature first 5: ${storedFeature.sliceArray(0..4).contentToString()}")
        }
        return faceEmbeddingManager.cosineSimilarity(feature1, storedFeature)
    }
    
    private fun byteArrayToFloatArray(byteArray: ByteArray): FloatArray {
        val floatArray = FloatArray(byteArray.size / 4)
        val buffer = ByteBuffer.wrap(byteArray).order(java.nio.ByteOrder.nativeOrder())
        for (i in floatArray.indices) {
            floatArray[i] = buffer.getFloat(i * 4)
        }
        return floatArray
    }

    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        if (vec1.size != vec2.size) {
            AppLogger.e("cosineSimilarity: 维度不匹配 vec1=${vec1.size}, vec2=${vec2.size}")
            return 0f
        }
        
        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f
        
        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }
        
        val sqrtNorm1 = kotlin.math.sqrt(norm1)
        val sqrtNorm2 = kotlin.math.sqrt(norm2)
        val denominator = sqrtNorm1 * sqrtNorm2
        
        if (denominator > 0f) {
            val similarity = dotProduct / denominator
            AppLogger.d("cosineSimilarity: dotProduct=${String.format("%.4f", dotProduct)}, norm1=${String.format("%.4f", sqrtNorm1)}, norm2=${String.format("%.4f", sqrtNorm2)}, similarity=${String.format("%.4f", similarity)}")
            return similarity
        } else {
            AppLogger.e("cosineSimilarity: denominator=0, 返回0")
            return 0f
        }
    }

    fun processGesture(face: Face) {
        val state = _recognitionState.value
        AppLogger.d("processGesture: isWaitingGesture=${state.isWaitingGesture}")
        
        if (!state.isWaitingGesture) return
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastGestureTime < gestureCooldown) {
            AppLogger.d("processGesture: 手势冷却期内")
            return
        }
        
        val headAngleX = face.headEulerAngleX
        val headAngleY = face.headEulerAngleY
        
        AppLogger.d("processGesture: headAngleX=$headAngleX, headAngleY=$headAngleY, deltaX=${headAngleX - lastHeadAngleX}")
        
        gestureHistory.add(headAngleX)
        if (gestureHistory.size > 10) {
            gestureHistory.removeAt(0)
        }
        
        val deltaX = headAngleX - lastHeadAngleX
        val deltaY = headAngleY - lastHeadAngleY
        
        lastHeadAngleX = headAngleX
        lastHeadAngleY = headAngleY
        lastGestureTime = currentTime
        
        val nodThreshold = 8f  // 降低点头阈值
        
        if (abs(deltaX) > nodThreshold) {
            nodCount++
            AppLogger.d("processGesture: 检测到点头动作 nodCount=$nodCount")
            if (nodCount >= 2) {
                _recognitionState.value = _recognitionState.value.copy(
                    isWaitingGesture = false,
                    gestureType = GestureType.NOD
                )
                AppLogger.d("========== 检测到点头动作 ==========")
            }
        } else {
            nodCount = maxOf(0, nodCount - 1)
        }
    }

    fun close() {
        faceEmbeddingManager.close()
    }

    // 获取完整的相似度矩阵
    fun getSimilarityMatrix(): List<List<Float>> {
        if (testFeatures.isEmpty()) return emptyList()
        
        val matrix = mutableListOf<List<Float>>()
        for (i in testFeatures.indices) {
            val row = mutableListOf<Float>()
            for (j in testFeatures.indices) {
                val sim = cosineSimilarity(testFeatures[i].second, testFeatures[j].second)
                row.add(sim)
            }
            matrix.add(row)
        }
        return matrix
    }

    // 获取测试人脸特征数据（用于诊断界面显示相似度）
    fun getTestFaceData(): List<Triple<String, FloatArray, Float>> {
        AppLogger.d("getTestFaceData: testFeatures.size=${testFeatures.size}")

        if (testFeatures.isEmpty()) {
            AppLogger.w("getTestFaceData: testFeatures为空")
            return emptyList()
        }

        AppLogger.d("getTestFaceData: 用户列表=${testFeatures.map { it.first }}")

        // 计算所有用户两两之间的相似度矩阵
        val result = mutableListOf<Triple<String, FloatArray, Float>>()
        
        AppLogger.d("===== 相似度矩阵 =====")
        for (i in testFeatures.indices) {
            val (name1, feature1) = testFeatures[i]
            for (j in testFeatures.indices) {
                val (name2, feature2) = testFeatures[j]
                val similarity = cosineSimilarity(feature1, feature2)
                if (i == j) {
                    AppLogger.d("getTestFaceData: $name1 vs $name2 = ${String.format("%.2f", similarity)} (本人)")
                } else if (j == i + 1 || j == 0) {
                    AppLogger.d("getTestFaceData: $name1 vs $name2 = ${String.format("%.2f", similarity)}")
                }
            }
        }
        AppLogger.d("=====================")

        // 返回以第一个用户为基准的结果
        val baselineFeature = testFeatures.firstOrNull()?.second ?: return emptyList()
        val baselineName = testFeatures.firstOrNull()?.first ?: "未知"
        
        AppLogger.d("getTestFaceData: 使用 $baselineName 作为基准")
        return testFeatures.map { (name, feature) ->
            val similarity = cosineSimilarity(baselineFeature, feature)
            AppLogger.d("getTestFaceData: $name 相似度=$similarity")
            Triple(name, feature, similarity)
        }
    }

    // 获取测试人脸特征提取时间
    fun getTestExtractTimes(): Map<String, Long> {
        return testExtractTimes
    }
}
