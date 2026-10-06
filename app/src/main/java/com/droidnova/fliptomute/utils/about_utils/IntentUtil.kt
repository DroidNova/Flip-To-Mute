package com.droidnova.fliptomute.utils.about_utils

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.utils.ShareCard
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

    /** The share card picture with a line of text; text alone when the picture could not be saved. */
    fun shareFlips(context: Context, total: Int) {
        val storeUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
        val text = context.resources.getQuantityString(R.plurals.share_flips_message, total, total, storeUrl)
        val picture = ShareCard.write(context, total)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            if (picture != null) {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, picture)
                // The chooser shows a preview only when the address travels as clip data too
                clipData = ClipData.newRawUri(null, picture)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
        }
        startFirstAvailable(context, Intent.createChooser(shareIntent, context.getString(R.string.share_flips)))
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

    /**
     * The support email. [summary] is the automatic part (M6-04): one line per "Check my setup"
     * step, so a report arrives with what support needs to answer it.
     */
    fun sendSupportMail(context: Context, isBug: Boolean, summary: String? = null) {
        val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
        val versionName = fetchAppVersion(context)
        val deviceBrand = Build.BRAND.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
        val subject = context.getString(
            if (isBug) R.string.support_subject_bug else R.string.support_subject_feedback,
            appName,
            versionName,
        )
        val body = buildString {
            appendLine(context.getString(R.string.support_device, deviceBrand, Build.MODEL))
            appendLine(context.getString(R.string.support_android, Build.VERSION.RELEASE, Build.VERSION.SDK_INT))
            if (!summary.isNullOrBlank()) {
                appendLine()
                appendLine(context.getString(R.string.support_check_results))
                appendLine(summary)
            }
            appendLine()
            append(context.getString(if (isBug) R.string.support_describe_issue else R.string.support_your_feedback))
        }

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

    /** A web page, such as the privacy policy. A blank URL does nothing. */
    fun openUrl(context: Context, url: String) {
        if (url.isBlank()) return
        startFirstAvailable(context, viewIntent(url))
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
