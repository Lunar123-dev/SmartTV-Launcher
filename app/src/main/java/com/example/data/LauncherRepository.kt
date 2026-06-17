package com.example.data

import kotlinx.coroutines.flow.Flow

class LauncherRepository(private val dao: LauncherDao) {
    val allAppConfigsFlow: Flow<List<AppConfig>> = dao.getAllAppConfigsFlow()
    val allSettingsFlow: Flow<List<LauncherSetting>> = dao.getAllSettingsFlow()

    suspend fun getAllAppConfigs(): List<AppConfig> = dao.getAllAppConfigs()

    suspend fun getAppConfig(packageName: String): AppConfig? = dao.getAppConfig(packageName)

    suspend fun saveAppConfig(config: AppConfig) = dao.insertAppConfig(config)

    suspend fun getSetting(key: String): String? = dao.getSetting(key)?.value

    suspend fun saveSetting(key: String, value: String) {
        dao.insertSetting(LauncherSetting(key, value))
    }

    suspend fun resetAll() {
        dao.clearConfigs()
        dao.clearSettings()
    }
}
