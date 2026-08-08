package com.pvlm.rorafreeze.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pvlm.rorafreeze.R
import com.pvlm.rorafreeze.domain.model.NotificationApp
import com.pvlm.rorafreeze.utils.AppSafety
import com.pvlm.rorafreeze.utils.AppSafetyLevel
import com.pvlm.rorafreeze.utils.AppUtil
import com.pvlm.rorafreeze.viewmodels.FreezeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val TOGGLE_BLOCKLIST = setOf(
    "moe.shizuku.manager",
    "moe.shizuku.privileged.api",
    "moe.shizuku.api",
    "com.android.settings",
    "com.android.systemui"
)

@Composable
fun AppSelectionSheet(
    viewModel: FreezeViewModel,
    onDismiss: () -> Unit,
    titleText: String,
    modeId: String,
    modePackages: List<String> = emptyList()
) {
    val context = LocalContext.current

    var showExportConfirm by remember { mutableStateOf(false) }

    var installedApps by remember { mutableStateOf<List<NotificationApp>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val selected = remember { mutableStateMapOf<String, Boolean>() }
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SortOption.NAME_ASC) }
    var filterOption by remember { mutableStateOf(FilterOption.ALL) }

    LaunchedEffect(Unit) {
        val apps = withContext(Dispatchers.IO) { AppUtil.getInstalledApps(context) }
        installedApps = apps
        modePackages.forEach { selected[it] = true }
        loading = false
    }

    val safety = remember(installedApps) {
        installedApps.associate { it.packageName to AppSafety.getSafetyLevel(context, it) }
    }

    val groups = remember(installedApps, safety, searchQuery, sortOption, filterOption) {
        val filtered = installedApps.filter { app ->
            val matchesQuery = searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filterOption) {
                FilterOption.ALL -> true
                FilterOption.USER -> !app.isSystemApp
                FilterOption.SYSTEM -> app.isSystemApp
            }
            matchesQuery && matchesFilter
        }
        val comparator = when (sortOption) {
            SortOption.NAME_ASC -> compareBy<NotificationApp> { it.appName.lowercase() }
            SortOption.NAME_DESC -> compareByDescending<NotificationApp> { it.appName.lowercase() }
            SortOption.NEWEST -> compareByDescending<NotificationApp> { it.lastUpdated }
        }
        val recommended = filtered.filter { safety[it.packageName] == AppSafetyLevel.RECOMMENDED }.sortedWith(comparator)
        val allApps = filtered.filter { safety[it.packageName] == AppSafetyLevel.SAFE }.sortedWith(comparator)
        val critical = filtered.filter { safety[it.packageName] == AppSafetyLevel.CRITICAL }.sortedWith(comparator)
        listOfNotNull(
            if (recommended.isNotEmpty()) Pair<AppSafetyLevel?, List<NotificationApp>>(AppSafetyLevel.RECOMMENDED, recommended) else null,
            if (allApps.isNotEmpty()) Pair<AppSafetyLevel?, List<NotificationApp>>(AppSafetyLevel.SAFE, allApps) else null,
            if (critical.isNotEmpty()) Pair<AppSafetyLevel?, List<NotificationApp>>(AppSafetyLevel.CRITICAL, critical) else null
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showExportConfirm = true }) {
                        Icon(
                            painterResource(R.drawable.rounded_export_24),
                            contentDescription = stringResource(R.string.action_export_freeze)
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.select_apps_close))
                    }
                }
                SearchField(
                    searchQuery = searchQuery,
                    onQueryChange = { searchQuery = it },
                    view = LocalView.current,
                    placeholderRes = R.string.freeze_selected_apps_search_hint,
                    horizontalPadding = 0.dp
                )
                SortFilterRow(
                    sortOption = sortOption,
                    filterOption = filterOption,
                    onSortChange = { sortOption = it },
                    onFilterChange = { filterOption = it }
                )

                if (loading) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.freeze_loading_apps),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        groups.forEach { (level, apps) ->
                            item(key = "header_${level?.name ?: "all"}") {
                                SectionHeader(level = level)
                            }
                            items(apps, key = { it.packageName }) { app ->
                                AppPickerRow(
                                    app = app,
                                    level = safety[app.packageName] ?: AppSafetyLevel.SAFE,
                                    allowSystemApps = viewModel.allowSystemApps.value,
                                    checked = selected[app.packageName] ?: false,
                                    onCheckedChange = { checked ->
                                        selected[app.packageName] = checked
                                    }
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val packages = installedApps
                            .filter { selected[it.packageName] == true }
                            .map { it.packageName }
                        viewModel.updateModeApps(context, modeId, packages)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Text(stringResource(R.string.select_apps_save))
                }
            }
        }
    }

    if (showExportConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showExportConfirm = false },
            title = { Text(stringResource(R.string.confirm_export_mode_title)) },
            text = { Text(stringResource(R.string.confirm_export_mode_message, titleText)) },
            confirmButton = {
                TextButton(onClick = {
                    val exported = viewModel.exportModeToFolder(context, modeId)
                    Toast.makeText(
                        context,
                        if (exported) R.string.freeze_export_folder_saved else R.string.freeze_export_folder_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                    showExportConfirm = false
                }) {
                    Text(stringResource(R.string.action_save), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(level: AppSafetyLevel?) {
    val title = when (level) {
        AppSafetyLevel.RECOMMENDED -> stringResource(R.string.section_recommended)
        null -> stringResource(R.string.section_all_apps)
        AppSafetyLevel.SAFE -> stringResource(R.string.section_all_apps)
        AppSafetyLevel.CRITICAL -> stringResource(R.string.section_critical)
    }
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun AppPickerRow(
    app: NotificationApp,
    level: AppSafetyLevel,
    allowSystemApps: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(
            packageName = app.packageName,
            modifier = Modifier.size(36.dp)
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                app.appName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            SafetyBadge(level = level)
        }
        val showToggle = when {
            app.packageName in TOGGLE_BLOCKLIST -> false
            level == AppSafetyLevel.CRITICAL -> allowSystemApps
            else -> true
        }
        if (showToggle) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun SafetyBadge(level: AppSafetyLevel) {
    if (level == AppSafetyLevel.SAFE) return
    val (labelRes, container, content) = when (level) {
        AppSafetyLevel.RECOMMENDED -> Triple(
            R.string.app_safety_recommended,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )

        AppSafetyLevel.SAFE -> Triple(
            R.string.app_safety_safe,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )

        AppSafetyLevel.CRITICAL -> Triple(
            R.string.app_safety_critical,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = container,
        modifier = Modifier.padding(top = 2.dp)
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}

private enum class SortOption { NAME_ASC, NAME_DESC, NEWEST }

private enum class FilterOption { ALL, USER, SYSTEM }

@Composable
private fun SortFilterRow(
    sortOption: SortOption,
    filterOption: FilterOption,
    onSortChange: (SortOption) -> Unit,
    onFilterChange: (FilterOption) -> Unit
) {
    var sortExpanded by remember { mutableStateOf(false) }
    var filterExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ControlChip(
            label = when (sortOption) {
                SortOption.NAME_ASC -> stringResource(R.string.sort_option_name_asc)
                SortOption.NAME_DESC -> stringResource(R.string.sort_option_name_desc)
                SortOption.NEWEST -> stringResource(R.string.sort_option_newest)
            },
            iconRes = R.drawable.rounded_sort_24,
            expanded = sortExpanded,
            onClick = { sortExpanded = true },
            onDismiss = { sortExpanded = false },
            modifier = Modifier.weight(1f)
        ) {
            SortOption.entries.forEach { option ->
                val label = when (option) {
                    SortOption.NAME_ASC -> stringResource(R.string.sort_option_name_asc)
                    SortOption.NAME_DESC -> stringResource(R.string.sort_option_name_desc)
                    SortOption.NEWEST -> stringResource(R.string.sort_option_newest)
                }
                DropdownMenuItem(
                    text = { Text(label) },
                    leadingIcon = {
                        if (option == sortOption) {
                            Icon(painterResource(R.drawable.rounded_check_24), contentDescription = null)
                        }
                    },
                    onClick = { onSortChange(option); sortExpanded = false }
                )
            }
        }
        ControlChip(
            label = when (filterOption) {
                FilterOption.ALL -> stringResource(R.string.filter_all)
                FilterOption.USER -> stringResource(R.string.filter_user)
                FilterOption.SYSTEM -> stringResource(R.string.filter_system)
            },
            iconRes = R.drawable.rounded_filter_list_24,
            expanded = filterExpanded,
            onClick = { filterExpanded = true },
            onDismiss = { filterExpanded = false },
            modifier = Modifier.weight(1f)
        ) {
            FilterOption.entries.forEach { option ->
                val label = when (option) {
                    FilterOption.ALL -> stringResource(R.string.filter_all)
                    FilterOption.USER -> stringResource(R.string.filter_user)
                    FilterOption.SYSTEM -> stringResource(R.string.filter_system)
                }
                DropdownMenuItem(
                    text = { Text(label) },
                    leadingIcon = {
                        if (option == filterOption) {
                            Icon(painterResource(R.drawable.rounded_check_24), contentDescription = null)
                        }
                    },
                    onClick = { onFilterChange(option); filterExpanded = false }
                )
            }
        }
    }
}

@Composable
private fun ControlChip(
    label: String,
    iconRes: Int,
    expanded: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (expanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            content()
        }
    }
}