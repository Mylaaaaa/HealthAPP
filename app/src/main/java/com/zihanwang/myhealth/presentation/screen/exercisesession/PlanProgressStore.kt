// app/src/main/java/com/example/myhealth/presentation/screen/exercisesession/PlanProgressStore.kt
package com.zihanwang.myhealth.presentation.screen.exercisesession

import android.content.Context
import java.time.LocalDate

/**
 * Tiny persistence for plan-day completion.
 * Key = yyyy-MM-dd|<dayTitle>, value = "1"
 */
class PlanProgressStore(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences("plan_progress", Context.MODE_PRIVATE)
    }

    private fun keyFor(date: LocalDate, dayTitle: String) =
        "${date}|${dayTitle}"

    /** Mark a plan day as done for 'date'. */
    fun markDone(date: LocalDate, dayTitle: String) {
        prefs.edit().putString(keyFor(date, dayTitle), "1").apply()
    }

    /** Undo (if you want to allow reset). */
    fun clear(date: LocalDate, dayTitle: String) {
        prefs.edit().remove(keyFor(date, dayTitle)).apply()
    }

    /** Whether plan day is done on 'date'. */
    fun isDone(date: LocalDate, dayTitle: String): Boolean =
        prefs.getString(keyFor(date, dayTitle), null) == "1"
}
