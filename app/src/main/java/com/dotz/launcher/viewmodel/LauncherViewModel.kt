package com.dotz.launcher.viewmodel

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dotz.launcher.data.*
import com.dotz.launcher.services.DotzNotificationService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WeatherData(
    val temp: String = "28°C",
    val description: String = "Cloudy"
)

/**
 * UI State for the Launcher screen.
 *
 * @property page0Tiles List of apps for the first page.
 * @property page1Tiles List of apps for the second page.
 * @property settings Current launcher settings from DataStore.
 * @property batteryLevel Current battery percentage (0-100).
 * @property networkStatus Current network type (WiFi, LTE, etc.).
 * @property isWifiEnabled Whether Wi-Fi is currently enabled.
 * @property isBluetoothEnabled Whether Bluetooth is currently enabled.
 * @property isSilentMode Whether the device is in silent or vibrate mode.
 * @property isTorchOn Whether the flashlight is currently on.
 * @property isAirplaneModeOn Whether airplane mode is currently enabled.
 * @property isDarkModeOn Whether system dark mode is active.
 * @property isDefaultLauncher Whether Dotz is currently set as the default home app.
 * @property weather Current weather information.
 */
data class LauncherUiState(
    val page0Tiles: List<AppTile> = DefaultApps.page0Defaults,
    val page1Tiles: List<AppTile> = DefaultApps.page1Defaults,
    val settings: DotzSettings = DotzSettings(),
    val batteryLevel: Int = -1,
    val networkStatus: String = "None",
    val isWifiEnabled: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val isSilentMode: Boolean = false,
    val isTorchOn: Boolean = false,
    val isAirplaneModeOn: Boolean = false,
    val isDarkModeOn: Boolean = false,
    val isDefaultLauncher: Boolean = false,
    val weather: WeatherData = WeatherData()
)

