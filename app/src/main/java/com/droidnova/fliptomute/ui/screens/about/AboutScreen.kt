package com.droidnova.fliptomute.ui.screens.about

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.droidnova.fliptomute.core.utils.about_utils.IntentUtil
import com.droidnova.fliptomute.core.utils.about_utils.getFeaturedOtherApps
import com.droidnova.fliptomute.ui.components.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val otherApps = remember { getFeaturedOtherApps() }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            AppHeader()
            SpacerHeight(16.dp)

            AboutItem(
                headingText = "Rate us",
                labelText = "Support us with a review",
                icon = Icons.Filled.Star,
                onClick = { IntentUtil.openRateUs(context) }
            )
            SpacerHeight(8.dp)

            AboutItem(
                headingText = "Share app",
                labelText = "Share this wallpaper app",
                icon = Icons.Filled.Share,
                onClick = { IntentUtil.shareApp(context) }
            )
            SpacerHeight(8.dp)

            AboutItem(
                headingText = "Report bugs",
                labelText = "Send issue details via email",
                icon = Icons.Filled.BugReport,
                onClick = { IntentUtil.sendSupportMail(context, isBug = true) }
            )
            SpacerHeight(8.dp)

            AboutItem(
                headingText = "Follow on Instagram",
                labelText = "Join our Instagram page",
                drawableIconRes = R.drawable.ic_instagram,
                onClick = { IntentUtil.openInstagram(context) },
                overrideTint = false
            )
            SpacerHeight(8.dp)

            AboutItem(
                headingText = "Join WhatsApp Group",
                labelText = "Join our official WhatsApp community",
                drawableIconRes = R.drawable.ic_whatsapp,
                onClick = { IntentUtil.openWhatsApp(context) },
                overrideTint = false
            )
            SpacerHeight(8.dp)

            AboutItem(
                headingText = "App version",
                labelText = IntentUtil.fetchAppVersion(context),
                icon = Icons.Filled.Info,
                enabled = false,
                onClick = {}
            )
            SpacerHeight(8.dp)

            Text(
                text = "Check out other apps",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            SpacerHeight(8.dp)

            otherApps.forEach { app ->
                AppCard(
                    onClick = { IntentUtil.openPlayStore(context, app.packageName) },
                    title = app.title,
                    description = app.description,
                    iconRes = app.iconRes
                )
                SpacerHeight(8.dp)
            }

            AppCard(
                onClick = { IntentUtil.openDeveloperPlayConsole(context) },
                title = "More apps on Play Store",
                description = "",
                iconRes = R.drawable.ic_play_store
            )
        }
    }
}

@Composable
private fun AppHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_flip_to_mute),
            contentDescription = "App icon",
            modifier = Modifier.size(72.dp)
        )
        SpacerHeight(12.dp)
        Text(
            text = "4K HD Wallpaper",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        SpacerHeight(4.dp)
        Text(
            text = "Discover and apply static and live wallpapers.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AppCard(
    onClick: () -> Unit,
    title: String,
    description: String,
    @DrawableRes iconRes: Int
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = title,
                modifier = Modifier.size(50.dp),
                tint = Color.Unspecified
            )
            SpacerWidth(8)
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutItem(
    headingText: String,
    labelText: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    @DrawableRes drawableIconRes: Int? = null,
    enabled: Boolean = true,
    overrideTint: Boolean = true
) {
    val rowModifier = if (enabled) {
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    } else {
        Modifier.fillMaxWidth()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = rowModifier
    ) {
        when {
            drawableIconRes != null -> {
                Icon(
                    painter = painterResource(drawableIconRes),
                    contentDescription = headingText,
                    modifier = Modifier.padding(12.dp),
                    tint = if (overrideTint) MaterialTheme.colorScheme.onSurface else Color.Unspecified
                )
            }

            icon != null -> {
                Icon(
                    imageVector = icon,
                    contentDescription = headingText,
                    modifier = Modifier.padding(12.dp),
                    tint = if (overrideTint) MaterialTheme.colorScheme.onSurface else Color.Unspecified
                )
            }

            else -> {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        Column {
            Text(text = headingText, style = MaterialTheme.typography.titleMedium)
            Text(
                text = labelText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SpacerHeight(height: androidx.compose.ui.unit.Dp) {
    Spacer(modifier = Modifier.height(height))
}

@Composable
private fun SpacerWidth(width: Int) {
    Spacer(modifier = Modifier.width(width.dp))
}
