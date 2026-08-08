package com.pvlm.rorafreeze.ui.activities

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pvlm.rorafreeze.R
import com.pvlm.rorafreeze.ui.FreezeGridScreen
import com.pvlm.rorafreeze.ui.FreezeSettingsScreen
import com.pvlm.rorafreeze.ui.SettingsTutorialScreen
import com.pvlm.rorafreeze.ui.theme.FreezeAppsTheme
import com.pvlm.rorafreeze.viewmodels.FreezeViewModel

class AppFreezingActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: FreezeViewModel = viewModel()
            val prefs = remember { getSharedPreferences("freeze_apps_prefs", Context.MODE_PRIVATE) }
            var showTutorial by remember { mutableStateOf(!prefs.getBoolean("tutorial_completed", false)) }
            var showSettings by remember { mutableStateOf(false) }
            var showAgreement by remember { mutableStateOf(prefs.getBoolean("tutorial_completed", false)) }

            FreezeAppsTheme {
                BackHandler(enabled = showSettings) {
                    showSettings = false
                }
                if (showTutorial) {
                    SettingsTutorialScreen(
                        onFinish = {
                            prefs.edit().putBoolean("tutorial_completed", true).apply()
                            showTutorial = false
                            showAgreement = true
                        }
                    )
                } else if (showAgreement) {
                    AgreementDialog(
                        onAgree = { showAgreement = false },
                        onCancel = { finish() }
                    )
                } else {
                    MainContent(
                        viewModel = viewModel,
                        showSettings = showSettings,
                        onOpenSettings = { showSettings = true },
                        onCloseSettings = { showSettings = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun AgreementDialog(
    onAgree: () -> Unit,
    onCancel: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.agreement_title)) },
        text = { Text(stringResource(R.string.agreement_message)) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onAgree) {
                Text(stringResource(R.string.agreement_agree))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onCancel) {
                Text(stringResource(R.string.agreement_cancel))
            }
        }
    )
}

@Composable
private fun MainContent(
    viewModel: FreezeViewModel,
    showSettings: Boolean,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    if (showSettings) {
                        Text(stringResource(R.string.freeze_tab_settings))
                    } else {
                        Column {
                            Text(stringResource(R.string.freeze_apps_title))
                            Text(
                                text = stringResource(R.string.freeze_apps_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (showSettings) {
                        androidx.compose.material3.IconButton(onClick = onCloseSettings) {
                            Icon(
                                painterResource(R.drawable.rounded_arrow_back_24),
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    }
                },
                actions = {
                    if (!showSettings) {
                        androidx.compose.material3.IconButton(onClick = onOpenSettings) {
                            Icon(
                                painterResource(R.drawable.rounded_settings_24),
                                contentDescription = stringResource(R.string.freeze_tab_settings)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showSettings) {
                FreezeSettingsScreen(viewModel = viewModel)
            } else {
                FreezeGridScreen(
                    viewModel = viewModel,
                    onSettingsClick = onOpenSettings,
                    onAppLaunched = {}
                )
            }
        }
    }
}