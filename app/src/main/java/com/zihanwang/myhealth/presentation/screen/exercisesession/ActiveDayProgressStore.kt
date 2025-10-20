// app/src/main/java/com/example/myhealth/presentation/screen/exercisesession/ActiveDayProgressStore.kt
package com.zihanwang.myhealth.presentation.screen.exercisesession

import android.content.Context
import java.time.LocalDate

/**
 * Persists in-progress per-item state for a day plan.
 * - Key: yyyy-MM-dd|<dayTitle>
 * - Value: "flags|index", where flags is a string of '1'/'0' (e.g. 10 means [true,false])
 *   and index is current queue index (0-based).
 *
 * Use cases:
 * - Save after each Done/Skip.
 * - Load on Guided entry to restore partial state.
 * - Clear on Summary save (finished) or when user chooses Redo.
 */
class ActiveDayProgressStore(private val context: Context) {
    private val sp by lazy { context.getSharedPreferences("plan_partial_progress", Context.MODE_PRIVATE) }
    private fun key(date: LocalDate, title: String) = "$date|$title"

    fun save(date: LocalDate, title: String, doneFlags: List<Boolean>, currentIndex: Int) {
        val flags = buildString(doneFlags.size) { doneFlags.forEach { append(if (it) '1' else '0') } }
        sp.edit().putString(key(date, title), "$flags|$currentIndex").apply()
    }

    /** Returns Pair<flags, index> or null if nothing saved. */
    fun load(date: LocalDate, title: String): Pair<List<Boolean>, Int>? {
        val raw = sp.getString(key(date, title), null) ?: return null
        val parts = raw.split('|')
        if (parts.size != 2) return null
        val flags = parts[0].map { it == '1' }
        val idx = parts[1].toIntOrNull() ?: 0
        return flags to idx
    }

    fun clear(date: LocalDate, title: String) {
        sp.edit().remove(key(date, title)).apply()
    }
}