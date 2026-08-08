package com.pvlm.rorafreeze.ui

import android.content.Context
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pvlm.rorafreeze.R
import com.pvlm.rorafreeze.utils.ShizukuUtils
import com.pvlm.rorafreeze.viewmodels.FreezeViewModel
import kotlinx.coroutines.delay

@Composable
fun FreezeSettingsScreen(
    viewModel: FreezeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isShizukuAvailable by viewModel.isShizukuAvailable
    val isShizukuPermissionGranted by viewModel.isShizukuPermissionGranted

    var showDonate by remember { mutableStateOf(false) }
    var showUpdates by remember { mutableStateOf(false) }
    var showSystemConfirm by remember { mutableStateOf(false) }
    var systemCountdown by remember { mutableStateOf(5) }

    LaunchedEffect(Unit) {
        viewModel.check(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
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
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Permission banner
        if (!viewModel.isRootEnabled.value && (!isShizukuAvailable || !isShizukuPermissionGranted)) {
            PermissionBanner(onRequestShizuku = { ShizukuUtils.requestPermission() })
        }

        // Root toggle
        Card {
            RowToggle(
                title = stringResource(R.string.freeze_use_root_title),
                subtitle = if (viewModel.isRootAvailable.value) {
                    stringResource(R.string.freeze_use_root_desc)
                } else {
                    stringResource(R.string.freeze_use_root_unavailable)
                },
                iconRes = R.drawable.rounded_shield_24,
                checked = viewModel.isRootEnabled.value,
                enabled = viewModel.isRootAvailable.value,
                onCheckedChange = { viewModel.setUseRoot(it) }
            )
        }

        // Risk policy toggle
        Card {
            RowToggle(
                title = stringResource(R.string.freeze_allow_system_title),
                subtitle = stringResource(R.string.freeze_allow_system_desc),
                iconRes = R.drawable.rounded_warning_24,
                checked = viewModel.allowSystemApps.value,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        systemCountdown = 5
                        showSystemConfirm = true
                    } else {
                        viewModel.setAllowSystemApps(false)
                    }
                }
            )
        }

        if (showSystemConfirm) {
            SystemRiskConfirmDialog(
                countdown = systemCountdown,
                onDismiss = { showSystemConfirm = false },
                onConfirm = {
                    viewModel.setAllowSystemApps(true)
                    showSystemConfirm = false
                }
            )
            LaunchedEffect(systemCountdown) {
                if (systemCountdown > 0) {
                    delay(1000)
                    systemCountdown -= 1
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.rounded_warning_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = stringResource(R.string.freeze_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }

        // Why RoraFreeze
        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = stringResource(R.string.about_why_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.about_why_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(12.dp))
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sameerasw/essentials")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.about_essentials_link))
                }
            }
        }

        // Support & Links
        Card {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.about_support_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = { showDonate = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_donate))
                }
                OutlinedButton(
                    onClick = { showUpdates = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_check_updates))
                }
                val feedbackSubject = stringResource(R.string.feedback_email_subject)
                val feedbackBody = stringResource(R.string.feedback_email_body)
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:pvlm.contact@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, feedbackSubject)
                            putExtra(Intent.EXTRA_TEXT, feedbackBody)
                        }
                        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_feedback))
                }
            }
        }

        // About / credits
        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = stringResource(R.string.about_credits_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = stringResource(R.string.about_credits_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(8.dp))
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://pvlm.site")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_visit_website))
                }
            }
        }
    }

    if (showDonate) {
        AlertDialog(
            onDismissRequest = { showDonate = false },
            title = { Text(stringResource(R.string.donate_title), textAlign = TextAlign.Center) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.gcash_qr),
                        contentDescription = null,
                        modifier = Modifier.size(260.dp)
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = stringResource(R.string.donate_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Column {
                    OutlinedButton(
                        onClick = {
                            val saved = saveQrToGallery(context)
                            Toast.makeText(
                                context,
                                if (saved) context.getString(R.string.qr_saved) else context.getString(R.string.qr_save_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_save_qr))
                    }
                    Spacer(Modifier.size(8.dp))
                    TextButton(onClick = { showDonate = false }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.select_apps_close))
                    }
                }
            }
        )
    }

    if (showUpdates) {
        AlertDialog(
            onDismissRequest = { showUpdates = false },
            title = { Text(stringResource(R.string.updates_title)) },
            text = { Text(stringResource(R.string.updates_latest)) },
            confirmButton = {
                TextButton(onClick = { showUpdates = false }) {
                    Text(stringResource(R.string.select_apps_close))
                }
            }
        )
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
private fun PermissionBanner(onRequestShizuku: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.freeze_permission_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.size(12.dp))
            Button(
                onClick = onRequestShizuku,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.freeze_request_shizuku))
            }
        }
    }
}

@Composable
private fun RowToggle(
    title: String,
    subtitle: String,
    iconRes: Int,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun SystemRiskConfirmDialog(
    countdown: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.freeze_allow_system_confirm_title)) },
        text = { Text(stringResource(R.string.freeze_allow_system_confirm_message, countdown)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = countdown == 0
            ) {
                Text(if (countdown > 0) {
                    stringResource(R.string.freeze_allow_system_confirm_wait, countdown)
                } else {
                    stringResource(R.string.freeze_allow_system_confirm_enable)
                })
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun saveQrToGallery(context: Context): Boolean {
    return try {
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.gcash_qr) ?: return false
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 900
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 900
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "RoraFreeze_donation_QR.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/RoraFreeze")
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        } ?: return false
        bitmap.recycle()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}