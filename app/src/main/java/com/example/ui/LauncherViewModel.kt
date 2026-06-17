package com.example.ui

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppConfig
import com.example.data.LauncherDatabase
import com.example.data.LauncherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LauncherApp(
    val packageName: String,
    val label: String,
    val customLabel: String? = null,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val favoriteOrder: Int = 0,
    val systemIcon: android.graphics.drawable.Drawable? = null
) {
    val displayName: String get() = customLabel ?: label
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val repository: LauncherRepository

    // Real-time ticking Clock details
    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentDate = MutableStateFlow("")
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    // System stats details
    private val _systemStats = MutableStateFlow(Triple("", "", "")) // RAM, Storage, Network
    val systemStats: StateFlow<Triple<String, String, String>> = _systemStats.asStateFlow()

    // Wallpaper Theme selection
    private val _wallpaperStyle = MutableStateFlow("cinematic")
    val wallpaperStyle: StateFlow<String> = _wallpaperStyle.asStateFlow()

    // Internal list of RAW system-installed launcher activities
    private val _installedAppsRaw = MutableStateFlow<List<RawAppInfo>>(emptyList())

    init {
        val dao = LauncherDatabase.getDatabase(context).launcherDao()
        repository = LauncherRepository(dao)

        startClockTicker()
        startSystemStatsTicker()
        loadInstalledApps()
        loadSettings()
    }

    // Combine raw installed app list with custom app configurations from Room Database!
    val allApps: StateFlow<List<LauncherApp>> = combine(
        _installedAppsRaw,
        repository.allAppConfigsFlow
    ) { rawApps, dbConfigs ->
        val configMap = dbConfigs.associateBy { it.packageName }
        rawApps.map { raw ->
            val dbConfig = configMap[raw.packageName]
            LauncherApp(
                packageName = raw.packageName,
                label = raw.label,
                customLabel = dbConfig?.customLabel,
                isFavorite = dbConfig?.isFavorite ?: false,
                isHidden = dbConfig?.isHidden ?: false,
                favoriteOrder = dbConfig?.favoriteOrder ?: 0,
                systemIcon = raw.systemIcon
            )
        }.sortedBy { it.displayName.lowercase(Locale.ROOT) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteApps: StateFlow<List<LauncherApp>> = combine(
        allApps,
        repository.allAppConfigsFlow
    ) { apps, _ ->
        apps.filter { it.isFavorite && !it.isHidden }
            .sortedWith(compareBy<LauncherApp> { it.favoriteOrder }.thenBy { it.displayName.lowercase(Locale.ROOT) })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun startClockTicker() {
        viewModelScope.launch(Dispatchers.Default) {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val dateFormat = SimpleDateFormat("EEEE, dd 'tháng' MM, yyyy", Locale("vi", "VN"))
            while (true) {
                val now = Date()
                _currentTime.value = timeFormat.format(now)
                _currentTime.value = timeFormat.format(now)
                _currentDate.value = dateFormat.format(now)
                delay(1000)
            }
        }
    }

    private fun startSystemStatsTicker() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val ram = getMemoryInfo(context)
                val strg = getStorageInfo()
                val net = getNetworkInfo(context)
                _systemStats.value = Triple(ram, strg, net)
                delay(5000)
            }
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val pm = context.packageManager
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
                val ourPackage = context.packageName

                val rawList = resolveInfos.asSequence()
                    .map { resolveInfo ->
                        val packageName = resolveInfo.activityInfo.packageName
                        val label = resolveInfo.loadLabel(pm).toString()
                        val systemIcon = resolveInfo.loadIcon(pm)
                        RawAppInfo(packageName, label, systemIcon)
                    }
                    .filter { it.packageName != ourPackage }
                    .distinctBy { it.packageName }
                    .toList()

                _installedAppsRaw.value = rawList
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val style = repository.getSetting("wallpaper_style") ?: "cinematic"
            _wallpaperStyle.value = style
        }
    }

    fun setWallpaperStyle(style: String) {
        viewModelScope.launch {
            _wallpaperStyle.value = style
            repository.saveSetting("wallpaper_style", style)
        }
    }

    fun toggleFavorite(packageName: String) {
        viewModelScope.launch {
            val existing = repository.getAppConfig(packageName)
            val currentMaxOrder = repository.getAllAppConfigs()
                .filter { it.isFavorite }
                .maxOfOrNull { it.favoriteOrder } ?: 0

            val newConfig = AppConfig(
                packageName = packageName,
                customLabel = existing?.customLabel,
                isFavorite = !(existing?.isFavorite ?: false),
                isHidden = existing?.isHidden ?: false,
                favoriteOrder = if (existing?.isFavorite == true) 0 else currentMaxOrder + 1
            )
            repository.saveAppConfig(newConfig)
        }
    }

    fun moveFavorite(packageName: String, moveUp: Boolean) {
        viewModelScope.launch {
            val favs = favoriteApps.value
            val index = favs.indexOfFirst { it.packageName == packageName }
            if (index == -1) return@launch

            val targetIndex = if (moveUp) index - 1 else index + 1
            if (targetIndex in favs.indices) {
                val currentApp = favs[index]
                val targetApp = favs[targetIndex]

                val currentConfig = repository.getAppConfig(currentApp.packageName) ?: AppConfig(currentApp.packageName)
                val targetConfig = repository.getAppConfig(targetApp.packageName) ?: AppConfig(targetApp.packageName)

                val tempOrder = currentConfig.favoriteOrder
                repository.saveAppConfig(currentConfig.copy(favoriteOrder = targetConfig.favoriteOrder))
                repository.saveAppConfig(targetConfig.copy(favoriteOrder = tempOrder))
            }
        }
    }

    fun toggleHidden(packageName: String) {
        viewModelScope.launch {
            val existing = repository.getAppConfig(packageName)
            val newConfig = AppConfig(
                packageName = packageName,
                customLabel = existing?.customLabel,
                isFavorite = existing?.isFavorite ?: false,
                isHidden = !(existing?.isHidden ?: false),
                favoriteOrder = existing?.favoriteOrder ?: 0
            )
            repository.saveAppConfig(newConfig)
        }
    }

    fun renameApp(packageName: String, customName: String?) {
        viewModelScope.launch {
            val existing = repository.getAppConfig(packageName)
            val newConfig = AppConfig(
                packageName = packageName,
                customLabel = if (customName.isNullOrBlank()) null else customName.trim(),
                isFavorite = existing?.isFavorite ?: false,
                isHidden = existing?.isHidden ?: false,
                favoriteOrder = existing?.favoriteOrder ?: 0
            )
            repository.saveAppConfig(newConfig)
        }
    }

    fun resetLauncherSettings() {
        viewModelScope.launch {
            repository.resetAll()
            _wallpaperStyle.value = "cinematic"
        }
    }

    fun launchApp(context: Context, packageName: String) {
        try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Không thể khởi chạy ứng dụng này", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Lỗi khi khởi chạy: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun uninstallApp(context: Context, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Lỗi khi mở giao diện gỡ cài đặt", Toast.LENGTH_SHORT).show()
        }
    }

    fun openAppDetails(context: Context, packageName: String) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Lỗi khi mở phần thông tin ứng dụng", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getStorageInfo(): String {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            val freeGb = (availableBlocks * blockSize) / (1024f * 1024f * 1024f)
            val totalGb = (totalBlocks * blockSize) / (1024f * 1024f * 1024f)
            String.format("Trống %.1f GB / %.1f GB OS", freeGb, totalGb)
        } catch (e: Exception) {
            "Trống N/A GB / N/A GB OS"
        }
    }

    private fun getMemoryInfo(context: Context): String {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            val availGb = memInfo.availMem / (1024f * 1024f * 1024f)
            val totalGb = memInfo.totalMem / (1024f * 1024f * 1024f)
            val usedGb = totalGb - availGb
            String.format("RAM: %.1f GB / %.1f GB", usedGb, totalGb)
        } catch (e: Exception) {
            "RAM: N/A"
        }
    }

    private fun getNetworkInfo(context: Context): String {
        return try {
            val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connManager.activeNetwork
            val capabilities = connManager.getNetworkCapabilities(activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi: Đang kết nối"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cáp Di động"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Cáp Ethernet LAN"
                    else -> "Đã kết nối LAN"
                }
            } else {
                "Mạng: Ngoại tuyến"
            }
        } catch (e: Exception) {
            "Mạng: Ngoại tuyến"
        }
    }
}

data class RawAppInfo(
    val packageName: String,
    val label: String,
    val systemIcon: android.graphics.drawable.Drawable
)
