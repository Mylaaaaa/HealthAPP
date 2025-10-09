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
 * Keys per date:
 *  - plan_tasks_<yyyy-MM-dd> : newline-joined rows
 *  - plan_title_<yyyy-MM-dd> : optional day title
 *
 * Row format (fields are separated by '§'):
 *   taskId § title § type § target § completed
 *   - type: arbitrary tag (e.g. "synthetic"); may be empty
 *   - target: minutes (Int?); serialized as string, may be empty
 *   - completed: "1" or "0"; missing -> treated as false (backward compatibility)
 */
class PlanTasksStore(private val context: Context) {

    /** Single planned task saved in the store. */
    data class PlanTask(
        val taskId: String = UUID.randomUUID().toString(),
        val title: String,
        val type: String? = null,  // e.g. "synthetic"
        val target: Int? = null,   // minutes
        val completed: Boolean = false
    )

    private val sp: SharedPreferences
        get() = context.getSharedPreferences("plan_tasks_store", Context.MODE_PRIVATE)
    private val df = DateTimeFormatter.ISO_LOCAL_DATE

    private fun keyTasks(date: LocalDate) = "plan_tasks_${df.format(date)}"
    private fun keyTitle(date: LocalDate) = "plan_title_${df.format(date)}"

    /* ---------------- Core CRUD ---------------- */

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
                    title  = p.getOrNull(1) ?: "Planned task",
                    type   = p.getOrNull(2)?.ifBlank { null },
                    target = p.getOrNull(3)?.toIntOrNull(),
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

    /* ---------------- Day title ---------------- */

    fun getDayTitle(date: LocalDate): String? = sp.getString(keyTitle(date), null)

    /** Replace only the day's title without touching tasks. */
    fun setDayTitle(date: LocalDate, title: String?) {
        sp.edit {
            if (title == null) remove(keyTitle(date)) else putString(keyTitle(date), title)
        }
    }

    /* ---------------- Convenience APIs ---------------- */

    /** Append one task and return it. */
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

    /** Mark a single task completed / not completed. */
    fun setCompleted(date: LocalDate, taskId: String, completed: Boolean) {
        val updated = getTasks(date).map {
            if (it.taskId == taskId) it.copy(completed = completed) else it
        }
        setTasks(date, getDayTitle(date), updated)
    }

    /**
     * Replace tasks for the date with a new template BUT KEEP any existing
     * "synthetic" tasks (and their completion states). Prevents Quick Add tasks
     * from being lost when Plan re-saves today's template.
     */
    /**
     * Replace tasks for the date with a new template BUT:
     *  1) keep existing completion flags for tasks with the same taskId
     *  2) keep all existing "synthetic" tasks (and their completion states)
     */
    fun setTasksKeepingSynthetic(
        date: LocalDate,
        dayTitle: String?,
        newTasks: List<PlanTask>
    ) {
        val existing = getTasks(date)
        val existingById = existing.associateBy { it.taskId }

        // keep completed for same-id planned tasks
        val mergedPlanned = newTasks.map { nt ->
            val old = existingById[nt.taskId]
            if (old != null && old.type != "synthetic") {
                nt.copy(completed = old.completed)
            } else {
                nt
            }
        }

        // keep all previously quick-added synthetic tasks
        val synthetic = existing.filter { it.type == "synthetic" }

        setTasks(date, dayTitle, mergedPlanned + synthetic)
    }


    /** Handy helpers used by dashboards. */
    fun count(date: LocalDate): Int = getTasks(date).size
    fun completedCount(date: LocalDate): Int = getTasks(date).count { it.completed }

    /** Add a "synthetic" placeholder task (used by Quick Add). */
    fun addSyntheticTask(date: LocalDate, title: String, target: Int? = null): PlanTask =
        addTask(date, title = title, type = "synthetic", target = target)

    /**
     * Remove one "synthetic" task if present (e.g. when deleting a quick-added session).
     * @return the removed task if any, otherwise null
     */
    fun consumeOneSyntheticTask(date: LocalDate): Boolean {
        val tasks = getTasks(date)
        val idx = tasks.indexOfFirst { it.type == "synthetic" }
        if (idx == -1) return false
        val newList = tasks.toMutableList().also { it.removeAt(idx) }
        setTasks(date, getDayTitle(date), newList)
        return true
    }

    /* ---------------- Change listeners (so Workout auto-refreshes) ---------------- */

    fun addOnChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) {
        sp.registerOnSharedPreferenceChangeListener(l)
    }

    fun removeOnChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) {
        sp.unregisterOnSharedPreferenceChangeListener(l)
    }
}
