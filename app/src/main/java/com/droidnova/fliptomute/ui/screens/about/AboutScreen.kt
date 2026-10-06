package com.droidnova.fliptomute.ui.screens.about

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.ui.components.ActionTile
import com.droidnova.fliptomute.ui.components.NovaCard
import com.droidnova.fliptomute.ui.components.NovaTopBar
import com.droidnova.fliptomute.ui.components.SectionLabel
import com.droidnova.fliptomute.ui.components.SettingsGroup
import com.droidnova.fliptomute.ui.components.SettingsRow
import com.droidnova.fliptomute.ui.components.appearIn
import com.droidnova.fliptomute.ui.components.novaCardColor
import com.droidnova.fliptomute.ui.components.novaTileColor
import com.droidnova.fliptomute.utils.about_utils.AppConstants
import com.droidnova.fliptomute.utils.about_utils.IntentUtil
import com.droidnova.fliptomute.utils.about_utils.OtherAppItem
import com.droidnova.fliptomute.utils.about_utils.getFeaturedOtherApps

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
    val featured = remember { getFeaturedOtherApps() }
    AboutScreen(
        versionName = IntentUtil.fetchAppVersion(context),
        featuredApps = featured,
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

/** Same layout as Secret Calculator's About screen (design spec 4.9). */
@Composable
fun AboutScreen(versionName: String, featuredApps: List<OtherAppItem>, actions: AboutActions) {
    Scaffold(
        topBar = { NovaTopBar(title = stringResource(R.string.about_title), onBack = actions::back) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppHeader(versionName)
                Row(Modifier.padding(top = 8.dp)) {
                    ActionTile(Icons.Outlined.StarRate, stringResource(R.string.rate_us), actions::rateUs, Modifier.weight(1f))
                    ActionTile(Icons.Outlined.Share, stringResource(R.string.share_us), actions::shareApp, Modifier.weight(1f))
                    ActionTile(Icons.Outlined.BugReport, stringResource(R.string.report_a_problem), actions::reportProblem, Modifier.weight(1f))
                }
                if (AppConstants.PRIVACY_POLICY_URL.isNotBlank()) {
                    SettingsGroup(stringResource(R.string.about_title)) {
                        SettingsRow(Icons.Filled.Policy, stringResource(R.string.privacy_policy), onClick = actions::openPrivacyPolicy)
                    }
                }
                SettingsGroup(stringResource(R.string.about_section_community)) {
                    // Brand icons keep their own colours
                    BrandRow(R.drawable.ic_instagram, stringResource(R.string.follow_instagram), stringResource(R.string.follow_instagram_description), actions::openInstagram)
                    BrandRow(R.drawable.ic_whatsapp, stringResource(R.string.join_whatsapp), stringResource(R.string.join_whatsapp_description), actions::openWhatsApp)
                }
                SectionLabel(stringResource(R.string.about_more_from), Modifier.padding(start = 6.dp, top = 12.dp, bottom = 2.dp))
                featuredApps.forEachIndexed { index, app ->
                    AppCard(
                        iconRes = app.iconRes,
                        title = stringResource(app.titleRes),
                        description = stringResource(app.descriptionRes),
                        onClick = { actions.openOtherApp(app.packageName) },
                        modifier = Modifier.appearIn(index).padding(bottom = 8.dp),
                    )
                }
                AppCard(
                    iconRes = R.drawable.ic_play_store,
                    title = stringResource(R.string.more_apps_play_store),
                    description = null,
                    onClick = actions::openMoreApps,
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/** The app's icon, name, version and one line, centred, as in Secret Calculator. */
@Composable
private fun AppHeader(versionName: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_flip_to_mute),
            contentDescription = stringResource(R.string.app_icon_description),
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)),
        )
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            stringResource(R.string.version_label, versionName),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(novaCardColor()).padding(horizontal = 10.dp, vertical = 3.dp),
        )
        Text(
            stringResource(R.string.about_app_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )
    }
}

/** A row led by a brand logo (not tinted), like the settings rows. */
@Composable
private fun BrandRow(@DrawableRes iconRes: Int, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).background(novaTileColor(), CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(iconRes), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AppCard(@DrawableRes iconRes: Int, title: String, description: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    NovaCard(modifier = modifier, contentPadding = PaddingValues(10.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)),
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