/**
 * Main ViewModel for the Launcher.
 * Handles app logic, system toggles, and state coordination between DataStore,
 * system events, and the UI.
 *
 * @param application The application instance.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = DotzPreferencesRepository(application)
    private val systemStateManager = SystemStateManager(application)
    private val pm: PackageManager = application.packageManager
    
    /** Cache manager for app icons to ensure smooth scrolling. */
    val iconCache = IconCacheManager(application)

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(System.currentTimeMillis())

    private val _isDefaultLauncher = _refreshTrigger.map {
        isDefaultLauncher()
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _tiles = combine(
        prefs.settingsFlow,
        DotzNotificationService.notificationCounts,
        _refreshTrigger
    ) { settings, notifCounts, _ ->
        val p0 = buildTiles(DefaultApps.page0Defaults, settings, notifCounts)
        val p1 = buildTiles(DefaultApps.page1Defaults, settings, notifCounts)
        p0 to p1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DefaultApps.page0Defaults to DefaultApps.page1Defaults)

    init {
        // Main UI State combination
        viewModelScope.launch {
            combine(
                prefs.settingsFlow,
                _tiles,
                systemStateManager.batteryLevel,
                systemStateManager.networkStatus,
                systemStateManager.isWifiEnabled,
                systemStateManager.isBluetoothEnabled,
                systemStateManager.isSilentMode,
                systemStateManager.isTorchOn,
                systemStateManager.isAirplaneModeOn,
                systemStateManager.isDarkModeOn,
                _isDefaultLauncher
            ) { args ->
                val settings = args[0] as DotzSettings
                val (p0, p1) = args[1] as Pair<List<AppTile>, List<AppTile>>
                LauncherUiState(
                    page0Tiles = p0,
                    page1Tiles = p1,
                    settings = settings,
                    batteryLevel = args[2] as Int,
                    networkStatus = args[3] as String,
                    isWifiEnabled = args[4] as Boolean,
                    isBluetoothEnabled = args[5] as Boolean,
                    isSilentMode = args[6] as Boolean,
                    isTorchOn = args[7] as Boolean,
                    isAirplaneModeOn = args[8] as Boolean,
                    isDarkModeOn = args[9] as Boolean,
                    isDefaultLauncher = args[10] as Boolean
                )

            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        systemStateManager.unregisterListeners()
    }

    // ── System Toggles ────────────────────────────────────────────────────────

    fun toggleWifi() = systemStateManager.toggleWifi()
    fun toggleBluetooth() = systemStateManager.toggleBluetooth()
    fun toggleSilentMode() = systemStateManager.toggleSilentMode()
    fun toggleTorch() = systemStateManager.toggleTorch()
    fun toggleAirplaneMode() = systemStateManager.toggleAirplaneMode()
    fun toggleDarkMode() = systemStateManager.toggleDarkMode()
    fun openMobileDataSettings() = systemStateManager.openMobileDataSettings()

    /**
     * Opens the system settings to set Dotz as the default launcher.
     */
    fun openDefaultLauncherSettings() {
        val app = getApplication<Application>()
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.ACTION_HOME_SETTINGS)
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            app.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS)
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(fallback)
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val res = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val currentDefault = res?.activityInfo?.packageName
        val myPackage = getApplication<Application>().packageName
        
        return when (currentDefault) {
            null, "android", "com.android.settings", "com.google.android.permissioncontroller" -> false
            else -> currentDefault == myPackage
        }
    }

    // ── Logic ────────────────────────────────────────────────────────────

    /**
     * Manually triggers a refresh of the UI state.
     */
    fun refreshState() {
        systemStateManager.refreshInitialState()
        _refreshTrigger.value = System.currentTimeMillis()
    }

    // ── App Logic ─────────────────────────────────────────────────────────────

    private fun buildTiles(
        defaults: List<AppTile>,
        settings: DotzSettings,
        notifCounts: Map<String, Int>
    ): List<AppTile> {
        return defaults.map { tile ->
            val pkg = settings.tileOverrides[tile.tileId] ?: resolvePackage(tile.packageName)
            val label = settings.tileLabels[tile.tileId] ?: tile.label
            val installed = isInstalled(pkg) || pkg == getApplication<Application>().packageName
            val count = if (settings.showNotificationDots) {
                val raw = notifCounts[pkg] ?: -1
                if (raw > 0 && settings.showNumericalCounts && DefaultApps.numericBadgePackages.contains(pkg)) {
                    raw
                } else if (raw >= 0) {
                    0
                } else {
                    -1
                }
            } else {
                -1
            }
            tile.copy(packageName = pkg, label = label, badgeCount = count, isInstalled = installed)
        }
    }

    private fun resolvePackage(preferred: String): String {
        if (isInstalled(preferred)) return preferred
        DefaultApps.packageFallbacks[preferred]?.forEach { fallback ->
            if (isInstalled(fallback)) return fallback
        }
        return preferred
    }

    private fun isInstalled(pkg: String): Boolean = try {
        pm.getPackageInfo(pkg, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) { false }

    /**
     * Called when a tile is tapped. Clears notifications for the package.
     */
    fun onTileTapped(tile: AppTile) {
        DotzNotificationService.clearBadge(tile.packageName)
        DotzNotificationService.cancelNotificationsForPackage(tile.packageName)
    }

    /**
     * Updates a tile assignment.
     */
    fun updateTileOverride(tileId: Int, packageName: String, label: String) {
        viewModelScope.launch {
            prefs.setTileOverride(tileId, packageName, label)
        }
    }

    // ── Preference Updates ───────────────────────────────────────────────────

    fun setShowNotificationDots(value: Boolean) = viewModelScope.launch { prefs.setShowNotificationDots(value) }
    fun setShowNumericalCounts(value: Boolean) = viewModelScope.launch { prefs.setShowNumericalCounts(value) }
    fun setNotificationFilterEnabled(value: Boolean) = viewModelScope.launch { prefs.setNotificationFilterEnabled(value) }
    fun setDynamicBackgroundEnabled(value: Boolean) = viewModelScope.launch { prefs.setDynamicBackgroundEnabled(value) }
    fun setTileOpacity(value: Float) = viewModelScope.launch { prefs.setTileOpacity(value) }
    fun setGrayscaleMode(value: Boolean) = viewModelScope.launch {
        prefs.setGrayscaleMode(value)
        iconCache.clearCache()
    }
    fun setVerticalScrolling(value: Boolean) = viewModelScope.launch { prefs.setVerticalScrolling(value) }
    fun setShowWeatherInfo(value: Boolean) = viewModelScope.launch { prefs.setShowWeatherInfo(value) }
    fun setIconPackPackage(value: String?) = viewModelScope.launch {
        prefs.setIconPackPackage(value)
        iconCache.clearCache()
    }

    suspend fun exportSettings(): String = prefs.exportSettings()
    suspend fun importSettings(json: String): Boolean = prefs.importSettings(json)

    /**
     * Returns a list of all installed apps with launcher category.
     */
    fun getInstalledApps(): List<Pair<String, String>> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .map { it.activityInfo.packageName to (it.loadLabel(pm).toString()) }
            .distinctBy { it.first }
            .sortedBy { it.second }
            .toList()
    }

    /**
     * Returns a list of installed apps filtered by tile category keywords/intents.
     */
    fun getInstalledAppsForTile(tileId: Int): List<Pair<String, String>> {
        val allApps = getInstalledApps()
        
        val filtered = when (tileId) {
            0 -> filterByIntent(allApps, Intent(Intent.ACTION_DIAL)) + 
                 filterByIntent(allApps, Intent(Intent.ACTION_VIEW).apply { data = "tel:".toUri() }) +
                 filterByKeywords(allApps, listOf("phone", "dialer", "call", "contact"))
            1 -> filterByKeywords(allApps, listOf("chat", "whatsapp", "telegram", "signal", "discord", "viber", "messenger", "social", "facebook", "insta"))
            2 -> filterByIntent(allApps, Intent(Intent.ACTION_SENDTO).apply { data = "smsto:".toUri() }) +
                 filterByKeywords(allApps, listOf("messag", "sms", "mms", "text"))
            3 -> filterByIntent(allApps, Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MAPS) }) +
                 filterByKeywords(allApps, listOf("map", "navig", "gps", "waze", "uber", "lyft"))
            4 -> filterByIntent(allApps, Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MUSIC) }) +
                 filterByIntent(allApps, Intent("android.intent.action.MUSIC_PLAYER")) +
                 filterByKeywords(allApps, listOf("music", "audio", "player", "spotify", "sound", "radio", "podcast", "yt music", "youtube music"))
            5 -> filterByKeywords(allApps, listOf("pay", "wallet", "bank", "finance", "cash", "money", "card", "crypto", "binance", "paypal"))
            6 -> filterByIntent(allApps, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) +
                 filterByKeywords(allApps, listOf("camera", "cam", "lens"))
            7 -> filterByIntent(allApps, Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALCULATOR) }) +
                 filterByKeywords(allApps, listOf("calc"))
            8 -> filterByKeywords(allApps, listOf("clock", "alarm", "timer", "watch"))
            9 -> filterByIntent(allApps, Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALENDAR) }) +
                 filterByKeywords(allApps, listOf("calen"))
            10 -> filterByKeywords(allApps, listOf("note", "keep", "memo", "todo", "sticky", "journal", "list", "writ"))
            else -> allApps
        }

        val result = filtered.asSequence().distinctBy { it.first }.sortedBy { it.second }.toList()
        return result.ifEmpty { allApps }
    }

    private fun filterByIntent(apps: List<Pair<String, String>>, intent: Intent): List<Pair<String, String>> {
        val resolved = pm.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }.toSet()
        return apps.filter { resolved.contains(it.first) }
    }

    private fun filterByKeywords(apps: List<Pair<String, String>>, keywords: List<String>): List<Pair<String, String>> {
        return apps.filter { (pkg, label) ->
            keywords.any { kw -> 
                label.contains(kw, ignoreCase = true) || pkg.contains(kw, ignoreCase = true)
            }
        }
    }

    /**
     * Returns a list of installed icon packs.
     */
    fun getInstalledIconPacks(): List<Pair<String, String>> {
        val iconPacks = mutableListOf<Pair<String, String>>()
        val intents = listOf(
            Intent("com.novalauncher.THEME"),
            Intent("org.adw.launcher.THEMES")
        )
        
        for (intent in intents) {
            val infos = pm.queryIntentActivities(intent, 0)
            for (info in infos) {
                val pkg = info.activityInfo.packageName
                if (iconPacks.none { it.first == pkg }) {
                    iconPacks.add(pkg to info.loadLabel(pm).toString())
                }
            }
        }

        return iconPacks.sortedBy { it.second }
    }
}
