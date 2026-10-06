package com.droidnova.fliptomute.utils.about_utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import com.droidnova.fliptomute.R
import java.util.Locale

/**
 * Links out of the app. Same API as Secret Calculator's IntentUtil, but every launch goes through
 * [startFirstAvailable] (audit R7, architecture X3): a phone with no browser, no email app or a
 * disabled Play Store gets a short message instead of a crash.
 */
object IntentUtil {

    /** "Rate us": the Play Store app, else the web listing. Same behaviour as 1.x. */
    fun openRateUs(context: Context) {
        openPlayStore(context, context.packageName)
    }

    fun shareApp(context: Context) {
        val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
        val storeUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, appName)
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_app_message, storeUrl))
        }
        startFirstAvailable(context, Intent.createChooser(shareIntent, context.getString(R.string.share_app)))
    }

    fun openInstagram(context: Context) {
        startFirstAvailable(context, viewIntent(AppConstants.INSTAGRAM_COMMUNITY_URL))
    }

    fun openWhatsApp(context: Context) {
        startFirstAvailable(context, viewIntent(AppConstants.WHATSAPP_COMMUNITY_GROUP_URL))
    }

    fun openDeveloperPlayConsole(context: Context) {
        startFirstAvailable(
            context,
            viewIntent(AppConstants.PLAY_CONSOLE_URL).setPackage(PLAY_STORE_PACKAGE),
            viewIntent(AppConstants.PLAY_CONSOLE_URL),
        )
    }

    fun sendSupportMail(context: Context, isBug: Boolean) {
        val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
        val versionName = fetchAppVersion(context)
        val deviceBrand = Build.BRAND.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
        val subject = if (isBug) {
            "Bug report - $appName v$versionName"
        } else {
            "Feedback - $appName v$versionName"
        }
        val body = """
            Device: $deviceBrand ${Build.MODEL}
            Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})

            ${if (isBug) "Describe the issue:" else "Your feedback:"}
        """.trimIndent()

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(AppConstants.SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        // Gmail first when installed, as before; any other email app otherwise
        val gmailIntent = if (PackageManagerExt.isPackageInstalled(GMAIL_PACKAGE, context)) {
            Intent(emailIntent).setPackage(GMAIL_PACKAGE)
        } else {
            null
        }
        startFirstAvailable(context, *listOfNotNull(gmailIntent, emailIntent).toTypedArray(), failureMessage = R.string.no_email_app)
    }

    fun fetchAppVersion(context: Context): String {
        return PackageManagerExt.getPackageInfo(context.packageManager, context.packageName)
            ?.versionName.orEmpty().ifBlank { context.getString(R.string.unknown_version) }
    }

    fun openPlayStore(context: Context, packageName: String) {
        startFirstAvailable(
            context,
            viewIntent("market://details?id=$packageName"),
            viewIntent("https://play.google.com/store/apps/details?id=$packageName"),
        )
    }

    /**
     * Starts the first intent some app can handle. Returns false, after a short message, when none
     * can. Never throws for a missing or refusing app.
     */
    fun startFirstAvailable(
        context: Context,
        vararg intents: Intent,
        @StringRes failureMessage: Int = R.string.no_compatible_app,
    ): Boolean {
        for (intent in intents) {
            try {
                context.startActivity(intent)
                return true
            } catch (_: ActivityNotFoundException) {
                // Try the next one
            } catch (_: SecurityException) {
                // The target refused; try the next one
            }
        }
        Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
        return false
    }

    private fun viewIntent(url: String) = Intent(Intent.ACTION_VIEW, Uri.parse(url))

    private const val PLAY_STORE_PACKAGE = "com.android.vending"
    private const val GMAIL_PACKAGE = "com.google.android.gm"
}
