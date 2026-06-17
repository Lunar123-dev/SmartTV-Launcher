package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "app_configs")
data class AppConfig(
    @PrimaryKey val packageName: String,
    val customLabel: String? = null,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val favoriteOrder: Int = 0
)

@Entity(tableName = "launcher_settings")
data class LauncherSetting(
    @PrimaryKey val key: String,
    val value: String
)

@Dao
interface LauncherDao {
    @Query("SELECT * FROM app_configs")
    fun getAllAppConfigsFlow(): Flow<List<AppConfig>>

    @Query("SELECT * FROM app_configs")
    suspend fun getAllAppConfigs(): List<AppConfig>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppConfig(config: AppConfig)

    @Query("SELECT * FROM app_configs WHERE packageName = :packageName")
    suspend fun getAppConfig(packageName: String): AppConfig?

    @Query("SELECT * FROM launcher_settings")
    fun getAllSettingsFlow(): Flow<List<LauncherSetting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: LauncherSetting)

    @Query("SELECT * FROM launcher_settings WHERE `key` = :key")
    suspend fun getSetting(key: String): LauncherSetting?

    @Query("DELETE FROM app_configs")
    suspend fun clearConfigs()

    @Query("DELETE FROM launcher_settings")
    suspend fun clearSettings()
}
