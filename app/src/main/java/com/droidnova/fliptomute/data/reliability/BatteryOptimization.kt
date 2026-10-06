package com.droidnova.fliptomute.data.reliability

import android.content.Context
import android.os.PowerManager
import androidx.annotation.ArrayRes
import com.droidnova.fliptomute.R
import java.util.Locale

/**
 * Whether Android may stop Flip to Mute in the background to save battery (audit R4). A
 * `fun interface`, as Secret Calculator does for system checks, so tests can pass a lambda.
 */
fun interface BatteryOptimizationStatus {
    /** True when the app is exempt from battery optimisation; null when the system cannot tell. */
    fun isIgnoringBatteryOptimizations(): Boolean?
}

class AndroidBatteryOptimizationStatus(context: Context) : BatteryOptimizationStatus {
    private val applicationContext = context.applicationContext

    override fun isIgnoringBatteryOptimizations(): Boolean? = try {
        applicationContext.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(applicationContext.packageName)
    } catch (_: RuntimeException) {
        null
    }
}

/**
 * Phone makers whose own battery managers stop background apps, each with short steps for
 * "Keep it running" (v2.0 design spec section 4.7). Steps live in string arrays so they can be
 * translated.
 */
enum class PhoneBrand(@param:ArrayRes val stepsRes: Int) {
    XIAOMI(R.array.keep_running_steps_xiaomi),
    SAMSUNG(R.array.keep_running_steps_samsung),
    OPPO_FAMILY(R.array.keep_running_steps_oppo),
    VIVO(R.array.keep_running_steps_vivo),
    HUAWEI_FAMILY(R.array.keep_running_steps_huawei),
    OTHER(R.array.keep_running_steps_other),
    ;

    companion object {
        /** Maps `Build.MANUFACTURER` (or `Build.BRAND`) to a brand family. */
        fun from(manufacturer: String?): PhoneBrand = when (manufacturer?.trim()?.lowercase(Locale.ROOT)) {
            "xiaomi", "redmi", "poco" -> XIAOMI
            "samsung" -> SAMSUNG
            "oppo", "realme", "oneplus" -> OPPO_FAMILY
            "vivo", "iqoo" -> VIVO
            "huawei", "honor" -> HUAWEI_FAMILY
            else -> OTHER
        }
    }
}
