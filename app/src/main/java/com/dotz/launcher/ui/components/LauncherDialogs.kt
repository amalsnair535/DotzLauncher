package com.dotz.launcher.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.dotz.launcher.ui.theme.DotzColors

/**
 * Dialog shown when notification listener permission is missing.
 */
@Composable
fun NotificationPermissionDialog(
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

/**
 * Dialog shown when Dotz is not the default launcher.
 */
@Composable
fun DefaultLauncherDialog(
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

/**
 * Dialog shown when an unassigned tile is tapped.
 */
@Composable
fun UnassignedTileDialog(
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
