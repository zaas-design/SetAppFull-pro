package ss.colytitse.setappfull.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import ss.colytitse.setappfull.BuildConfig
import ss.colytitse.setappfull.core.RuleCodec
import ss.colytitse.setappfull.R
import androidx.compose.material3.ripple

private val LightPalette = lightColorScheme(
    primary = Color(0xFF4B5C91), onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EDF9), onPrimaryContainer = Color(0xFF29385F),
    secondary = Color(0xFF326953), secondaryContainer = Color(0xFFE2F0E8),
    background = Color(0xFFF5F6F8), onBackground = Color(0xFF1D222C),
    surface = Color.White, onSurface = Color(0xFF1D222C),
    surfaceVariant = Color(0xFFECEFF3), onSurfaceVariant = Color(0xFF5F6572),
    outline = Color(0xFF8A919E), outlineVariant = Color(0xFFDEE2EA),
)
private val DarkPalette = darkColorScheme(
    primary = Color(0xFFB6C5F4), onPrimary = Color(0xFF24335D),
    primaryContainer = Color(0xFF303F65), onPrimaryContainer = Color(0xFFE3EAFD),
    secondary = Color(0xFF9BD2B5), secondaryContainer = Color(0xFF254737),
    background = Color(0xFF12151B), onBackground = Color(0xFFE9EDF4),
    surface = Color(0xFF1D222B), onSurface = Color(0xFFE9EDF4),
    surfaceVariant = Color(0xFF2B313D), onSurfaceVariant = Color(0xFFB2BAC8),
    outline = Color(0xFF838D9D), outlineVariant = Color(0xFF343C49),
)

