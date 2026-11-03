package com.bytechnum.reveil.ui.alarms

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytechnum.reveil.R
import com.bytechnum.reveil.domain.model.Alarm
import com.bytechnum.reveil.ui.components.PermissionBanner
import com.bytechnum.reveil.util.MissingPermission
import com.bytechnum.reveil.util.Permissions
import java.time.Duration
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Int) -> Unit,
    viewModel: AlarmListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var missingPermissions by remember { mutableStateOf(Permissions.findMissingPermissions(context)) }
    LifecycleResumeEffect(Unit) {
        missingPermissions = Permissions.findMissingPermissions(context)
        onPauseOrDispose { }
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (!isGranted) {
            context.startActivity(Permissions.createSettingsIntent(context, MissingPermission.NOTIFICATIONS))
        }
        missingPermissions = Permissions.findMissingPermissions(context)
    }
    val testAlarmLabel = stringResource(R.string.test_alarm_label)
    val testAlarmMessage = stringResource(R.string.test_alarm_created)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = {
                        viewModel.createTestAlarm(testAlarmLabel)
                        coroutineScope.launch { snackbarHostState.showSnackbar(testAlarmMessage) }
                    }) { Text(stringResource(R.string.test_alarm_action)) }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAlarm) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_alarm))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PermissionBanner(missingPermissions, onGrant = { permission ->
                    if (permission == MissingPermission.NOTIFICATIONS &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.startActivity(Permissions.createSettingsIntent(context, permission))
                    }
                })
            }
            item { NextAlarmCard(state.next) }
            items(state.alarms, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    onClick = { onEditAlarm(alarm.id) },
                    onToggle = { isEnabled -> viewModel.setEnabled(alarm.id, isEnabled) },
                )
            }
        }
    }
}

@Composable
private fun NextAlarmCard(next: NextAlarm?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (next == null) {
                Text(text = stringResource(R.string.no_alarm_title), style = MaterialTheme.typography.titleLarge)
                Text(text = stringResource(R.string.no_alarm_hint), style = MaterialTheme.typography.bodyLarge)
            } else {
                val now by produceState(ZonedDateTime.now()) {
                    while (true) {
                        delay(30_000)
                        value = ZonedDateTime.now()
                    }
                }
                Text(text = stringResource(R.string.next_alarm), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = formatTime(next.triggerAt.hour, next.triggerAt.minute),
                    style = MaterialTheme.typography.displayMedium,
                )
                Text(
                    text = stringResource(R.string.next_alarm_in, describeDelay(Duration.between(now, next.triggerAt))),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = formatTime(alarm.hour, alarm.minute), style = MaterialTheme.typography.displaySmall)
                Text(text = describeDays(alarm.days), style = MaterialTheme.typography.bodyMedium)
                if (alarm.label.isNotBlank()) Text(text = alarm.label, style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = alarm.isEnabled, onCheckedChange = onToggle)
        }
    }
}
