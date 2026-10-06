package com.droidnova.fliptomute.data.stats

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Visible proof that Flip to Mute works (audit U11). Counts and two times only, never call details. */
data class FlipStats(
    val thisMonth: Int = 0,
    val total: Int = 0,
    val lastFlipAt: Long? = null,
    /** When Flip to Mute was first seen running; discovery cards wait a few days after it. */
    val firstOnAt: Long? = null,
)

/**
 * Flip counts on this device (M5-05), a small store in Secret Calculator's style (architecture X1):
 * an injected singleton with its own preferences file. The service records; Home reads [stats].
 */
@Singleton
class FlipStatsStore(
    context: Context,
    private val now: () -> Long,
) {
    @Inject constructor(@ApplicationContext context: Context) : this(context, System::currentTimeMillis)

    private val appContext = context.applicationContext
    private val prefs get() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val mutableStats = MutableStateFlow(read())
    val stats: StateFlow<FlipStats> = mutableStats.asStateFlow()

    /** A ringing call was silenced or vibrated by a flip. */
    @Synchronized
    fun recordFlip() {
        val time = now()
        val month = monthKey(time)
        val sameMonth = prefs.getInt(KEY_MONTH, 0) == month
        prefs.edit {
            putInt(KEY_MONTH, month)
            putInt(KEY_MONTH_COUNT, if (sameMonth) prefs.getInt(KEY_MONTH_COUNT, 0) + 1 else 1)
            putInt(KEY_TOTAL, prefs.getInt(KEY_TOTAL, 0) + 1)
            putLong(KEY_LAST_FLIP, time)
        }
        mutableStats.value = read()
    }

    /** Flip to Mute is running; remembers the first time only. */
    @Synchronized
    fun recordOn() {
        if (prefs.getLong(KEY_FIRST_ON, 0L) > 0L) return
        prefs.edit { putLong(KEY_FIRST_ON, now()) }
        mutableStats.value = read()
    }

    /** Re-reads, so a new month starts at zero even if nothing was recorded since. */
    fun refresh() {
        mutableStats.value = read()
    }

    private fun read(): FlipStats {
        val sameMonth = prefs.getInt(KEY_MONTH, 0) == monthKey(now())
        return FlipStats(
            thisMonth = if (sameMonth) prefs.getInt(KEY_MONTH_COUNT, 0) else 0,
            total = prefs.getInt(KEY_TOTAL, 0),
            lastFlipAt = prefs.getLong(KEY_LAST_FLIP, 0L).takeIf { it > 0L },
            firstOnAt = prefs.getLong(KEY_FIRST_ON, 0L).takeIf { it > 0L },
        )
    }

    private fun monthKey(epochMs: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = epochMs }
        return calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH)
    }

    private companion object {
        const val PREFS = "flip_stats"
        const val KEY_MONTH = "month"
        const val KEY_MONTH_COUNT = "month_count"
        const val KEY_TOTAL = "total"
        const val KEY_LAST_FLIP = "last_flip_at"
        const val KEY_FIRST_ON = "first_on_at"
    }
}
