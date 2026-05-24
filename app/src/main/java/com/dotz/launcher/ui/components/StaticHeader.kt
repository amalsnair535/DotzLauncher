package com.dotz.launcher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotz.launcher.ui.theme.DotzType
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StaticHeader(
    batteryLevel: Int,
    networkStatus: String,
    isWifiEnabled: Boolean,
    isBluetoothEnabled: Boolean,
    isSilentMode: Boolean,
    isTorchOn: Boolean,
    isAirplaneModeOn: Boolean,
    isDarkModeOn: Boolean,
    weather: com.dotz.launcher.viewmodel.WeatherData,
    showWeather: Boolean,
    onLauncherSettingsTap: () -> Unit,
    onWifiToggle: () -> Unit,
    onBluetoothToggle: () -> Unit,
    onSilentToggle: () -> Unit,
    onTorchToggle: () -> Unit,
    onAirplaneToggle: () -> Unit,
    onDarkModeToggle: () -> Unit,
    onDataClick: () -> Unit,
    onWeatherClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var timeText by remember { mutableStateOf(currentTime()) }
    var dateText by remember { mutableStateOf(currentDate()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            timeText = currentTime()
            dateText = currentDate()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 0.dp),
    ) {
        // Weather (Top Right)
        if (showWeather) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable(onClick = onWeatherClick),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = weather.temp,
                    style = DotzType.DateStyle.copy(fontSize = 18.sp),
                    color = Color.White,
                )
                Text(
                    text = weather.description,
                    style = DotzType.DateStyle.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
        }

        // Main Content (Centered)
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Network (Left)
                Text(
                    text = networkStatus,
                    style = DotzType.DateStyle.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.width(60.dp),
                    textAlign = TextAlign.End
                )

                Spacer(Modifier.width(20.dp))

                // Time (Center)
                Text(
                    text      = timeText,
                    style     = DotzType.TimeStyle,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.width(20.dp))

                // Battery (Right)
                Text(
                    text = if (batteryLevel >= 0) "$batteryLevel%" else "--%",
                    style = DotzType.DateStyle.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.width(60.dp),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(Modifier.height(4.dp))
            
            Text(
                text      = dateText,
                style     = DotzType.DateStyle,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            DetoxPanel(
                isWifiEnabled = isWifiEnabled,
                isBluetoothEnabled = isBluetoothEnabled,
                isSilentMode = isSilentMode,
                isTorchOn = isTorchOn,
                isAirplaneModeOn = isAirplaneModeOn,
                isDarkModeOn = isDarkModeOn,
                onWifiToggle = onWifiToggle,
                onBluetoothToggle = onBluetoothToggle,
                onSilentToggle = onSilentToggle,
                onTorchToggle = onTorchToggle,
                onAirplaneToggle = onAirplaneToggle,
                onDarkModeToggle = onDarkModeToggle,
                onSettingsClick = onLauncherSettingsTap,
                onDataClick = onDataClick,
                modifier = Modifier.padding(bottom = 0.dp)
            )
        }
    }
}

private fun currentTime(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun currentDate(): String =
    SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
        .format(Date())
        .uppercase(Locale.getDefault())
