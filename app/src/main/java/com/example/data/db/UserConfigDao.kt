package com.example.data.db

import androidx.room.*
import com.example.data.model.UserConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface UserConfigDao {
    @Query("SELECT * FROM user_config WHERE userId = :userId LIMIT 1")
    fun getUserConfig(userId: String): Flow<UserConfig?>

    @Query("SELECT * FROM user_config WHERE userId = :userId LIMIT 1")
    suspend fun getUserConfigOnce(userId: String): UserConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserConfig(config: UserConfig)
}
