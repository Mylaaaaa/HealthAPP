// file: app/src/main/java/com/example/myhealth/presentation/screen/exercisesession/planaccess/PlanTasksStore.kt
package com.example.myhealth.presentation.screen.exercisesession.planaccess

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Lightweight day-plan store backed by SharedPreferences.
 *
 * - Key:  plan_tasks_<yyyy-MM-dd>
 *         Value is newline-joined rows; each row is a task serialized as:
 *         "taskId§title§type§target§completed"
 *         type      : optional tag (e.g., "synthetic")
 *         target    : minutes (nullable)
 *         completed : "1" or "0" (if missing -> treated as false for backward compatibility)
 *
 * - Key:  plan_title_<yyyy-MM-dd>
 *         Optional display title for that day.
 *
 * This class is intentionally simple so you can swap it with Room/remote later without
 * touching the UI code.
 */
class PlanTasksStore(private val context: Context) {

    /** Single planned task model stored in the SP. */
    data class PlanTask(
        val taskId: String = UUID.randomUUID().toString(),
        val title: String,
        val type: String? = null,   // arbitrary tag, e.g., "synthetic"
        val target: Int? = null,    // minutes
        val completed: Boolean = false
    )

    private val sp: SharedPreferences
        get() = context.getSharedPreferences("plan_tasks_store", Context.MODE_PRIVATE)

    private val df = DateTimeFormatter.ISO_LOCAL_DATE

    private fun keyTasks(date: LocalDate) = "plan_tasks_${df.format(date)}"
    private fun keyTitle(date: LocalDate) = "plan_title_${df.format(date)}"

    /* ------------------------------------------------------------------------------------------
     * Core CRUD
     * ------------------------------------------------------------------------------------------ */

    /** Overwrite tasks for a date and optionally update its title. */
    fun setTasks(date: LocalDate, dayTitle: String?, tasks: List<PlanTask>) {
        val serialized = tasks.joinToString("\n") { t ->
            listOf(
                t.taskId,
                t.title,
                t.type ?: "",
                t.target?.toString() ?: "",
                if (t.completed) "1" else "0"
            ).joinToString("§")
        }
        sp.edit {
            putString(keyTasks(date), serialized)
            if (dayTitle != null) putString(keyTitle(date), dayTitle)
        }
    }

    /**
     * Overwrite tasks for a date BUT keep existing "synthetic" tasks (quick-add placeholders).
     * Use this when the Plan page regenerates a day's template so quick-added tasks survive.
     */
    fun setTasksKeepingSynthetic(
        date: LocalDate,
        dayTitle: String?,
        newTasks: List<PlanTask>
    ) {
        // keep existing synthetic items from current storage
        val synthetic = getTasks(date).filter { it.type == "synthetic" }
        // merge template + synthetic (template comes first so it stays visually before)
        val merged = newTasks + synthetic
        setTasks(date, dayTitle, merged)
    }

    /** Read tasks for a date. Returns an empty list if nothing is stored. */
    fun getTasks(date: LocalDate): List<PlanTask> {
        val raw = sp.getString(keyTasks(date), null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()

        return raw.lineSequence()
            .filter { it.isNotBlank() }
            .map { line ->
                val p = line.split("§")
                PlanTask(
                    taskId = p.getOrNull(0) ?: UUID.randomUUID().toString(),
                    title   = p.getOrNull(1) ?: "Planned task",
                    type    = p.getOrNull(2)?.ifBlank { null },
                    target  = p.getOrNull(3)?.toIntOrNull(),
                    // Back-compat: older rows have no index 4 -> treat as not completed
                    completed = p.getOrNull(4)?.let { it == "1" } ?: false
                )
            }.toList()
    }

    /** Remove both tasks and title for a date. */
    fun clear(date: LocalDate) {
        sp.edit {
            remove(keyTasks(date))
            remove(keyTitle(date))
        }
    }

    /* ------------------------------------------------------------------------------------------
     * Day title only
     * ------------------------------------------------------------------------------------------ */

    fun getDayTitle(date: LocalDate): String? = sp.getString(keyTitle(date), null)

    /** Replace only the day's title without touching tasks. */
    fun setDayTitle(date: LocalDate, title: String?) {
        sp.edit {
            if (title == null) remove(keyTitle(date)) else putString(keyTitle(date), title)
        }
    }

    /* ------------------------------------------------------------------------------------------
     * Convenience APIs for Plan & Workout dashboards
     * ------------------------------------------------------------------------------------------ */

    /** Append a new task to the date and return it. */
    fun addTask(
        date: LocalDate,
        title: String,
        type: String? = null,
        target: Int? = null,
        dayTitle: String? = null
    ): PlanTask {
        val current = getTasks(date)
        val newTask = PlanTask(title = title, type = type, target = target, completed = false)
        val effectiveTitle = dayTitle ?: getDayTitle(date)
        setTasks(date, effectiveTitle, current + newTask)
        return newTask
    }

    /** Remove one task by id. No-op if not found. */
    fun removeTask(date: LocalDate, taskId: String) {
        val filtered = getTasks(date).filterNot { it.taskId == taskId }
        setTasks(date, getDayTitle(date), filtered)
    }

    /** Mark a task completed / not completed. */
    fun setCompleted(date: LocalDate, taskId: String, completed: Boolean) {
        val updated = getTasks(date).map {
            if (it.taskId == taskId) it.copy(completed = completed) else it
        }
        setTasks(date, getDayTitle(date), updated)
    }

    /** Handy counters used by dashboards. */
    fun count(date: LocalDate): Int = getTasks(date).size
    fun completedCount(date: LocalDate): Int = getTasks(date).count { it.completed }

    /**
     * Add a "synthetic" placeholder task for Quick Add from the Workout page.
     * This increases the planned count instantly while keeping the weekly template clean.
     */
    fun addSyntheticTask(
        date: LocalDate,
        title: String,
        target: Int? = null
    ): PlanTask = addTask(date, title = title, type = "synthetic", target = target)

    /**
     * Remove one "synthetic" task if present (when deleting a quick-added session).
     * @return true if one synthetic task was found and removed
     */
    fun consumeOneSyntheticTask(date: LocalDate): Boolean {
        val tasks = getTasks(date)
        val idx = tasks.indexOfFirst { it.type == "synthetic" }
        if (idx == -1) return false
        val newList = tasks.toMutableList().also { it.removeAt(idx) }
        setTasks(date, getDayTitle(date), newList)
        return true
    }

    /* ------------------------------------------------------------------------------------------
     * Change listeners (so Workout can auto-refresh when Plan writes today)
     * ------------------------------------------------------------------------------------------ */

    fun addOnChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) {
        sp.registerOnSharedPreferenceChangeListener(l)
    }

    fun removeOnChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) {
        sp.unregisterOnSharedPreferenceChangeListener(l)
    }
}
