package com.droidnova.fliptomute.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.droidnova.fliptomute.R

@Composable
fun SetupChecklistItem(
    title: String,
    description: String,
    actionLabel: String,
    status: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAction: Boolean = true,
    statusIcon: ImageVector,
    statusGranted: Boolean,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = description, style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val statusColor = if (statusGranted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                    Icon(statusIcon, contentDescription = null, tint = statusColor)
                    Text(
                        text = stringResource(R.string.setup_access_status, status),
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            if (showAction) {
                Button(onClick = onClick) { Text(actionLabel) }
            }
        }
    }
}