@Composable
fun SetAppFullApp(
    state: UiState,
    onRefresh: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onRuleChange: (String, Int, Boolean) -> Unit,
    onScopeRequest: (String) -> Unit,
    onShowSystem: (Boolean) -> Unit,
    onReset: () -> Unit,
    onOpenAppSettings: () -> Unit = {},
    permissionRequestInProgress: Boolean = false,
    onJoinCommunity: () -> Unit = {},
) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(colorScheme = if (dark) DarkPalette else LightPalette) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        var page by rememberSaveable { mutableIntStateOf(0) }
        var detailsPackage by rememberSaveable { mutableStateOf<String?>(null) }
        var pendingSystemPackage by rememberSaveable { mutableStateOf<String?>(null) }
        var pendingScreenshotPackage by rememberSaveable { mutableStateOf<String?>(null) }
        var resetDialog by rememberSaveable { mutableStateOf(false) }
        var accessNoticeDismissed by rememberSaveable { mutableStateOf(false) }
        val accessNeedsAttention = state.appListAccess == AppListAccess.DENIED || state.appListAccess == AppListAccess.ERROR
        LaunchedEffect(state.appListAccess) {
            // A refresh passes through CHECKING; it must not reopen dismissed guidance.
            if (state.appListAccess == AppListAccess.READY) accessNoticeDismissed = false
        }
        val guardedToggle: (String, Boolean) -> Unit = { packageName, enabled ->
            if (enabled && state.apps.firstOrNull { it.packageName == packageName }?.isSystem == true) {
                pendingSystemPackage = packageName
            } else {
                onToggle(packageName, enabled)
            }
        }
        val navigationHeight = (64 * LocalDensity.current.fontScale.coerceIn(1f, 1.55f)).dp
        Box(Modifier.fillMaxSize().imePadding().background(MaterialTheme.colorScheme.background).testTag("app_root")) {
            Column(
                Modifier.fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = 900.dp).fillMaxSize()) {
                    Header(onRefresh)
                    if (permissionRequestInProgress) {
                        Text("Allow app-list access to choose which apps to configure. Select Allow in the system permission dialog.",
                            Modifier.padding(horizontal = 24.dp, vertical = 8.dp).testTag("app_access_purpose"),
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    if (page == 0) {
                        HomePage(state, { page = 1 }, onJoinCommunity, navigationHeight)
                    } else {
                        SettingsPage(state, guardedToggle, onShowSystem,
                            { detailsPackage = it }, { resetDialog = true }, navigationHeight,
                            onOpenAppSettings, onRefresh)
                    }
                }
            }
            SolidNavigation(page, { page = it }, navigationHeight,
                Modifier.align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .padding(horizontal = 28.dp, vertical = 16.dp))
        }
        if (accessNeedsAttention && !accessNoticeDismissed && !permissionRequestInProgress) {
            AlertDialog(
                onDismissRequest = { accessNoticeDismissed = true },
                modifier = Modifier.testTag("app_access_dialog"),
                title = { Text("App-list access required") },
                text = { Text(state.appListAccessMessage) },
                confirmButton = {
                    TextButton(onClick = {
                        accessNoticeDismissed = true
                        onOpenAppSettings()
                    }) { Text("Open permission settings") }
                },
                dismissButton = {
                    TextButton(onClick = { accessNoticeDismissed = true }) { Text("Not now") }
                },
            )
        }
        val detailedApp = state.apps.firstOrNull { it.packageName == detailsPackage }
        if (detailedApp != null) {
            RulesDialog(detailedApp, guardedToggle, onRuleChange, onScopeRequest,
                onScreenshotChange = { packageName, enabled ->
                    if (enabled && !detailedApp.allowScreenshot) pendingScreenshotPackage = packageName
                    else onRuleChange(packageName, RuleCodec.ALLOW_SCREENSHOT, enabled)
                }) { detailsPackage = null }
        }
        val pendingScreenshotApp = state.apps.firstOrNull { it.packageName == pendingScreenshotPackage }
        if (pendingScreenshotApp != null) {
            AlertDialog(
                onDismissRequest = { pendingScreenshotPackage = null },
                modifier = Modifier.testTag("screenshot_confirm_dialog"),
                title = { Text("Allow screenshots in “${pendingScreenshotApp.label}”?") },
                text = { Text("Applies only to this app. DRM, banking, secure pages, and hardware-protected content may still block screenshots. Use only with apps and content you are authorized to capture. Turning this off restores the app’s original secure-window behavior.") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingScreenshotPackage = null
                        onRuleChange(pendingScreenshotApp.packageName, RuleCodec.ALLOW_SCREENSHOT, true)
                    }, modifier = Modifier.testTag("confirm_screenshot")) { Text("Allow screenshots") }
                },
                dismissButton = { TextButton(onClick = { pendingScreenshotPackage = null }) { Text("Cancel") } },
            )
        }
        val pendingSystemApp = state.apps.firstOrNull { it.packageName == pendingSystemPackage }
        if (pendingSystemApp != null) {
            AlertDialog(
                onDismissRequest = { pendingSystemPackage = null },
                modifier = Modifier.testTag("system_enable_dialog"),
                title = { Text("Enable rules for “${pendingSystemApp.label}”?") },
                text = { Text("This changes how this system app displays its windows and may affect system UI, the keyboard, or related features. If display issues occur, turn off its rule and restart the app. Some system components may require a device restart to recover.") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingSystemPackage = null
                        onToggle(pendingSystemApp.packageName, true)
                    }, modifier = Modifier.testTag("confirm_system_enable")) { Text("Enable rule") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingSystemPackage = null },
                        modifier = Modifier.testTag("cancel_system_enable")) { Text("Cancel") }
                },
            )
        }
        if (resetDialog) {
            AlertDialog(
                onDismissRequest = { resetDialog = false },
                title = { Text("Reset app rules?") },
                text = { Text("This turns off fullscreen rules for all apps and restores default options. Restart affected apps to restore their original display. Framework scope will not change.") },
                confirmButton = { TextButton(onClick = { resetDialog = false; onReset() }) { Text("Reset rules") } },
                dismissButton = { TextButton(onClick = { resetDialog = false }) { Text("Cancel") } },
            )
        }
        }
    }
}

