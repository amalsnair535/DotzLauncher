package com.dotz.launcher.data

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
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
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages and monitors system-level states such as battery, connectivity, and hardware toggles.
 *
 * This class centralizes the logic for listening to system broadcasts and callbacks,
 * providing a unified interface for the launcher to react to environment changes.
 *
 * @param application The application context used for system service access and receiver registration.
 */
class SystemStateManager(private val application: Application) {

    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = application.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val wifiManager = application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _batteryLevel = MutableStateFlow(-1)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _networkStatus = MutableStateFlow("None")
    val networkStatus: StateFlow<String> = _networkStatus.asStateFlow()

    private val _isWifiEnabled = MutableStateFlow(false)
    val isWifiEnabled: StateFlow<Boolean> = _isWifiEnabled.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _isSilentMode = MutableStateFlow(false)
    val isSilentMode: StateFlow<Boolean> = _isSilentMode.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _isAirplaneModeOn = MutableStateFlow(false)
    val isAirplaneModeOn: StateFlow<Boolean> = _isAirplaneModeOn.asStateFlow()

    private val _isDarkModeOn = MutableStateFlow(false)
    val isDarkModeOn: StateFlow<Boolean> = _isDarkModeOn.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if ((level != -1) && (scale != -1)) {
                _batteryLevel.value = ((level * 100) / scale.toFloat()).toInt()
            }
        }
    }

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    _isWifiEnabled.value = wifiManager.isWifiEnabled
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
                }
                AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                    _isSilentMode.value = audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL
                }
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                    _isAirplaneModeOn.value = intent.getBooleanExtra("state", false)
                }
            }
        }
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            _isTorchOn.value = enabled
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { updateNetwork() }
        override fun onLost(network: Network) { updateNetwork() }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { updateNetwork() }

        private fun updateNetwork() {
            val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            _networkStatus.value = when {
                caps == null -> "None"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "LTE"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Eth"
                else -> "Online"
            }
        }
    }

    init {
        registerListeners()
        refreshInitialState()
    }

    /**
     * Registers system receivers and callbacks.
     */
    fun registerListeners() {
        // Register Battery Receiver
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(batteryReceiver, batteryFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            application.registerReceiver(batteryReceiver, batteryFilter)
        }

        // Register System Toggles Receiver
        val systemFilter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        }
        application.registerReceiver(systemReceiver, systemFilter)

        // Torch state tracking
        try {
            cameraManager.registerTorchCallback(torchCallback, null)
        } catch (e: Exception) { e.printStackTrace() }

        // Register Network Callback
        val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            cm.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback)
        } catch (e: Exception) { e.printStackTrace() }
    }

    /**
     * Unregisters system receivers and callbacks to prevent memory leaks.
     */
    fun unregisterListeners() {
        try {
            application.unregisterReceiver(batteryReceiver)
            application.unregisterReceiver(systemReceiver)
            cameraManager.unregisterTorchCallback(torchCallback)
        } catch (e: Exception) { e.printStackTrace() }

        val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            cm.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) { e.printStackTrace() }
    }

    /**
     * Refreshes the initial state of all monitored properties.
     */
    fun refreshInitialState() {
        _isWifiEnabled.value = wifiManager.isWifiEnabled
        _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
        _isSilentMode.value = audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL
        _isAirplaneModeOn.value = Settings.Global.getInt(application.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        _isDarkModeOn.value = (application.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    // ── Toggles ──────────────────────────────────────────────────────────────

    /**
     * Toggles Wi-Fi state.
     * Note: On Android 10+ (API 29+), this opens the settings panel.
     */
    fun toggleWifi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(Settings.Panel.ACTION_WIFI)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            application.startActivity(intent)
        } else {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
        }
    }

    /**
     * Opens Bluetooth settings.
     */
    fun toggleBluetooth() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }

    /**
     * Toggles between normal and vibrate/silent ringer mode.
     * Requires notification policy access.
     */
    fun toggleSilentMode() {
        val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            application.startActivity(intent)
            return
        }

        val newMode = if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            AudioManager.RINGER_MODE_VIBRATE
        } else {
            AudioManager.RINGER_MODE_NORMAL
        }
        audioManager.ringerMode = newMode
    }

    /**
     * Toggles the device torch (flashlight).
     */
    fun toggleTorch() {
        try {
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, !_isTorchOn.value)
        } catch (e: Exception) { e.printStackTrace() }
    }

    /**
     * Opens airplane mode settings.
     */
    fun toggleAirplaneMode() {
        val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }

    /**
     * Opens display settings for dark mode toggle.
     */
    fun toggleDarkMode() {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }

    /**
     * Opens mobile data settings.
     */
    fun openMobileDataSettings() {
        val actions = listOf(
            Settings.ACTION_DATA_ROAMING_SETTINGS,
            "android.settings.DATA_ROAMING_SETTINGS",
            "android.settings.NETWORK_OPERATOR_SETTINGS",
            Settings.ACTION_WIRELESS_SETTINGS,
            Settings.ACTION_SETTINGS
        )

        for (action in actions) {
            try {
                val intent = Intent(action)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                application.startActivity(intent)
                return
            } catch (_: Exception) { }
        }
    }
}
