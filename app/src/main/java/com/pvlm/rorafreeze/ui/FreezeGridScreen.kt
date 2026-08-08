package com.pvlm.rorafreeze.ui

import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.BasicTextField
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pvlm.rorafreeze.R
import com.pvlm.rorafreeze.domain.model.FreezeMode
import com.pvlm.rorafreeze.utils.HapticUtil
import com.pvlm.rorafreeze.viewmodels.FreezeViewModel

@Composable
fun FreezeGridScreen(
    viewModel: FreezeViewModel,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onGetStartedClick: (() -> Unit)? = null,
    onAppLaunched: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var modeToEdit by remember { mutableStateOf<FreezeMode?>(null) }
    var showAddModeDialog by remember { mutableStateOf(false) }
    var modeToDelete by remember { mutableStateOf<FreezeMode?>(null) }
    var modeToActivate by remember { mutableStateOf<FreezeMode?>(null) }
    var modeToDeactivate by remember { mutableStateOf<FreezeMode?>(null) }

    val modes = viewModel.freezeModes.value
    val activeModeId = viewModel.activeModeId.value
    val isShizukuPermissionGranted by viewModel.isShizukuPermissionGranted
    val isRootEnabled by viewModel.isRootEnabled
    val canFreeze = isRootEnabled || isShizukuPermissionGranted

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val name = context.contentResolver.query(it, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
                if (name != null && !name.endsWith(".prrf", ignoreCase = true)) {
                    Toast.makeText(context, context.getString(R.string.import_prrf_only), Toast.LENGTH_SHORT).show()
                    return@let
                }
                context.contentResolver.openInputStream(it)?.use { stream -> viewModel.importModes(context, stream) }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.check(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        if (!canFreeze && onSettingsClick != null) {
            PermissionNotGrantedBanner(onOpenSettings = onSettingsClick)
        }
        if (modes.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).padding(bottom = 160.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painterResource(R.drawable.rounded_widgets_24),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(16.dp))
                Text(
                    text = stringResource(R.string.modes_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showAddModeDialog = true }) {
                        Icon(painterResource(R.drawable.rounded_add_24), contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.action_add_mode))
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/octet-stream")) }) {
                        Icon(painterResource(R.drawable.rounded_file_open_24), contentDescription = stringResource(R.string.action_import), modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { showAddModeDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(painterResource(R.drawable.rounded_add_24), contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.action_add_mode))
                        }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/octet-stream")) }) {
                            Icon(painterResource(R.drawable.rounded_file_open_24), contentDescription = stringResource(R.string.action_import), modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(modes, key = { it.id }) { mode ->
                    ModeCard(
                        name = mode.name,
                        appCount = mode.packageNames.size,
                        isActive = mode.id == activeModeId,
                        enabled = canFreeze,
                        onActivate = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            modeToActivate = mode
                        },
                        onDeactivate = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            modeToDeactivate = mode
                        },
                        onOpen = { modeToEdit = mode },
                        onDelete = { modeToDelete = mode }
                    )
                }
            }
        }
    }

    modeToEdit?.let { mode ->
        AppSelectionSheet(
            viewModel = viewModel,
            onDismiss = { modeToEdit = null },
            titleText = mode.name,
            modeId = mode.id,
            modePackages = mode.packageNames
        )
    }

    modeToDelete?.let { mode ->
        AlertDialog(
            onDismissRequest = { modeToDelete = null },
            title = { Text(stringResource(R.string.confirm_delete_mode_title)) },
            text = { Text(stringResource(R.string.confirm_delete_mode_message, mode.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMode(context, mode.id)
                    modeToDelete = null
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { modeToDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    modeToActivate?.let { mode ->
        AlertDialog(
            onDismissRequest = { modeToActivate = null },
            title = { Text(stringResource(R.string.confirm_activate_mode_title)) },
            text = { Text(stringResource(R.string.confirm_activate_mode_message, mode.packageNames.size, mode.name)) },
            confirmButton = {
                TextButton(onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.activateMode(context, mode.id)
                    modeToActivate = null
                }) {
                    Text(stringResource(R.string.action_freeze), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { modeToActivate = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    modeToDeactivate?.let { mode ->
        AlertDialog(
            onDismissRequest = { modeToDeactivate = null },
            title = { Text(stringResource(R.string.confirm_deactivate_mode_title)) },
            text = { Text(stringResource(R.string.confirm_deactivate_mode_message, mode.packageNames.size, mode.name)) },
            confirmButton = {
                TextButton(onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.deactivateMode(context, mode.id)
                    modeToDeactivate = null
                }) {
                    Text(stringResource(R.string.action_unfreeze), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { modeToDeactivate = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showAddModeDialog) {
        AddModeDialog(
            onDismiss = { showAddModeDialog = false },
            onConfirm = { name ->
                val newMode = viewModel.addMode(name)
                showAddModeDialog = false
                if (newMode != null) {
                    modeToEdit = newMode
                }
            }
        )
    }
}

@Composable
fun SearchField(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    view: android.view.View,
    placeholderRes: Int = R.string.search_frozen_apps_placeholder,
    horizontalPadding: Dp = 16.dp
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(R.drawable.rounded_search_24),
                contentDescription = stringResource(R.string.label_search_content_description),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(8.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f)
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painterResource(R.drawable.rounded_close_24),
                        contentDescription = stringResource(R.string.action_stop),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModeCard(
    name: String,
    appCount: Int,
    isActive: Boolean,
    enabled: Boolean,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val container = if (isActive) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val activeColor = Color(0xFF1A73E8)
    val containerBorder = if (isActive) {
        activeColor
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = container,
        border = BorderStroke(1.dp, containerBorder),
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(onClick = onOpen, onLongClick = onDelete)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.size(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isActive) {
                                activeColor
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = if (isActive) {
                                    stringResource(R.string.modes_active_suffix)
                                } else {
                                    stringResource(R.string.modes_inactive_suffix)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isActive) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.modes_apps_count, appCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.size(16.dp))
            Button(
                onClick = if (isActive) onDeactivate else onActivate,
                enabled = enabled,
                colors = if (isActive) {
                    ButtonDefaults.buttonColors(
                        containerColor = activeColor,
                        contentColor = Color.White
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                },
                border = null,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(
                    painterResource(if (isActive) R.drawable.rounded_mode_cool_off_24 else R.drawable.rounded_mode_cool_24),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    if (isActive) {
                        stringResource(R.string.action_deactivate)
                    } else {
                        stringResource(R.string.action_activate)
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionNotGrantedBanner(onOpenSettings: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.freeze_open_settings_to_grant),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.size(12.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.action_open_settings))
            }
        }
    }
}

@Composable
private fun AddModeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_mode_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 24) name = it },
                label = { Text(stringResource(R.string.add_mode_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("${name.length}/24") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