@Composable
private fun Header(onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_pro_mark), null, Modifier.size(30.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).testTag("app_title"))
        IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh")) {
            Icon(AppIcons.Refresh, "Refresh framework status and app list", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HomePage(state: UiState, onSettings: () -> Unit, onJoinCommunity: () -> Unit,
    navigationHeight: androidx.compose.ui.unit.Dp) {
    val connected = state.status == FrameworkStatus.CONNECTED
    val statusText = when (state.status) {
        FrameworkStatus.CHECKING -> "Checking"
        FrameworkStatus.CONNECTED -> "Framework connected"
        FrameworkStatus.UNAVAILABLE -> "Framework not connected"
        FrameworkStatus.DISCONNECTED -> "Framework disconnected"
        FrameworkStatus.UNSUPPORTED -> "Framework not supported"
    }
    LazyColumn(Modifier.fillMaxSize().testTag("home_page"),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp,
            bottom = navigationHeight + 64.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Fullscreen", modifier = Modifier.testTag("home_title"),
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Configure display behavior separately for each app.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).background(
                            if (connected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                            Icon(if (connected) AppIcons.Check else AppIcons.Link, null,
                                tint = if (connected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(statusText, modifier = Modifier.testTag("framework_status"),
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(if (connected) "You can manage app rules" else "Enable the module and check the framework first",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    InfoLine("Framework", state.frameworkName.ifBlank { "Unavailable" })
                    InfoLine("Framework version", state.frameworkVersion.ifBlank { "Unavailable" })
                    InfoLine("Xposed API", state.frameworkApi?.toString() ?: "Unavailable", "framework_api")
                    if (state.statusMessage.isNotBlank()) {
                        Text(state.statusMessage, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Surface(onClick = onSettings, shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("manage_apps"), color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.Expand, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Manage apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Fullscreen rules enabled for ${state.enabledCount} apps", color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    Icon(AppIcons.Arrow, "Manage app rules", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        if (state.error != null) item { MessageCard(state.error, true) }
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(20.dp).testTag("quick_start"),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Quick start", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TutorialStep("1", "Enable the module", "Enable SetAppFull in your framework manager.", "tutorial_enable")
                    TutorialStep("2", "Choose apps", "Enable fullscreen here and follow the prompts, or select apps directly in the framework scope.", "tutorial_select")
                    TutorialStep("3", "Check the result", "Restart the target app as prompted and check fullscreen behavior. Tap an app to adjust its rules.", "tutorial_verify")
                    Text("A connected framework does not guarantee that the module is loaded in every target app.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            OutlinedButton(onClick = onJoinCommunity,
                modifier = Modifier.fillMaxWidth().testTag("join_community"),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
                Icon(AppIcons.Link, null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Join the community", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text("Telegram · @tiaxcj", style = MaterialTheme.typography.bodySmall)
                }
                Icon(AppIcons.Arrow, null)
            }
        }
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoLine("Module version", BuildConfig.VERSION_NAME, "module_version")
                    InfoLine("Android version", "${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}", "android_version")
                }
            }
        }
    }
}

@Composable
private fun TutorialStep(number: String, title: String, description: String, tag: String) {
    Row(Modifier.fillMaxWidth().testTag(tag), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(28.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center) {
            Text(number, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, tag: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.weight(0.8f), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1.2f).then(if (tag != null) Modifier.testTag(tag) else Modifier),
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SettingsPage(
    state: UiState, onToggle: (String, Boolean) -> Unit, onShowSystem: (Boolean) -> Unit,
    onDetails: (String) -> Unit, onReset: () -> Unit, navigationHeight: androidx.compose.ui.unit.Dp,
    onOpenAppSettings: () -> Unit, onRefresh: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val apps = remember(state.apps, query, filter, state.showSystem) {
        state.apps.filter { app ->
            (state.showSystem || !app.isSystem) &&
                (query.isBlank() || app.label.contains(query.trim(), ignoreCase = true) ||
                    app.packageName.contains(query.trim(), ignoreCase = true)) &&
                (filter == 0 || (filter == 1 && app.enabled) || (filter == 2 && !app.enabled))
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("settings_page"),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 10.dp,
            bottom = navigationHeight + 64.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("App settings", Modifier.padding(bottom = 6.dp).testTag("settings_title"), style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold)
            Text("Enable fullscreen and follow the prompts. Tap an app to adjust its rules.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.appListAccess == AppListAccess.DENIED || state.appListAccess == AppListAccess.ERROR) item {
            AppListAccessCard(state, onOpenAppSettings, onRefresh)
        }
        item {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(top = 10.dp).testTag("app_search"),
                placeholder = { Text("Search by app name or package name") }, leadingIcon = { Icon(AppIcons.Search, null) },
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { query = "" }) { Icon(AppIcons.Close, "Clear search") } }
                } else null,
                shape = RoundedCornerShape(16.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface))
        }
        item {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Enabled", "Disabled").forEachIndexed { index, label ->
                    FilterChip(filter == index, { filter = index }, { Text(label) },
                        modifier = Modifier.testTag("filter_$index"))
                }
            }
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Show system apps", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(state.showSystem, onShowSystem, modifier = Modifier.testTag("show_system")
                        .semantics { contentDescription = "Show system apps" })
                }
            }
        }
        item {
            Text("${apps.size} apps", Modifier.padding(top = 8.dp, bottom = 2.dp).testTag("app_count"),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.error != null) item { MessageCard(state.error, true) }
        if (state.loading) item {
            Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Loading apps…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!state.loading && apps.isEmpty() && state.appListAccess != AppListAccess.DENIED && state.appListAccess != AppListAccess.ERROR) item {
            Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(AppIcons.Search, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text(if (query.isBlank()) "No apps match these filters" else "No matching app found",
                    style = MaterialTheme.typography.bodyLarge)
                Text("Try another search or filter", Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(apps, key = { it.packageName }) { app ->
            ApplicationCard(app, { onToggle(app.packageName, it) }, { onDetails(app.packageName) })
        }
        if (state.appListAccess != AppListAccess.DENIED && state.appListAccess != AppListAccess.ERROR) item {
            AppListAccessCard(state, onOpenAppSettings, onRefresh)
        }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(onClick = onReset, modifier = Modifier.padding(top = 10.dp).testTag("reset_rules")) {
                    Text("Reset all app rules")
                }
            }
        }
    }
}

@Composable
private fun AppListAccessCard(state: UiState, onOpenAppSettings: () -> Unit, onRefresh: () -> Unit) {
    val ready = state.appListAccess == AppListAccess.READY
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp).testTag("app_list_access"),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(when (state.appListAccess) {
                AppListAccess.CHECKING -> "Checking app-list access"
                AppListAccess.READY -> "App list available"
                AppListAccess.DENIED -> "App-list access restricted"
                AppListAccess.ERROR -> "Could not finish reading the app list"
            }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(state.appListAccessMessage.ifBlank { "Required app-list access is checked automatically at startup." },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.appListAccess != AppListAccess.CHECKING) {
                TextButton(onClick = onOpenAppSettings, modifier = Modifier.testTag("open_app_permissions")) {
                    Text(if (ready) "Incomplete list? Check system access" else "Open permission settings")
                }
            }
            if (state.appListAccess == AppListAccess.DENIED || state.appListAccess == AppListAccess.ERROR) {
                TextButton(onClick = onRefresh, modifier = Modifier.testTag("retry_app_access")) { Text("Check again") }
            }
        }
    }
}

