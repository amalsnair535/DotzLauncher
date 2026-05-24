package com.dotz.launcher

import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dotz.launcher.ui.screens.AppSelectionActivity
import com.dotz.launcher.ui.screens.AppSelectionListActivity
import com.dotz.launcher.ui.screens.DotzHomeScreen
import com.dotz.launcher.ui.screens.DotzSettingsActivity
import com.dotz.launcher.ui.theme.DotzColors
import com.dotz.launcher.ui.theme.DotzTheme
import com.dotz.launcher.viewmodel.LauncherViewModel
import com.dotz.launcher.data.AppTile

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
                var showNotifPermDialog by remember { mutableStateOf(value = false) }
                var showDefaultLauncherDialog by remember { mutableStateOf(value = false) }
                var tileToAssign by remember { mutableStateOf<AppTile?>(value = null) }

                // Check permissions and default launcher
                LaunchedEffect(uiState.isDefaultLauncher) {
                    if (!isNotificationListenerEnabled()) showNotifPermDialog = true
                    
                    // Show dialog if not default launcher, but hide it if it becomes default
                    if (!uiState.isDefaultLauncher) {
                        showDefaultLauncherDialog = true
                    } else {
                        showDefaultLauncherDialog = false
                    }
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
                            openWeatherApp()
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
                                    // Fallback to general settings if specific intent fails
                                    startActivity(Intent(Settings.ACTION_SETTINGS))
                                }
                            },
                        )
                    }

                    if (showDefaultLauncherDialog) {
                        DefaultLauncherDialog(
                            onDismiss = { showDefaultLauncherDialog = false },
                        ) {
                            showDefaultLauncherDialog = false
                            viewModel.openDefaultLauncherSettings()
                        }
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

    /** Disable back button — this IS the home screen */
    @Deprecated("Deprecated in API 33")
    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

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
                // Should not happen if isInstalled is true, but just in case
                onUnassigned()
            }
        } else {
            // App not installed / Tile unassigned
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

    private fun openWeatherApp() {
        val weatherIntents = mutableListOf<Intent?>(
            Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("dynweather://") },
            Intent("android.intent.action.WEATHER_READY"),
            packageManager.getLaunchIntentForPackage("com.google.android.googlequicksearchbox"), // Google Weather
            packageManager.getLaunchIntentForPackage("com.android.weather"),
            packageManager.getLaunchIntentForPackage("com.sec.android.app.weather"), // Samsung
            packageManager.getLaunchIntentForPackage("com.miui.weather2"), // Xiaomi
            packageManager.getLaunchIntentForPackage("com.coloros.weather"), // Oppo
            packageManager.getLaunchIntentForPackage("com.huawei.android.totemweather") // Huawei
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            weatherIntents.add(0, Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_WEATHER) })
        }

        for (intent in weatherIntents) {
            if (intent != null) {
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    return
                } catch (_: Exception) {}
            }
        }
        
        // Fallback: Open Google Search for weather
        try {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=weather"))
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(fallbackIntent)
        } catch (_: Exception) {}
    }
}

@Composable
private fun NotificationPermissionDialog(
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = DotzColors.Tile,
        title = {
            Text("Enable Notifications", color = DotzColors.White, fontSize = 16.sp)
        },
        text = {
            Text(
                "Allow Dotz to read notifications so it can show badge counts on your app tiles.",
                color = DotzColors.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onGoToSettings) {
                Text("ENABLE", color = DotzColors.White, fontSize = 13.sp, letterSpacing = 1.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("SKIP", color = DotzColors.White.copy(alpha = 0.4f), fontSize = 13.sp, letterSpacing = 1.sp)
            }
        }
    )
}

@Composable
private fun DefaultLauncherDialog(
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = DotzColors.Tile,
        title = {
            Text("Set as Default Launcher", color = DotzColors.White, fontSize = 16.sp)
        },
        text = {
            Text(
                "To use Dotz as your main home screen, you need to set it as the default launcher in system settings.",
                color = DotzColors.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onGoToSettings) {
                Text("SET DEFAULT", color = DotzColors.White, fontSize = 13.sp, letterSpacing = 1.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("SKIP", color = DotzColors.White.copy(alpha = 0.4f), fontSize = 13.sp, letterSpacing = 1.sp)
            }
        }
    )
}

@Composable
private fun UnassignedTileDialog(
    tileLabel: String,
    onDismiss: () -> Unit,
    onSelectApp: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = DotzColors.Tile,
        title = {
            Text("Unassigned Tile", color = DotzColors.White, fontSize = 16.sp)
        },
        text = {
            Text(
                "No app is currently assigned to the $tileLabel tile. Would you like to select one now?",
                color = DotzColors.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onSelectApp) {
                Text("SELECT APP", color = DotzColors.White, fontSize = 13.sp, letterSpacing = 1.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = DotzColors.White.copy(alpha = 0.4f), fontSize = 13.sp, letterSpacing = 1.sp)
            }
        }
    )
}
