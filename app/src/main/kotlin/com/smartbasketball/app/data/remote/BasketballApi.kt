package com.smartbasketball.app.data.remote

import android.util.Base64
import com.smartbasketball.app.data.local.UserEntity
import com.smartbasketball.app.util.ApiKit
import com.smartbasketball.app.util.AppLogger
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SyncFacesResult(
    val errcode: Int,
    val errmsg: String,
    val operators: List<UserEntity>,
    val teachers: List<UserEntity>,
    val students: List<UserEntity>
) {
    val allUsers: List<UserEntity> get() = operators + teachers + students
}

@Singleton
class BasketballApi @Inject constructor() {
    
    companion object {
        private const val BASE_URL = "https://api.xixiti.com"
        // IMEI临时用TEST代替
        const val TEMP_IMEI = "TEST"
    }
    
    suspend fun synchFaces(schoolId: String, timestamp: Long): Result<SyncFacesResult> = withContext(Dispatchers.IO) {
        try {
            val url = ApiKit.getReqUrl("$BASE_URL/basketball/synchfaces")
            val params = JSONObject().apply {
                put("school_id", schoolId)
                put("timestamp", timestamp)
            }
            AppLogger.d("========== 同步人脸接口 ==========")
            AppLogger.d("schoolId: $schoolId, timestamp: $timestamp")
            val response = ApiKit.postJson(url, params.toString().toByteArray())
            AppLogger.d("同步人脸返回: $response")
            
            val errcode = response.optInt("errcode", -1)
            val errmsg = response.optString("errmsg", "")
            
            val operators = parseUsersFromData(response.optJSONArray("operators"))
            val teachers = parseUsersFromData(response.optJSONArray("teachers"))
            val students = parseUsersFromData(response.optJSONArray("students"))
            
            AppLogger.d("解析到 operators: ${operators.size}, teachers: ${teachers.size}, students: ${students.size}")
            
            Result.success(SyncFacesResult(errcode, errmsg, operators, teachers, students))
        } catch (e: Exception) {
            AppLogger.e("同步人脸异常: ${e.message}")
            Result.failure(e)
        }
    }

    private fun parseUsersFromData(data: JSONArray?): List<UserEntity> {
        if (data == null) return emptyList()
        
        val users = mutableListOf<UserEntity>()
        val now = System.currentTimeMillis()
        
        for (i in 0 until data.length()) {
            try {
                val userJson = data.getJSONObject(i)
                val userId = userJson.optString("_id", "") 
                if (userId.isEmpty()) continue
                
                val faceTs = userJson.optLong("face_ts", 0)
                val faceUrl = userJson.optString("face", null)
                
                val user = UserEntity(
                    id = userId,
                    name = userJson.optString("name", ""),
                    title = userJson.optString("title", ""),
                    role = userJson.optString("role", ""),
                    faceUrl = if (faceUrl.isNullOrEmpty()) null else faceUrl,
                    faceTs = faceTs,
                    faceFeature = null,
                    featureGeneratedAt = 0,
                    createdAt = now,
                    updatedAt = now,
                    modelType = null,
                    modelVersion = null,
                    featureDimension = null
                )
                users.add(user)
            } catch (e: Exception) {
                AppLogger.e("解析用户数据失败: ${e.message}")
            }
        }
        
        return users
    }
    
    suspend fun getGameRecords(schoolId: String, page: Int = 1, size: Int = 20): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = ApiKit.getReqUrl("$BASE_URL/basketball/records")
            val params = JSONObject().apply {
                put("schoolId", schoolId)
                put("page", page)
                put("size", size)
            }
            val response = ApiKit.postJson(url, params.toString().toByteArray())
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun uploadGameScore(data: JSONObject): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = ApiKit.getReqUrl("$BASE_URL/basketball/score")
            val response = ApiKit.postJson(url, data.toString().toByteArray())
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserInfo(userId: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = ApiKit.getReqUrl("$BASE_URL/basketball/user/$userId")
            val response = ApiKit.postJson(url, null)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun register(schoolId: String, password: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = ApiKit.getReqUrl("$BASE_URL/basketball/register")
            val params = JSONObject().apply {
                put("db_id", schoolId)
                put("secret", password)
            }
            val response = ApiKit.postJson(url, params.toString().toByteArray())
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recognizeFace(
        schoolId: String,
        type: String = "jpg",
        imei: String = TEMP_IMEI,
        imageBytes: ByteArray
    ): Result<FaceRecogResult> = withContext(Dispatchers.IO) {
        try {
            val url = ApiKit.getReqUrl("$BASE_URL/basketball/facerecog")
            
            AppLogger.d("========== 人脸识别接口 ==========")
            AppLogger.d("schoolId: $schoolId, imei: $imei, imageSize: ${imageBytes.size}")
            
            // 使用postImage上传图片文件
            val params = java.util.HashMap<String, Any>().apply {
                put("school_id", schoolId)
                put("type", type)
                put("imei", imei)
            }
            
            val response = ApiKit.postImage(url, imageBytes, "face.jpg", params)
            AppLogger.d("人脸识别返回: $response")
            
            val errcode = response.optInt("errcode", -1)
            val errmsg = response.optString("errmsg", "")
            
            val faceObj = response.optJSONObject("face")
            val userName = faceObj?.optString("name")
            val userRole = faceObj?.optString("role")
            val userTitle = faceObj?.optString("title")
            val userId = faceObj?.optString("_id")
            val similarity = response.optDouble("simi", 0.0).toFloat()
            val faceId = response.optString("face_id")
            
            Result.success(FaceRecogResult(
                errcode = errcode,
                errmsg = errmsg,
                userName = userName,
                userRole = userRole,
                userTitle = userTitle,
                userId = userId,
                similarity = similarity,
                faceId = faceId
            ))
        } catch (e: Exception) {
            AppLogger.e("人脸识别异常: ${e.message}")
            Result.failure(e)
        }
    }
}

data class FaceRecogResult(
    val errcode: Int,
    val errmsg: String?,
    val userName: String?,
    val userRole: String?,
    val userTitle: String?,
    val userId: String?,
    val similarity: Float,
    val faceId: String?
)
