@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.dotz.launcher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dotz.launcher.data.AppTile
import com.dotz.launcher.data.IconCacheManager
import com.dotz.launcher.ui.components.AppGrid
import com.dotz.launcher.ui.components.DynamicBackground
import com.dotz.launcher.ui.components.StaticHeader
import com.dotz.launcher.viewmodel.LauncherUiState
import kotlin.math.abs

/**
 * The main home screen of the Dotz Launcher.
 * Displays a fixed header with system info and a pager containing app grids.
 *
 * @param uiState Current UI state from the ViewModel.
 * @param iconCache Cache manager for app icons.
 * @param onTileTap Callback for when an app tile is tapped.
 * @param onTileLongPress Callback for when an app tile is long-pressed.
 * @param onLauncherSettingsTap Callback for when the settings icon is tapped.
 * @param onWifiToggle Callback to toggle Wi-Fi.
 * @param onBluetoothToggle Callback to toggle Bluetooth.
 * @param onSilentToggle Callback to toggle silent mode.
 * @param onTorchToggle Callback to toggle the flashlight.
 * @param onAirplaneToggle Callback to toggle airplane mode.
 * @param onDarkModeToggle Callback to toggle dark mode.
 * @param onDataClick Callback to open mobile data settings.
 * @param onWeatherClick Callback to open the weather app.
 */
@Composable
fun DotzHomeScreen(
    uiState: LauncherUiState,
    iconCache: IconCacheManager,
    onTileTap: (AppTile) -> Unit,
    onTileLongPress: (AppTile) -> Unit,
    onLauncherSettingsTap: () -> Unit,
    onWifiToggle: () -> Unit,
    onBluetoothToggle: () -> Unit,
    onSilentToggle: () -> Unit,
    onTorchToggle: () -> Unit,
    onAirplaneToggle: () -> Unit,
    onDarkModeToggle: () -> Unit,
    onDataClick: () -> Unit,
    onWeatherClick: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val pages = listOf(uiState.page0Tiles, uiState.page1Tiles)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DynamicBackground(enabled = uiState.settings.dynamicBackgroundEnabled)
            
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Fixed Header ──────────────────────────────────────────────
                StaticHeader(
                    batteryLevel  = uiState.batteryLevel,
                    networkStatus = uiState.networkStatus,
                    isWifiEnabled = uiState.isWifiEnabled,
                    isBluetoothEnabled = uiState.isBluetoothEnabled,
                    isSilentMode = uiState.isSilentMode,
                    isTorchOn = uiState.isTorchOn,
                    isAirplaneModeOn = uiState.isAirplaneModeOn,
                    isDarkModeOn = uiState.isDarkModeOn,
                    weather = uiState.weather,
                    showWeather = uiState.settings.showWeatherInfo,
                    onLauncherSettingsTap = onLauncherSettingsTap,
                    onWifiToggle = onWifiToggle,
                    onBluetoothToggle = onBluetoothToggle,
                    onSilentToggle = onSilentToggle,
                    onTorchToggle = onTorchToggle,
                    onAirplaneToggle = onAirplaneToggle,
                    onDarkModeToggle = onDarkModeToggle,
                    onDataClick = onDataClick,
                    onWeatherClick = onWeatherClick,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .weight(0.40f)
                )

                // ── Pager (remaining area) ─────────────────────────────
                if (uiState.settings.verticalScrolling) {
                    VerticalPager(
                        state    = pagerState,
                        modifier = Modifier.weight(0.60f)
                    ) { pageIndex ->
                        val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                        val scaleFactor = lerp(0.95f, 1.0f, 1f - abs(pageOffset).coerceIn(0f, 1f))

                        AppGrid(
                            tiles         = pages[pageIndex],
                            iconCache     = iconCache,
                            tileOpacity   = uiState.settings.tileOpacity,
                            grayscale     = uiState.settings.grayscaleMode,
                            iconPackPackage = uiState.settings.iconPackPackage,
                            showBadges    = uiState.settings.showNotificationDots,
                            onTileTap     = onTileTap,
                            onTileLongPress = onTileLongPress,
                            modifier      = Modifier
                                .fillMaxSize()
                                .scale(scaleFactor)
                        )
                    }
                } else {
                    HorizontalPager(
                        state    = pagerState,
                        modifier = Modifier.weight(0.60f)
                    ) { pageIndex ->
                        val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                        val scaleFactor = lerp(0.95f, 1.0f, 1f - abs(pageOffset).coerceIn(0f, 1f))

                        AppGrid(
                            tiles         = pages[pageIndex],
                            iconCache     = iconCache,
                            tileOpacity   = uiState.settings.tileOpacity,
                            grayscale     = uiState.settings.grayscaleMode,
                            iconPackPackage = uiState.settings.iconPackPackage,
                            showBadges    = uiState.settings.showNotificationDots,
                            onTileTap     = onTileTap,
                            onTileLongPress = onTileLongPress,
                            modifier      = Modifier
                                .fillMaxSize()
                                .scale(scaleFactor)
                        )
                    }
                }
            }
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + fraction * (stop - start)
