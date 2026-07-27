package com.droidnova.fliptomute.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.ui.screens.home.MonitoringStatus
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme

@Composable
fun StatusCard(status: MonitoringStatus, isEnabled: Boolean, modifier: Modifier = Modifier) {
    val content = statusContent(status)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    content.icon,
                    contentDescription = null,
                    tint = when (status) {
                        MonitoringStatus.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
                        MonitoringStatus.SETUP_REQUIRED -> MaterialTheme.colorScheme.error
                        MonitoringStatus.ACTIVE -> MaterialTheme.colorScheme.primary
                    },
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(content.title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(content.description), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.enable_feature), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.enable_feature_description),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = isEnabled, onCheckedChange = null, enabled = false)
            }
            if (status == MonitoringStatus.SETUP_REQUIRED) {
                Text(
                    stringResource(R.string.complete_setup_hint),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private data class StatusContent(val title: Int, val description: Int, val icon: ImageVector)

private fun statusContent(status: MonitoringStatus) = when (status) {
    MonitoringStatus.DISABLED -> StatusContent(
        R.string.status_off_title,
        R.string.status_off_description,
        Icons.Default.Info,
    )
    MonitoringStatus.SETUP_REQUIRED -> StatusContent(
        R.string.status_setup_title,
        R.string.status_setup_description,
        Icons.Default.Warning,
    )
    MonitoringStatus.ACTIVE -> StatusContent(
        R.string.status_active_title,
        R.string.status_active_description,
        Icons.Default.CheckCircle,
    )
}

@Preview(showBackground = true)
@Composable
private fun StatusCardPreview() {
    FlipToMuteTheme(dynamicColor = false) {
        StatusCard(MonitoringStatus.SETUP_REQUIRED, false, Modifier.padding(16.dp))
    }
}
