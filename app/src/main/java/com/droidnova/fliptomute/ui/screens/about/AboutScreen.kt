package com.droidnova.fliptomute.ui.screens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.ui.components.AppTopBar
import com.droidnova.fliptomute.util.AboutIntentUtil

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(stringResource(R.string.about_title), onBack) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppHeader()
            Spacer(Modifier.height(8.dp))
            AboutItem(
                heading = stringResource(R.string.rate_us),
                description = stringResource(R.string.rate_us_description),
                icon = Icons.Filled.Star,
                onClick = { AboutIntentUtil.openPlayStore(context) },
            )
            AboutItem(
                heading = stringResource(R.string.share_app),
                description = stringResource(R.string.share_app_description),
                icon = Icons.Filled.Share,
                onClick = { AboutIntentUtil.shareApp(context) },
            )
            AboutItem(
                heading = stringResource(R.string.report_bugs),
                description = stringResource(R.string.report_bugs_description),
                icon = Icons.Filled.BugReport,
                onClick = { AboutIntentUtil.sendBugReport(context) },
            )
            AboutItem(
                heading = stringResource(R.string.app_version),
                description = AboutIntentUtil.appVersion(context),
                icon = Icons.Filled.Info,
            )
        }
    }
}

@Composable
private fun AppHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher),
            contentDescription = stringResource(R.string.app_icon_description),
            modifier = Modifier.size(88.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.about_app_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AboutItem(
    heading: String,
    description: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(heading, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
