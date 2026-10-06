package com.droidnova.fliptomute.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.droidnova.fliptomute.R
import java.io.File

/**
 * "50 calls silenced with a flip of my phone", as a square picture to share (future features F38).
 * Only the count is on it: the app has nothing else to show, and nothing about who called.
 */
object ShareCard {
    private const val SIZE = 1080
    private const val FOLDER = "share"
    private const val FILE = "flip_to_mute.png"

    /** Matches the provider in the manifest and res/xml/share_paths.xml. */
    fun authority(context: Context): String = "${context.packageName}.share"

    fun render(context: Context, total: Int): Bitmap {
        val bitmap = createBitmap(SIZE, SIZE)
        val canvas = Canvas(bitmap)
        val centre = SIZE / 2f
        // The brand colours of the default theme, top to bottom
        val background = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, SIZE.toFloat(), 0xFF575992.toInt(), 0xFF2B2A60.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), background)

        fun text(size: Float, bold: Boolean, alpha: Int = 255) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.alpha = alpha
            textSize = size
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
        // A very large number is drawn smaller so it always fits the width
        val number = total.toString()
        val numberPaint = text(340f, bold = true)
        val widest = SIZE * 0.8f
        if (numberPaint.measureText(number) > widest) numberPaint.textSize *= widest / numberPaint.measureText(number)
        canvas.drawText(number, centre, 500f, numberPaint)
        canvas.drawText(context.resources.getQuantityString(R.plurals.share_card_calls, total), centre, 610f, text(64f, bold = true))
        canvas.drawText(context.getString(R.string.share_card_how), centre, 690f, text(46f, bold = false, alpha = 210))
        canvas.drawText(context.getString(R.string.app_name), centre, 960f, text(54f, bold = true))
        return bitmap
    }

    /** Writes the picture where FileProvider may hand it out. Null when the phone could not save it. */
    fun write(context: Context, total: Int): Uri? = try {
        val folder = File(context.cacheDir, FOLDER).apply { mkdirs() }
        val file = File(folder, FILE)
        file.outputStream().use { render(context, total).compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, authority(context), file)
    } catch (error: Exception) {
        MonitoringLog.failure(context, "Share card could not be saved", error)
        null
    }
}
