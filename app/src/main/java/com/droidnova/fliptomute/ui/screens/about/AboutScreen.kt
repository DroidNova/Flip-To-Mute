package com.droidnova.fliptomute.ui.screens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                .verticalScroll(rememberScrollState()),
        ) {
            AppHeader(Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            AboutItem(stringResource(R.string.rate_us), stringResource(R.string.rate_us_description), Icons.Filled.Star) {
                AboutIntentUtil.openPlayStore(context)
            }
            AboutItem(stringResource(R.string.share_app), stringResource(R.string.share_app_description), Icons.Filled.Share) {
                AboutIntentUtil.shareApp(context)
            }
            AboutItem(stringResource(R.string.report_bugs), stringResource(R.string.report_bugs_description), Icons.Filled.BugReport) {
                AboutIntentUtil.sendBugReport(context)
            }
            AboutItem(
                stringResource(R.string.follow_instagram),
                stringResource(R.string.follow_instagram_description),
                Icons.Filled.CameraAlt,
                iconTint = Color(0xFFE1306C),
            ) { AboutIntentUtil.openUrl(context, context.getString(R.string.instagram_url)) }
            AboutItem(
                stringResource(R.string.join_whatsapp),
                stringResource(R.string.join_whatsapp_description),
                Icons.Filled.Chat,
                iconTint = Color(0xFF25D366),
            ) { AboutIntentUtil.openUrl(context, context.getString(R.string.whatsapp_url)) }
            AboutItem(
                stringResource(R.string.app_version),
                AboutIntentUtil.appVersion(context),
                Icons.Filled.Info,
            )

            Text(
                text = stringResource(R.string.check_other_apps),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
            )
            FeaturedAppCard(
                stringResource(R.string.background_video_recorder),
                stringResource(R.string.background_video_recorder_description),
                Icons.Filled.Videocam,
            ) { AboutIntentUtil.searchPlayStore(context, context.getString(R.string.background_video_recorder)) }
            FeaturedAppCard(
                stringResource(R.string.flash_alerts),
                stringResource(R.string.flash_alerts_description),
                Icons.Filled.FlashOn,
            ) { AboutIntentUtil.searchPlayStore(context, context.getString(R.string.flash_alerts)) }
            FeaturedAppCard(
                stringResource(R.string.more_apps_play_store),
                "",
                Icons.Filled.Shop,
            ) { AboutIntentUtil.openDeveloperApps(context) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AppHeader(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primary,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_notification_flip),
                contentDescription = stringResource(R.string.app_icon_description),
                modifier = Modifier.padding(14.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(R.string.about_app_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun AboutItem(
    heading: String,
    description: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(heading, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FeaturedAppCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp).size(26.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (description.isNotEmpty()) {
                    Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    Divider(color = Color.Transparent, thickness = 4.dp)
}
