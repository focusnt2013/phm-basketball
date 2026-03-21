package com.smartbasketball.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsersList(): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE name = :userName")
    suspend fun getUserByName(userName: String): UserEntity?

    @Query("SELECT * FROM users WHERE faceUrl IS NOT NULL AND (faceFeature IS NULL OR featureGeneratedAt < faceTs)")
    suspend fun getUsersNeedFeatureGeneration(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET faceFeature = :feature, featureGeneratedAt = :generatedAt, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateFaceFeature(userId: String, feature: ByteArray, generatedAt: Long, updatedAt: Long)

    @Query("UPDATE users SET faceFeature = NULL, featureGeneratedAt = 0 WHERE id = :userId")
    suspend fun clearFaceFeature(userId: String)
    
    @Query("UPDATE users SET faceFeature = NULL, featureGeneratedAt = 0")
    suspend fun clearAllFaceFeatures()

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query("SELECT COUNT(*) FROM users WHERE faceFeature IS NOT NULL")
    suspend fun getValidFaceCount(): Int

    @Query("SELECT * FROM users WHERE faceFeature IS NOT NULL")
    suspend fun getUsersWithFeatures(): List<UserEntity>
}
