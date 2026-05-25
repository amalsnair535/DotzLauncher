package com.dotz.launcher.ui.screens

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import com.dotz.launcher.data.IconCacheManager
import com.dotz.launcher.ui.theme.DotzColors
import com.dotz.launcher.ui.theme.DotzTheme
import com.dotz.launcher.viewmodel.LauncherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Activity for selecting an app to assign to a tile.
 * Displays a list of recommended apps based on the tile category.
 */
class AppSelectionActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val tileId = intent.getIntExtra("tileId", -1)
        val tileLabel = intent.getStringExtra("tileLabel") ?: "APP"
        if (tileId == -1) { finish(); return }

        val installedApps = viewModel.getInstalledAppsForTile(tileId)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            DotzTheme(settings = uiState.settings) {
                AppSelectionScreen(
                    apps    = installedApps,
                    title   = "SELECT $tileLabel APP",
                    iconCache = viewModel.iconCache,
                    iconPackPackage = uiState.settings.iconPackPackage,
                    onBack  = { finish() }
                ) { pkg, label ->
                    viewModel.updateTileOverride(tileId, pkg, label.uppercase())
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppSelectionScreen(
    apps: List<Pair<String, String>>,
    title: String,
    iconCache: IconCacheManager,
    iconPackPackage: String?,
    onBack: () -> Unit,
    onSelect: (String, String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) apps
        else apps.filter { it.second.contains(query, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Normal,
                        color = DotzColors.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DotzColors.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
            )
        },
        containerColor = Color.Black,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                modifier      = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                placeholder   = { Text("Search apps…", color = DotzColors.White.copy(alpha = 0.3f)) },
                leadingIcon   = { Icon(Icons.Default.Search, null, tint = DotzColors.White.copy(alpha = 0.5f)) },
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = DotzColors.White.copy(alpha = 0.4f),
                    unfocusedBorderColor = DotzColors.White.copy(alpha = 0.15f),
                    focusedTextColor     = DotzColors.White,
                    unfocusedTextColor   = DotzColors.White,
                    cursorColor          = DotzColors.White
                )
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { (pkg, label) ->
                    AppRow(
                        pkg = pkg, 
                        label = label, 
                        iconCache = iconCache,
                        iconPackPackage = iconPackPackage,
                        onClick = { 
                            onSelect(pkg, label)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    pkg: String, 
    label: String, 
    iconCache: IconCacheManager,
    iconPackPackage: String?,
    onClick: () -> Unit
) {
    val iconBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = pkg,
        key2 = iconPackPackage
    ) {
        val bitmap = withContext(Dispatchers.IO) {
            val cached = iconCache.getIcon(pkg, iconPackPackage, false)
            if (cached != null) {
                cached.asImageBitmap()
            } else {
                iconCache.loadIcon(pkg, iconPackPackage)?.let { drawable ->
                    iconCache.saveIcon(pkg, iconPackPackage, false, drawable)
                    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 512
                    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 512
                    drawable.toBitmap(width, height).asImageBitmap()
                }
            }
        }
        value = bitmap
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DotzColors.Tile)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap             = iconBitmap!!,
                contentDescription = label,
                modifier           = Modifier.size(36.dp)
            )
        } else {
            Spacer(Modifier.size(36.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, color = DotzColors.White, fontSize = 14.sp)
            Text(pkg, color = DotzColors.White.copy(alpha = 0.35f), fontSize = 10.sp)
        }
    }
}
