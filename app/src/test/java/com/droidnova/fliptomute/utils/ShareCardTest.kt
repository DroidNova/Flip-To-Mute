package com.droidnova.fliptomute.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/** The share card (future features F38). Also saved under app/build/snapshots for a visual check. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShareCardTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun rendersASquarePictureWithSomethingOnIt() {
        val bitmap = ShareCard.render(context, 50)
        assertEquals(1080, bitmap.width)
        assertEquals(1080, bitmap.height)
        // The background runs from one brand colour to another, so it was really drawn
        assertNotEquals(bitmap.getPixel(10, 10), bitmap.getPixel(10, 1070))
        save(bitmap, "share_card")
        save(ShareCard.render(context, 1), "share_card_one")
        save(ShareCard.render(context, 123_456), "share_card_large")
    }

    @Test fun writesThePictureWhereItCanBeShared() {
        val uri = ShareCard.write(context, 12)
        assertTrue(File(context.cacheDir, "share/flip_to_mute.png").length() > 0)
        // FileProvider cannot map Robolectric's temporary folders on Windows ("Failed to find configured
        // root"), so the address is checked where the tests run on Linux or macOS, as in CI
        assumeFalse(System.getProperty("os.name").orEmpty().startsWith("Windows"))
        assertNotNull(uri)
        assertEquals("content", uri!!.scheme)
        assertEquals(ShareCard.authority(context), uri.authority)
    }

    private fun save(bitmap: Bitmap, name: String) {
        File(File("build/snapshots").apply { mkdirs() }, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
