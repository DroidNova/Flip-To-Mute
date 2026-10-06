package com.droidnova.fliptomute.data.stats

/** Where the user stands between two milestones. */
data class MilestoneProgress(
    /** Every milestone, each marked reached or not, in order. */
    val steps: List<Step>,
    /** The next one to reach, or null once the last is passed. */
    val next: Int?,
    /** Flips still needed for [next]. */
    val remaining: Int,
    /** 0 to 1 between the last reached milestone and [next]. */
    val fraction: Float,
) {
    data class Step(val count: Int, val reached: Boolean)
}

/**
 * Calls silenced, counted in round numbers (future features F35). Something to reach, and a reason
 * to look at the activity screen. Pure, so every boundary is tested.
 */
object Milestones {
    val STEPS = listOf(1, 10, 25, 50, 100, 250, 500, 1000)

    /** The milestone this exact total lands on, or null. */
    fun reachedAt(total: Int): Int? = total.takeIf { it in STEPS }

    fun progress(total: Int): MilestoneProgress {
        val next = STEPS.firstOrNull { it > total }
        val previous = STEPS.lastOrNull { it <= total } ?: 0
        return MilestoneProgress(
            steps = STEPS.map { MilestoneProgress.Step(it, it <= total) },
            next = next,
            remaining = if (next == null) 0 else next - total,
            fraction = if (next == null) 1f else (total - previous).toFloat() / (next - previous),
        )
    }
}