@Composable
private fun ApplicationCard(app: AppRow, onToggle: (Boolean) -> Unit, onDetails: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().testTag("app_row_${app.packageName}").clickable(onClick = onDetails)
            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppIcon(app)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(app.label, modifier = Modifier.testTag("app_label_${app.packageName}"),
                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(app.packageName, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val status = app.syncMessage.ifBlank {
                    if (!app.enabled) "Fullscreen control is off"
                    else when (app.inScope) {
                        true -> "In scope · behavior needs verification"
                        false -> "Add to framework scope"
                        null -> "Scope status unconfirmed"
                    }
                }
                Text(status, Modifier.testTag("app_sync_${app.packageName}"),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (app.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                if (app.allowScreenshot) Text("Screenshots allowed (change in app details)", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary)
            }
            Switch(app.enabled, onToggle, modifier = Modifier.testTag("app_toggle_${app.packageName}")
                .semantics { contentDescription = "Fullscreen rules for ${app.label}" })
        }
    }
}

@Composable
private fun AppIcon(app: AppRow) {
    val bitmap = remember(app.packageName, app.icon) { runCatching { app.icon?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull() }
    if (bitmap != null) Image(bitmap, null, Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
    else Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center) {
        Text(app.label.take(1), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RulesDialog(
    app: AppRow, onToggle: (String, Boolean) -> Unit, onRuleChange: (String, Int, Boolean) -> Unit,
    onScopeRequest: (String) -> Unit, onScreenshotChange: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("rules_dialog"),
        title = { Text(app.label) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                RuleRow("Enable fullscreen rule", "Applies to this app only", app.enabled) { onToggle(app.packageName, it) }
                HorizontalDivider()
                RuleRow("Hide status bar", "Use more space at the top", app.flags and 2 != 0, app.enabled) {
                    onRuleChange(app.packageName, 2, it)
                }
                RuleRow("Hide navigation bar", "Temporarily reveal with an edge swipe", app.flags and 4 != 0, app.enabled) {
                    onRuleChange(app.packageName, 4, it)
                }
                RuleRow("Extend into the camera cutout area", "Allow content near the screen cutout", app.flags and 8 != 0, app.enabled) {
                    onRuleChange(app.packageName, 8, it)
                }
                if (app.packageName == "tv.danmaku.bili") {
                    RuleRow("Bilibili playback layout fix", "Correct system-bar spacing on playback pages while preserving video aspect ratio",
                        app.flags and RuleCodec.DISABLE_BILIBILI_OPTIMIZATION == 0) {
                        onRuleChange(app.packageName, RuleCodec.DISABLE_BILIBILI_OPTIMIZATION, !it)
                    }
                }
                RuleRow("Allow screenshots", "This app only. DRM or hardware-protected content may still block screenshots.",
                    app.allowScreenshot, enabled = true) { onScreenshotChange(app.packageName, it) }
                RuleRow("VPN detection compatibility", "This app only; off by default. Supports common Android VPN status APIs without changing proxy routing or TLS checks.",
                    app.networkEnvironmentCompat, enabled = true) {
                    onRuleChange(app.packageName, RuleCodec.COMPAT_NETWORK_ENVIRONMENT, it)
                }
                HorizontalDivider()
                Text(when (app.inScope) {
                    true -> "Added to framework scope. After changing rules, restart this app and check the result."
                    false -> "Enabling a rule requests scope access automatically. Retry or select the app in the framework if needed."
                    null -> "Could not confirm scope. Check the framework connection and module status."
                }, style = MaterialTheme.typography.bodySmall)
                if (app.inScope != true) OutlinedButton(onClick = { onScopeRequest(app.packageName) },
                    enabled = !app.scopeRequestPending,
                    modifier = Modifier.fillMaxWidth().testTag("request_scope")) {
                    Text(if (app.scopeRequestPending) "Waiting for framework" else "Request scope access")
                }
                if (app.syncMessage.isNotBlank()) Text(app.syncMessage, Modifier.testTag("rule_sync_status"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
                Text("Fullscreen does not change an app’s aspect ratio; spacing inside the app may remain.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Screenshot access is separate from fullscreen rules and can only be enabled here. Turning it off restores the app’s original secure-window behavior.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun RuleRow(label: String, explanation: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onChange, enabled = enabled, modifier = Modifier.semantics { contentDescription = label })
    }
}

@Composable
private fun MessageCard(message: String, error: Boolean) {
    Surface(color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(18.dp)) {
        Text(message, Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.bodySmall,
            color = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun SolidNavigation(
    selected: Int, onSelect: (Int) -> Unit,
    height: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(40.dp)
    // An opaque navigation surface keeps labels legible over every page.
    val navigationColor = MaterialTheme.colorScheme.surface
    Row(modifier.widthIn(max = 380.dp).fillMaxWidth().height(height)
        .background(navigationColor, shape)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
        .padding(7.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Home" to AppIcons.Home, "Apps" to AppIcons.Settings).forEachIndexed { index, (label, icon) ->
            val active = selected == index
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            val focused by interactionSource.collectIsFocusedAsState()
            val background by animateColorAsState(
                if (active) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent, label = "tabColor")
            val scale by animateFloatAsState(
                when { pressed -> 0.95f; focused -> 1.015f; active -> 1f; else -> 0.985f },
                animationSpec = spring(dampingRatio = 0.78f), label = "tabScale")
            val alpha by animateFloatAsState(if (pressed) 0.82f else 1f,
                animationSpec = spring(dampingRatio = 0.8f), label = "tabAlpha")
            Column(Modifier.weight(1f).fillMaxHeight().graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                .clip(RoundedCornerShape(34.dp)).background(background)
                .indication(interactionSource, ripple())
                .selectable(active, role = Role.Tab, onClick = { onSelect(index) },
                    indication = null, interactionSource = interactionSource)
                .focusable()
                .testTag("nav_$index")
                .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Icon(icon, null, Modifier.size(20.dp), tint = color)
                Spacer(Modifier.height(2.dp))
                Text(label, color = color, fontSize = 12.sp, lineHeight = 15.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

private object AppIcons {
    private fun icon(name: String, path: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round, pathBuilder = path)
        }.build()
    val Home = icon("Home") {
        moveTo(3f, 10.5f); lineTo(12f, 3f); lineTo(21f, 10.5f)
        moveTo(5f, 9f); lineTo(5f, 21f); lineTo(10f, 21f); lineTo(10f, 14f)
        lineTo(14f, 14f); lineTo(14f, 21f); lineTo(19f, 21f); lineTo(19f, 9f)
    }
    val Settings = icon("Settings") {
        moveTo(4f, 7f); lineTo(20f, 7f); moveTo(4f, 17f); lineTo(20f, 17f)
        moveTo(9f, 4f); lineTo(9f, 10f); moveTo(15f, 14f); lineTo(15f, 20f)
    }
    val Expand = icon("Expand") {
        moveTo(9f, 3f); lineTo(3f, 3f); lineTo(3f, 9f)
        moveTo(15f, 3f); lineTo(21f, 3f); lineTo(21f, 9f)
        moveTo(3f, 15f); lineTo(3f, 21f); lineTo(9f, 21f)
        moveTo(15f, 21f); lineTo(21f, 21f); lineTo(21f, 15f)
    }
    val Refresh = icon("Refresh") {
        moveTo(20f, 7f); curveTo(16f, 1f, 7f, 2f, 4f, 8f)
        moveTo(20f, 3f); lineTo(20f, 8f); lineTo(15f, 8f)
        moveTo(4f, 17f); curveTo(8f, 23f, 17f, 22f, 20f, 16f)
        moveTo(4f, 21f); lineTo(4f, 16f); lineTo(9f, 16f)
    }
    val Check = icon("Check") { moveTo(5f, 12f); lineTo(10f, 17f); lineTo(20f, 7f) }
    val Arrow = icon("Arrow") {
        moveTo(4f, 12f); lineTo(20f, 12f); moveTo(14f, 6f); lineTo(20f, 12f); lineTo(14f, 18f)
    }
    val Search = icon("Search") {
        moveTo(17f, 10f); arcTo(7f, 7f, 0f, true, true, 3f, 10f)
        arcTo(7f, 7f, 0f, true, true, 17f, 10f); moveTo(15f, 15f); lineTo(21f, 21f)
    }
    val Close = icon("Close") { moveTo(6f, 6f); lineTo(18f, 18f); moveTo(6f, 18f); lineTo(18f, 6f) }
    val Link = icon("Link") {
        moveTo(10f, 8f); lineTo(13f, 5f); curveTo(18f, 1f, 23f, 7f, 19f, 11f); lineTo(16f, 14f)
        moveTo(14f, 16f); lineTo(11f, 19f); curveTo(6f, 23f, 1f, 17f, 5f, 13f); lineTo(8f, 10f)
        moveTo(8f, 16f); lineTo(16f, 8f)
    }
}
