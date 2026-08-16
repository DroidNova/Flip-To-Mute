package com.droidnova.fliptomute.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.droidnova.fliptomute.R

object AboutIntentUtil {
    fun openPlayStore(context: Context) {
        val market = Uri.parse("market://details?id=${context.packageName}")
        val website = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
        launch(context, Intent(Intent.ACTION_VIEW, market)) {
            launch(context, Intent(Intent.ACTION_VIEW, website))
        }
    }

    fun openUrl(context: Context, url: String) {
        launch(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun searchPlayStore(context: Context, query: String) {
        openUrl(context, "https://play.google.com/store/search?q=${Uri.encode(query)}&c=apps")
    }

    fun openDeveloperApps(context: Context) {
        openUrl(context, "https://play.google.com/store/search?q=pub:DroidNova&c=apps")
    }

    fun shareApp(context: Context) {
        val storeUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_app_message, storeUrl))
        }
        launch(context, Intent.createChooser(intent, context.getString(R.string.share_app)))
    }

    fun sendBugReport(context: Context) {
        val subject = context.getString(R.string.bug_report_subject, appVersion(context))
        val body = context.getString(
            R.string.bug_report_body,
            Build.MANUFACTURER,
            Build.MODEL,
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT,
        )
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${context.getString(R.string.support_email)}")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        launch(context, intent)
    }

    fun appVersion(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty().ifBlank { context.getString(R.string.unknown_version) }

    private fun launch(context: Context, intent: Intent, fallback: (() -> Unit)? = null) {
        runCatching { context.startActivity(intent) }.onFailure {
            if (fallback != null) fallback() else Toast.makeText(
                context,
                R.string.no_compatible_app,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}
