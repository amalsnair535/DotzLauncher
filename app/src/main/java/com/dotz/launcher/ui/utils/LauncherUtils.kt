package com.dotz.launcher.ui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Utility functions for common launcher actions.
 */
object LauncherUtils {

    /**
     * Attempts to open the best available weather app on the device.
     * Falls back to a Google search if no app is found.
     */
    fun openWeatherApp(context: Context) {
        val pm = context.packageManager
        val weatherIntents = mutableListOf<Intent?>(
            Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("dynweather://") },
            Intent("android.intent.action.WEATHER_READY"),
            pm.getLaunchIntentForPackage("com.google.android.googlequicksearchbox"), // Google Weather
            pm.getLaunchIntentForPackage("com.android.weather"),
            pm.getLaunchIntentForPackage("com.sec.android.app.weather"), // Samsung
            pm.getLaunchIntentForPackage("com.miui.weather2"), // Xiaomi
            pm.getLaunchIntentForPackage("com.coloros.weather"), // Oppo
            pm.getLaunchIntentForPackage("com.huawei.android.totemweather") // Huawei
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            weatherIntents.add(0, Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_WEATHER) })
        }

        for (intent in weatherIntents) {
            if (intent != null) {
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return
                } catch (_: Exception) {}
            }
        }
        
        // Fallback: Open Google Search for weather
        try {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=weather"))
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallbackIntent)
        } catch (_: Exception) {}
    }
}
