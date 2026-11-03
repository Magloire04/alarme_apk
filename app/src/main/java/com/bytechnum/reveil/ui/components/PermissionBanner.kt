package com.bytechnum.reveil.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bytechnum.reveil.R
import com.bytechnum.reveil.util.MissingPermission

@Composable
fun PermissionBanner(
    missingPermissions: Set<MissingPermission>,
    onGrant: (MissingPermission) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (missingPermissions.isEmpty()) return
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(R.string.permissions_title), style = MaterialTheme.typography.titleMedium)
            missingPermissions.forEach { permission ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(permission.labelRes()), modifier = Modifier.weight(1f))
                    Button(onClick = { onGrant(permission) }) { Text(stringResource(R.string.permission_grant)) }
                }
            }
        }
    }
}

private fun MissingPermission.labelRes(): Int = when (this) {
    MissingPermission.NOTIFICATIONS -> R.string.permission_notifications
    MissingPermission.FULL_SCREEN -> R.string.permission_full_screen
    MissingPermission.EXACT_ALARM -> R.string.permission_exact_alarm
}
