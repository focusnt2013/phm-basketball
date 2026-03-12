package com.smartbasketball.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val title: String,
    val role: String,
    val faceUrl: String?,
    val faceTs: Long,
    val faceFeature: ByteArray?,
    val featureGeneratedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    // 新增：模型信息字段
    val modelType: String?,       // 模型类型，如 "onnx", "tflite"
    val modelVersion: String?,    // 模型版本，如 "1.0"
    val featureDimension: Int?    // 特征维度，如 512, 192
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as UserEntity
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
