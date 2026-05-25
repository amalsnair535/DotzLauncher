package com.dotz.launcher

import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dotz.launcher.data.AppTile
import com.dotz.launcher.ui.components.DefaultLauncherDialog
import com.dotz.launcher.ui.components.NotificationPermissionDialog
import com.dotz.launcher.ui.components.UnassignedTileDialog
import com.dotz.launcher.ui.screens.AppSelectionActivity
import com.dotz.launcher.ui.screens.DotzHomeScreen
import com.dotz.launcher.ui.screens.DotzSettingsActivity
import com.dotz.launcher.ui.theme.DotzTheme
import com.dotz.launcher.ui.utils.LauncherUtils
import com.dotz.launcher.viewmodel.LauncherViewModel

/**
 * Main entry point of the launcher.
 * Handles the immersive mode, permission dialogs, and delegates UI rendering to [DotzHomeScreen].
 */
class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Modern full-screen immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            DotzTheme(settings = uiState.settings) {
                var showNotifPermDialog by remember { mutableStateOf(false) }
                var showDefaultLauncherDialog by remember { mutableStateOf(false) }
                var tileToAssign by remember { mutableStateOf<AppTile?>(null) }

                // Check permissions and default launcher
                LaunchedEffect(uiState.isDefaultLauncher) {
                    if (!isNotificationListenerEnabled()) showNotifPermDialog = true
                    
                    // Show dialog if not default launcher
                    showDefaultLauncherDialog = !uiState.isDefaultLauncher
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    DotzHomeScreen(
                        uiState       = uiState,
                        iconCache     = viewModel.iconCache,
                        onTileTap     = { tile ->
                            viewModel.onTileTapped(tile)
                            handleTileClick(tile) {
                                tileToAssign = tile
                            }
                        },
                        onTileLongPress = { _ ->
                            hapticPulse()
                            startActivity(Intent(this@MainActivity, DotzSettingsActivity::class.java))
                        },
                        onLauncherSettingsTap = {
                            startActivity(Intent(this@MainActivity, DotzSettingsActivity::class.java))
                        },
                        onWifiToggle = viewModel::toggleWifi,
                        onBluetoothToggle = viewModel::toggleBluetooth,
                        onSilentToggle = viewModel::toggleSilentMode,
                        onTorchToggle = viewModel::toggleTorch,
                        onAirplaneToggle = viewModel::toggleAirplaneMode,
                        onDarkModeToggle = viewModel::toggleDarkMode,
                        onDataClick = viewModel::openMobileDataSettings,
                        onWeatherClick = {
                            LauncherUtils.openWeatherApp(this@MainActivity)
                        }
                    )

                    if (showNotifPermDialog) {
                        NotificationPermissionDialog(
                            onDismiss = { showNotifPermDialog = false },
                            onGoToSettings = {
                                showNotifPermDialog = false
                                try {
                                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                } catch (_: Exception) {
                                    startActivity(Intent(Settings.ACTION_SETTINGS))
                                }
                            },
                        )
                    }

                    if (showDefaultLauncherDialog) {
                        DefaultLauncherDialog(
                            onDismiss = { showDefaultLauncherDialog = false },
                            onGoToSettings = {
                                showDefaultLauncherDialog = false
                                viewModel.openDefaultLauncherSettings()
                            }
                        )
                    }

                    tileToAssign?.let { tile ->
                        UnassignedTileDialog(
                            tileLabel = tile.label,
                            onDismiss = { tileToAssign = null },
                            onSelectApp = {
                                tileToAssign = null
                                startActivity(
                                    Intent(this@MainActivity, AppSelectionActivity::class.java)
                                        .putExtra("tileId", tile.tileId)
                                        .putExtra("tileLabel", tile.label),
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
        // Re-apply immersive mode
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun handleTileClick(tile: AppTile, onUnassigned: () -> Unit) {
        // Self-tap opens settings
        if (tile.packageName == this.packageName) {
            startActivity(Intent(this, DotzSettingsActivity::class.java))
            return
        }

        if (tile.isInstalled) {
            val intent = packageManager.getLaunchIntentForPackage(tile.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                onUnassigned()
            }
        } else {
            onUnassigned()
        }
    }

    private fun hapticPulse() {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        vibrator.vibrate(VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabled?.contains(packageName) == true
    }
}
