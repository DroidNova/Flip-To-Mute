package com.droidnova.fliptomute.ui.screens.about

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.ui.components.AppIcon
import com.droidnova.fliptomute.utils.about_utils.AppConstants
import com.droidnova.fliptomute.utils.about_utils.IntentUtil
import com.droidnova.fliptomute.utils.about_utils.OtherAppItem
import com.droidnova.fliptomute.utils.about_utils.randomOtherApps

/** Everything About can ask for, as Secret Calculator's AboutActions. */
interface AboutActions {
    fun back()
    fun rateUs()
    fun shareApp()
    fun reportProblem()
    fun openPrivacyPolicy()
    fun openInstagram()
    fun openWhatsApp()
    fun openOtherApp(packageName: String)
    fun openMoreApps()
}

@Composable
fun AboutRoute(onBack: () -> Unit, onReportProblem: () -> Unit, onRateUsTapped: () -> Unit) {
    val context = LocalContext.current
    val apps = remember { randomOtherApps(context.packageName) }
    AboutScreen(
        versionName = IntentUtil.fetchAppVersionWithCode(context),
        featuredApps = apps,
        actions = object : AboutActions {
            override fun back() = onBack()
            // Unchanged from 1.x: opens the Play Store listing (decision D1, UPDATE_PLAN section 8)
            override fun rateUs() {
                onRateUsTapped()
                IntentUtil.openRateUs(context)
            }
            override fun shareApp() = IntentUtil.shareApp(context)
            override fun reportProblem() = onReportProblem()
            override fun openPrivacyPolicy() = IntentUtil.openUrl(context, AppConstants.PRIVACY_POLICY_URL)
            override fun openInstagram() = IntentUtil.openInstagram(context)
            override fun openWhatsApp() = IntentUtil.openWhatsApp(context)
            override fun openOtherApp(packageName: String) = IntentUtil.openPlayStore(context, packageName)
            override fun openMoreApps() = IntentUtil.openDeveloperPlayConsole(context)
        },
    )
}

/**
 * The DroidNova About page, laid out as in All File Reader (and NotiSave), so every app of ours looks
 * the same here: the app header, the support links as plain rows with a line of explanation each,
 * the version, the privacy policy, then other DroidNova apps. Colours come from the app's own theme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(versionName: String, featuredApps: List<OtherAppItem>, actions: AboutActions) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title), fontWeight = FontWeight.W900, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = actions::back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_description))
                    }
                },
                // MainActivity pads for the system bars once
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Centred on tablets and in landscape (design spec 8)
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppHeader()
                Spacer(Modifier.height(8.dp))
                AboutItem(stringResource(R.string.rate_us), stringResource(R.string.rate_us_desc), imageVector = Icons.Outlined.Star, onClick = actions::rateUs)
                AboutItem(stringResource(R.string.share_us), stringResource(R.string.share_us_desc), imageVector = Icons.Outlined.Share, onClick = actions::shareApp)
                AboutItem(stringResource(R.string.report_bugs), stringResource(R.string.report_bugs_desc), imageVector = Icons.Outlined.BugReport, onClick = actions::reportProblem)
                AboutItem(
                    stringResource(R.string.follow_instagram), stringResource(R.string.follow_instagram_description),
                    iconRes = R.drawable.ic_instagram, keepIconColors = true, onClick = actions::openInstagram,
                )
                AboutItem(
                    stringResource(R.string.join_whatsapp), stringResource(R.string.join_whatsapp_description),
                    iconRes = R.drawable.ic_whatsapp, keepIconColors = true, onClick = actions::openWhatsApp,
                )
                AboutItem(stringResource(R.string.app_version), versionName, imageVector = Icons.Outlined.Info, onClick = null)
                // Shown once the privacy policy address is set (decision D7, M6-06)
                if (AppConstants.PRIVACY_POLICY_URL.isNotBlank()) {
                    AboutItem(
                        stringResource(R.string.privacy_policy), stringResource(R.string.privacy_policy_desc),
                        imageVector = Icons.Outlined.Security, onClick = actions::openPrivacyPolicy,
                    )
                }
                Spacer(Modifier.height(4.dp))
                OtherAppsSection(featuredApps, onOpenApp = actions::openOtherApp, onMoreApps = actions::openMoreApps)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/**
 * "Check Out Other Apps": two DroidNova apps and the developer page. Used at the bottom of Settings
 * and of the About page, as in All File Reader.
 */
@Composable
fun OtherAppsSection(
    apps: List<OtherAppItem>,
    onOpenApp: (packageName: String) -> Unit,
    onMoreApps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.checkout_other_apps),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        apps.forEach { app ->
            AppCard(stringResource(app.titleRes), stringResource(app.descriptionRes), app.iconRes) { onOpenApp(app.packageName) }
        }
        AppCard(stringResource(R.string.more_apps_play_store), null, R.drawable.ic_play_store, onMoreApps)
    }
}

@Composable
private fun AppCard(title: String, description: String?, @DrawableRes iconRes: Int, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!description.isNullOrEmpty()) {
                    Text(
                        description,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** The app's icon, name and one line, centred. */
@Composable
private fun AppHeader() {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AppIcon(80.dp, contentDescription = stringResource(R.string.app_icon_description))
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.about_app_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** One row: an icon, a title and a line under it. Brand logos keep their own colours. */
@Composable
private fun AboutItem(
    title: String,
    description: String,
    imageVector: ImageVector? = null,
    @DrawableRes iconRes: Int? = null,
    keepIconColors: Boolean = false,
    onClick: (() -> Unit)?,
) {
    val tint = if (keepIconColors) Color.Unspecified else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            imageVector != null -> Icon(imageVector, contentDescription = null, modifier = Modifier.padding(12.dp), tint = tint)
            iconRes != null -> Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.padding(12.dp).size(24.dp), tint = tint)
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal object PreviewAboutActions : AboutActions {
    override fun back() = Unit
    override fun rateUs() = Unit
    override fun shareApp() = Unit
    override fun reportProblem() = Unit
    override fun openPrivacyPolicy() = Unit
    override fun openInstagram() = Unit
    override fun openWhatsApp() = Unit
    override fun openOtherApp(packageName: String) = Unit
    override fun openMoreApps() = Unit
}
