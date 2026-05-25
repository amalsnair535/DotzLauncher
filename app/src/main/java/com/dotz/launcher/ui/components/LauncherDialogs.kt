package com.dotz.launcher.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.dotz.launcher.R
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
            Text(stringResource(R.string.notification_permission_title), color = DotzColors.White, fontSize = 16.sp)
        },
        text = {
            Text(
                stringResource(R.string.notification_permission_desc),
                color = DotzColors.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onGoToSettings) {
                Text(stringResource(R.string.btn_enable), color = DotzColors.White, fontSize = 13.sp, letterSpacing = 1.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_skip), color = DotzColors.White.copy(alpha = 0.4f), fontSize = 13.sp, letterSpacing = 1.sp)
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
            Text(stringResource(R.string.default_launcher_title), color = DotzColors.White, fontSize = 16.sp)
        },
        text = {
            Text(
                stringResource(R.string.default_launcher_desc),
                color = DotzColors.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onGoToSettings) {
                Text(stringResource(R.string.btn_set_default), color = DotzColors.White, fontSize = 13.sp, letterSpacing = 1.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_skip), color = DotzColors.White.copy(alpha = 0.4f), fontSize = 13.sp, letterSpacing = 1.sp)
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
            Text(stringResource(R.string.unassigned_tile_title), color = DotzColors.White, fontSize = 16.sp)
        },
        text = {
            Text(
                stringResource(R.string.unassigned_tile_desc, tileLabel),
                color = DotzColors.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onSelectApp) {
                Text(stringResource(R.string.btn_select_app), color = DotzColors.White, fontSize = 13.sp, letterSpacing = 1.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = DotzColors.White.copy(alpha = 0.4f), fontSize = 13.sp, letterSpacing = 1.sp)
            }
        }
    )
}

