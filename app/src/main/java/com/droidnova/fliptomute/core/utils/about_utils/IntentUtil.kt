package com.droidnova.fliptomute.core.utils.about_utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.droidnova.fliptomute.R
import java.util.Locale

object IntentUtil {

    fun openRateUs(context: Context) {
        openPlayStore(context, context.packageName)
    }

    fun shareApp(context: Context) {
        val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
        val storeUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, appName)
            putExtra(
                Intent.EXTRA_TEXT,
                context.getString(R.string.share_app_message, storeUrl)
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_app)))
    }

    fun openInstagram(context: Context) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.INSTAGRAM_COMMUNITY_URL)))
    }

    fun openWhatsApp(context: Context) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.WHATSAPP_COMMUNITY_GROUP_URL)))
    }

    fun openDeveloperPlayConsole(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.PLAY_CONSOLE_URL)).apply {
            setPackage("com.android.vending")
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.PLAY_CONSOLE_URL)))
        }
    }

    fun sendSupportMail(context: Context, isBug: Boolean) {
        val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
        val versionName = PackageManagerExt.getPackageInfo(context.packageManager, context.packageName)
            ?.versionName.orEmpty().ifBlank { "Unknown" }

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

        if (PackageManagerExt.isPackageInstalled("com.google.android.gm", context)) {
            emailIntent.setPackage("com.google.android.gm")
        }

        runCatching {
            context.startActivity(emailIntent)
        }.onFailure {
            Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    fun fetchAppVersion(context: Context): String {
        return PackageManagerExt.getPackageInfo(context.packageManager, context.packageName)
            ?.versionName.orEmpty().ifBlank { "Unknown" }
    }

    fun openPlayStore(context: Context, packageName: String) {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))

        runCatching {
            context.startActivity(marketIntent)
        }.onFailure {
            context.startActivity(webIntent)
        }
    }
}
