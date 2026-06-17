package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TVLauncherScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // View Model states
    val currentTime by viewModel.currentTime.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val systemStats by viewModel.systemStats.collectAsState()
    val wallpaperStyle by viewModel.wallpaperStyle.collectAsState()

    val allApps by viewModel.allApps.collectAsState()
    val favoriteApps by viewModel.favoriteApps.collectAsState()

    // Screen states
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") } // all, favorites, system, non-system, hidden
    var activeAppDialogItem by remember { mutableStateOf<LauncherApp?>(null) }
    var activeRenameItem by remember { mutableStateOf<LauncherApp?>(null) }
    var newCustomNameText by remember { mutableStateOf("") }
    var showHiddenAppsDialog by remember { mutableStateOf(false) }

    // Navigation Category list
    val categories = listOf(
        Triple("all", "Tất cả ứng dụng", Icons.Rounded.Apps),
        Triple("favorites", "Yêu thích", Icons.Rounded.Star),
        Triple("system", "Hệ thống", Icons.Rounded.Build),
        Triple("user", "Đã cài thêm", Icons.Rounded.SaveAlt)
    )

    // Filter apps safely
    val filteredApps = remember(allApps, selectedCategory, searchQuery) {
        allApps.filter { app ->
            // Check category filter
            val matchesCategory = when (selectedCategory) {
                "favorites" -> app.isFavorite && !app.isHidden
                "system" -> app.packageName.startsWith("com.android") || 
                            app.packageName.startsWith("com.google") || 
                            app.packageName.contains("system") || 
                            app.packageName.startsWith("android")
                "user" -> !app.packageName.startsWith("com.android") && 
                          !app.packageName.startsWith("com.google") && 
                          !app.packageName.contains("system") && 
                          !app.packageName.startsWith("android")
                else -> !app.isHidden
            }
            // Check text search filter
            val matchesSearch = searchQuery.isBlank() || 
                    app.displayName.lowercase().contains(searchQuery.lowercase()) ||
                    app.packageName.lowercase().contains(searchQuery.lowercase())

            matchesCategory && matchesSearch
        }
    }

    // Hidden apps list
    val concealedApps = remember(allApps) {
        allApps.filter { it.isHidden }
    }

    // Modern glassmorphic background layer
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0F14))
    ) {
        // Layer 1: Background wallpaper image or programmatic deep gradient
        when (wallpaperStyle) {
            "cinematic" -> {
                Image(
                    painter = painterResource(id = R.drawable.img_tv_wallpaper),
                    contentDescription = "Cinematic TV Wallpaper",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Dark premium cinematic gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.55f),
                                    Color(0xFF0F1219).copy(alpha = 0.85f),
                                    Color(0xFF070A0F).copy(alpha = 0.98f)
                                )
                            )
                        )
                )
            }
            "cosmic" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1B0B33),
                                    Color(0xFF0E0519),
                                    Color(0xFF05010A)
                                )
                            )
                        )
                )
            }
            "minimal" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1E222B),
                                    Color(0xFF11141A)
                                )
                            )
                        )
                )
            }
            "cyber" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF00121A),
                                    Color(0xFF000508)
                                )
                            )
                        )
                )
            }
        }

        // Layer 2: Main Layout Body (TV Box proportions)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Top HUD Dashboard
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Branding
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tv,
                        contentDescription = "SmartTV icon",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "SmartTV Launcher",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Giao điện TV Box mượt mà & tùy biến",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                // Center/Right: Live System details badges & Ticking Clock
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // System Info chips (RAM, Storage, Net)
                    SystemMetricChip(icon = Icons.Rounded.Memory, label = systemStats.first, color = Color(0xFFE2B6FF))
                    SystemMetricChip(icon = Icons.Rounded.SdCard, label = systemStats.second, color = Color(0xFFB4E33D))
                    SystemMetricChip(icon = Icons.Rounded.Wifi, label = systemStats.third, color = Color(0xFF00E5FF))

                    // Clock Display Card
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (currentTime.isEmpty()) "00:00:00" else currentTime,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Right
                        )
                        Text(
                            text = if (currentDate.isEmpty()) "Hôm nay" else currentDate,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.65f),
                            textAlign = TextAlign.Right
                        )
                    }
                }
            }

            // Divider
            Divider(
                color = Color.White.copy(alpha = 0.08f),
                thickness = 1.dp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Dynamic Content Area (Horizontal Split: Row Grid layout)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Panel: Category Menu selection & Extra config deck
                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "DANH MỤC",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                    )

                    // Navigation Category buttons
                    categories.forEach { cat ->
                        CategoryListItem(
                            label = cat.second,
                            icon = cat.third,
                            isSelected = selectedCategory == cat.first,
                            onClick = { selectedCategory = cat.first },
                            testTag = "category_${cat.first}"
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Sub Config Deck in side menu
                    Text(
                        text = "TÙY BIẾN NHANH",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(start = 8.dp)
                    )

                    // Live Wallpaper Toggle button (Cycles style)
                    TVMenuButton(
                        text = when (wallpaperStyle) {
                            "cinematic" -> "Hình nền: Điện ảnh"
                            "cosmic" -> "Hình nền: Không gian"
                            "minimal" -> "Hình nền: Tối giản"
                            else -> "Hình nền: Cyber Neon"
                        },
                        icon = Icons.Rounded.Wallpaper,
                        onClick = {
                            val nextStyle = when (wallpaperStyle) {
                                "cinematic" -> "cosmic"
                                "cosmic" -> "minimal"
                                "minimal" -> "cyber"
                                else -> "cinematic"
                            }
                            viewModel.setWallpaperStyle(nextStyle)
                        },
                        colorTint = Color(0xFF00E5FF),
                        testTag = "wallpaper_toggle_button"
                    )

                    // Management tools buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            TVMenuButton(
                                text = "Ẩn (${concealedApps.size})",
                                icon = Icons.Rounded.VisibilityOff,
                                onClick = { showHiddenAppsDialog = true },
                                colorTint = Color.LightGray,
                                compactMode = true,
                                testTag = "hidden_apps_trigger"
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            TVMenuButton(
                                text = "Đặt lại",
                                icon = Icons.Rounded.RestartAlt,
                                onClick = { viewModel.resetLauncherSettings() },
                                colorTint = Color(0xFFFF5252),
                                compactMode = true,
                                testTag = "reset_settings_button"
                            )
                        }
                    }
                }

                // Right Panel: Primary App Dashboard grid/scroll area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Search layout line
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Tìm kiếm ứng dụng...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search icon", tint = Color.White.copy(alpha = 0.5f)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = "Clear search", tint = Color.White.copy(alpha = 0.5f))
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00E5FF).copy(alpha = 0.8f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedContainerColor = Color.Black.copy(alpha = 0.25f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("search_app_input")
                        )

                        // Info indicator
                        Text(
                            text = "Chọn ứng dụng để mở / hiện Menu",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.width(180.dp)
                        )
                    }

                    // Content Rows Layout (Favorites & Sub-filtered apps)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Section: FAVORITE APPS Row (Always visible pinned drawer)
                        if (selectedCategory == "all" && favoriteApps.isNotEmpty() && searchQuery.isEmpty()) {
                            item {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        Icon(Icons.Rounded.Star, contentDescription = "Star", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "ỨNG DỤNG YÊU THÍCH",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD700),
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)
                                    ) {
                                        itemsIndexed(favoriteApps) { idx, app ->
                                            AppTileItem(
                                                app = app,
                                                isFavoriteSection = true,
                                                onLaunch = { viewModel.launchApp(context, app.packageName) },
                                                onOptionSelected = { activeAppDialogItem = app },
                                                testTag = "fav_tile_${app.packageName}"
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Section: Filtered apps list (Dynamic comfortable wrapping layout / dual rows)
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Dynamic Title based on selected classification
                                Text(
                                    text = when (selectedCategory) {
                                        "all" -> if (searchQuery.isNotEmpty()) "KẾT QUẢ TÌM KIẾM (${filteredApps.size})" else "TẤT CẢ ỨNG DỤNG CHI TIẾT (${filteredApps.size})"
                                        "favorites" -> "ỨNG DỤNG YÊU THÍCH ĐÃ PIN (${filteredApps.size})"
                                        "system" -> "HỆ THỐNG GỐC TV (${filteredApps.size})"
                                        else -> "ỨNG DỤNG ĐÃ CÀI THÊM OS (${filteredApps.size})"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(start = 4.dp)
                                )

                                if (filteredApps.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Rounded.SearchOff,
                                                contentDescription = "No app found",
                                                tint = Color.White.copy(alpha = 0.3f),
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Không tìm thấy ứng dụng nào phù hợp",
                                                color = Color.White.copy(alpha = 0.4f),
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                } else {
                                    // Chunk apps in lists of 5 for dual level wrapping rows inside scrolling container
                                    val appChunks = filteredApps.chunked(5)
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        appChunks.forEach { chunk ->
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                chunk.forEach { app ->
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        AppTileItem(
                                                            app = app,
                                                            isFavoriteSection = false,
                                                            onLaunch = { viewModel.launchApp(context, app.packageName) },
                                                            onOptionSelected = { activeAppDialogItem = app },
                                                            testTag = "app_tile_${app.packageName}"
                                                        )
                                                    }
                                                }
                                                // Pad empty items to align grid elegantly
                                                if (chunk.size < 5) {
                                                    repeat(5 - chunk.size) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ================== DIALOGS AND OVERLAYS ==================

        // 1. App Actions Bottom sheet/Dialog Options
        activeAppDialogItem?.let { app ->
            Dialog(onDismissRequest = { activeAppDialogItem = null }) {
                Card(
                    modifier = Modifier
                        .width(420.dp)
                        .padding(16.dp)
                        .testTag("app_actions_dialog"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header info
                        AppIconContainer(
                            systemIcon = app.systemIcon,
                            contentDescription = app.displayName,
                            size = 64
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = app.displayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        if (app.customLabel != null) {
                            Text(
                                text = "Gốc: ${app.label}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            text = app.packageName,
                            fontSize = 10.sp,
                            color = Color(0xFF00E5FF).copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Divider
                        Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 12.dp))

                        // Controls List
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DialogActionButton(
                                label = "🚀 Khởi chạy ứng dụng",
                                icon = Icons.Rounded.PlayArrow,
                                color = Color(0xFF00E5FF),
                                onClick = {
                                    activeAppDialogItem = null
                                    viewModel.launchApp(context, app.packageName)
                                },
                                testTag = "dialog_launch_btn"
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    DialogActionButton(
                                        label = if (app.isFavorite) "⭐ Bỏ ghim" else "⭐ Ghim Yêu thích",
                                        icon = if (app.isFavorite) Icons.Rounded.StarOutline else Icons.Rounded.Star,
                                        color = Color(0xFFFFD700),
                                        onClick = {
                                            viewModel.toggleFavorite(app.packageName)
                                            activeAppDialogItem = null
                                        },
                                        testTag = "dialog_fav_btn"
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    DialogActionButton(
                                        label = "✏️ Đổi tên hiển thị",
                                        icon = Icons.Rounded.Edit,
                                        color = Color(0xFFB4E33D),
                                        onClick = {
                                            newCustomNameText = app.customLabel ?: app.label
                                            activeRenameItem = app
                                            activeAppDialogItem = null
                                        },
                                        testTag = "dialog_rename_btn"
                                    )
                                }
                            }

                            // Left/Right shift reordering (If pinned as Favorite)
                            if (app.isFavorite) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        DialogActionButton(
                                            label = "⬅️ Dịch Sang Trái",
                                            icon = Icons.Rounded.ArrowBack,
                                            color = Color.White,
                                            onClick = {
                                                viewModel.moveFavorite(app.packageName, moveUp = true)
                                                activeAppDialogItem = null
                                            },
                                            testTag = "dialog_move_left_btn"
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        DialogActionButton(
                                            label = "➡️ Dịch Sang Phải",
                                            icon = Icons.Rounded.ArrowForward,
                                            color = Color.White,
                                            onClick = {
                                                viewModel.moveFavorite(app.packageName, moveUp = false)
                                                activeAppDialogItem = null
                                            },
                                            testTag = "dialog_move_right_btn"
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    DialogActionButton(
                                        label = "👁️ Ẩn ứng dụng",
                                        icon = Icons.Rounded.VisibilityOff,
                                        color = Color(0xFFFF9800),
                                        onClick = {
                                            viewModel.toggleHidden(app.packageName)
                                            activeAppDialogItem = null
                                        },
                                        testTag = "dialog_hide_btn"
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    DialogActionButton(
                                        label = "ℹ️ Chi tiết hệ thống",
                                        icon = Icons.Rounded.Info,
                                        color = Color.LightGray,
                                        onClick = {
                                            viewModel.openAppDetails(context, app.packageName)
                                            activeAppDialogItem = null
                                        },
                                        testTag = "dialog_details_btn"
                                    )
                                }
                            }

                            // Danger actions
                            Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(2f)) {
                                    DialogActionButton(
                                        label = "🗑️ Gỡ cài đặt ứng dụng",
                                        icon = Icons.Rounded.Delete,
                                        color = Color(0xFFFF5252),
                                        onClick = {
                                            viewModel.uninstallApp(context, app.packageName)
                                            activeAppDialogItem = null
                                        },
                                        testTag = "dialog_uninstall_btn"
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    DialogActionButton(
                                        label = "Đóng",
                                        icon = Icons.Rounded.Close,
                                        color = Color.White.copy(alpha = 0.4f),
                                        backgroundOpacity = 0.08f,
                                        onClick = { activeAppDialogItem = null },
                                        testTag = "dialog_close_btn"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Rename Dialog overlay
        activeRenameItem?.let { app ->
            Dialog(onDismissRequest = { activeRenameItem = null }) {
                Card(
                    modifier = Modifier
                        .width(400.dp)
                        .padding(16.dp)
                        .testTag("app_rename_dialog"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFB4E33D).copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Đổi tên hiển thị",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = newCustomNameText,
                            onValueChange = { newCustomNameText = it },
                            placeholder = { Text("Nhập tên hiển thị mới...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFB4E33D),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rename_input_field")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    viewModel.renameApp(app.packageName, null) // reset
                                    activeRenameItem = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Khôi phục gốc", color = Color.White, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.renameApp(app.packageName, newCustomNameText)
                                    activeRenameItem = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB4E33D)),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text("Áp dụng", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Hidden Apps Dialog overlay
        if (showHiddenAppsDialog) {
            Dialog(onDismissRequest = { showHiddenAppsDialog = false }) {
                Card(
                    modifier = Modifier
                        .width(480.dp)
                        .padding(16.dp)
                        .testTag("app_hidden_dialog"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151921)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "DANH SÁCH ỨNG DỤNG ĐANG ẨN (${concealedApps.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Các ứng dụng đã ẩn sẽ không hiển thị trên danh sách ngoài màn hình chính.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 12.dp))

                        if (concealedApps.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Không có ứng dụng nào bị ẩn",
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(concealedApps) { app ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            AppIconContainer(systemIcon = app.systemIcon, contentDescription = app.displayName, size = 32)
                                            Text(
                                                text = app.displayName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.width(200.dp)
                                            )
                                        }

                                        Button(
                                            onClick = { viewModel.toggleHidden(app.packageName) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Hiện lại", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showHiddenAppsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("hidden_dialog_close")
                        ) {
                            Text("Đóng HUD", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// Sub Component: App tile (Standard TV-style Tile card)
@Composable
fun AppTileItem(
    app: LauncherApp,
    isFavoriteSection: Boolean,
    onLaunch: () -> Unit,
    onOptionSelected: () -> Unit,
    testTag: String
) {
    var isFocused by remember { mutableStateOf(false) }

    // Animations responsive to D-pad active focus state
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.00f,
        animationSpec = tween(durationMillis = 150),
        label = "TileScale"
    )

    val borderGlowColor by animateColorAsState(
        targetValue = if (isFocused) {
            if (isFavoriteSection) Color(0xFFFFD700) else Color(0xFF00E5FF)
        } else {
            Color.White.copy(alpha = 0.05f)
        },
        animationSpec = tween(durationMillis = 150),
        label = "TileBorderColor"
    )

    val cardBgColor by animateColorAsState(
        targetValue = if (isFocused) {
            Color.White.copy(alpha = 0.15f)
        } else {
            Color.White.copy(alpha = 0.05f)
        },
        animationSpec = tween(durationMillis = 150),
        label = "TileBgColor"
    )

    Card(
        modifier = Modifier
            .scale(scale)
            .width(135.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderGlowColor,
                shape = RoundedCornerShape(16.dp)
            )
            .onFocusChanged { state -> isFocused = state.isFocused }
            .clickable { onLaunch() }
            .focusable()
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Options trigger badge
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (app.isFavorite && !isFavoriteSection) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Pinned App",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Settings cog/expand options on individual app tile click/focus options
                    IconButton(
                        onClick = { onOptionSelected() },
                        modifier = Modifier
                            .size(18.dp)
                            .testTag("app_actions_trigger_${app.packageName}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Option details",
                            tint = Color.White.copy(alpha = if (isFocused) 0.85f else 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Real System App Icon
            AppIconContainer(
                systemIcon = app.systemIcon,
                contentDescription = app.displayName,
                size = 48
            )

            // App title text below
            Text(
                text = app.displayName,
                fontSize = 11.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

// Auxiliary Component: Real icon renderer safely handling system drawables
@Composable
fun AppIconContainer(
    systemIcon: android.graphics.drawable.Drawable?,
    contentDescription: String,
    size: Int
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape((size / 4).dp))
            .padding((size / 8).dp)
    ) {
        if (systemIcon != null) {
            // Render genuine Android resource drawable via coil or custom AndroidView / Image
            val bitmap = remember(systemIcon) {
                try {
                    val width = systemIcon.intrinsicWidth.takeIf { it > 0 } ?: 128
                    val height = systemIcon.intrinsicHeight.takeIf { it > 0 } ?: 128
                    val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bmp)
                    systemIcon.setBounds(0, 0, canvas.width, canvas.height)
                    systemIcon.draw(canvas)
                    bmp.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Tv,
                    contentDescription = contentDescription,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Icon(
                imageVector = Icons.Rounded.Tv,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// Sub Component: Category List Item (Left side menu selection)
@Composable
fun CategoryListItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    var isFocused by remember { mutableStateOf(false) }

    val containerBg by animateColorAsState(
        targetValue = if (isSelected) {
            Color(0xFF00E5FF).copy(alpha = 0.15f)
        } else if (isFocused) {
            Color.White.copy(alpha = 0.08f)
        } else {
            Color.Transparent
        },
        label = "CategoryBg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            Color(0xFF00E5FF)
        } else if (isFocused) {
            Color.White
        } else {
            Color.White.copy(alpha = 0.65f)
        },
        label = "CategoryText"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerBg)
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.4f) else if (isFocused) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged { state -> isFocused = state.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = textColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}

// Sub Component: TV Quick Settings & Wallpaper controller button
@Composable
fun TVMenuButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    colorTint: Color,
    compactMode: Boolean = false,
    testTag: String
) {
    var isFocused by remember { mutableStateOf(false) }

    val bgColored by animateColorAsState(
        targetValue = if (isFocused) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f),
        label = "MenuButtonBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compactMode) 38.dp else 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColored)
            .border(
                width = 1.dp,
                color = if (isFocused) colorTint.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { state -> isFocused = state.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = if (compactMode) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (compactMode) Arrangement.Center else Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isFocused) colorTint else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(if (compactMode) 16.dp else 18.dp)
        )
        if (!compactMode || isFocused) {
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            fontSize = if (compactMode) 10.sp else 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isFocused) Color.White else Color.White.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Sub Component: Center Action Button inside the context menus
@Composable
fun DialogActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    backgroundOpacity: Float = 0.12f,
    onClick: () -> Unit,
    testTag: String
) {
    var isFocused by remember { mutableStateOf(false) }

    val dynamicBg by animateColorAsState(
        targetValue = if (isFocused) color.copy(alpha = 0.25f) else color.copy(alpha = backgroundOpacity),
        label = "DiagBtnBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(dynamicBg)
            .border(
                width = 1.5.dp,
                color = if (isFocused) color else color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { state -> isFocused = state.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isFocused) Color.White else color,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

// Sub Component: System chip
@Composable
fun SystemMetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}
