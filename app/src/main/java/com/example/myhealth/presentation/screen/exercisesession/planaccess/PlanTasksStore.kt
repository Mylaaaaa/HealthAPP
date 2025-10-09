package com.example.myhealth.presentation.screen.exercisesession.planaccess

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Lightweight store for "today's planned tasks".
 *
 * Why this class exists:
 * - The Plan page writes the tasks of a day.
 * - The Workout dashboard reads the count to display "Planned" and progress.
 *
 * Storage model (backed by SharedPreferences):
 *  - Key: plan_tasks_<yyyy-MM-dd>
 *      Value: newline-joined tasks; every task is serialized as "taskId§title§type§target"
 *  - Key: plan_title_<yyyy-MM-dd>
 *      Value: optional title for that day shown on Plan (e.g., "MON – fat-loss focus")
 *
 * You can later swap this file with Room/remote DB. UI code should not change.
 */
class PlanTasksStore(private val context: Context) {

    /** Single planned task model saved in the store. */
    data class PlanTask(
        val taskId: String = UUID.randomUUID().toString(),
        val title: String,
        val type: String? = null,
        val target: Int? = null
    )

    private val sp get() = context.getSharedPreferences("plan_tasks_store", Context.MODE_PRIVATE)
    private val df = DateTimeFormatter.ISO_LOCAL_DATE

    /** Overwrite tasks for a date and optionally update its title. */
    fun setTasks(date: LocalDate, dayTitle: String?, tasks: List<PlanTask>) {
        val d = df.format(date)
        val keyTasks = "plan_tasks_$d"
        val keyTitle = "plan_title_$d"

        val serialized = tasks.joinToString("\n") { t ->
            listOf(
                t.taskId,
                t.title,
                t.type ?: "",
                t.target?.toString() ?: ""
            ).joinToString("§")
        }

        sp.edit {
            putString(keyTasks, serialized)
            if (dayTitle != null) putString(keyTitle, dayTitle)
        }
    }

    /** Read tasks for a date. Returns an empty list if nothing is stored. */
    fun getTasks(date: LocalDate): List<PlanTask> {
        val key = "plan_tasks_${df.format(date)}"
        val raw = sp.getString(key, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()

        return raw.lineSequence()
            .filter { it.isNotBlank() }
            .map { line ->
                val p = line.split("§")
                PlanTask(
                    taskId = p.getOrNull(0) ?: UUID.randomUUID().toString(),
                    title = p.getOrNull(1) ?: "Planned task",
                    type = p.getOrNull(2)?.ifBlank { null },
                    target = p.getOrNull(3)?.toIntOrNull()
                )
            }.toList()
    }

    /** Get the Plan day title (nullable). */
    fun getDayTitle(date: LocalDate): String? {
        val key = "plan_title_${df.format(date)}"
        return sp.getString(key, null)
    }

    /** Remove both tasks and title for a date. */
    fun clear(date: LocalDate) {
        val d = df.format(date)
        sp.edit {
            remove("plan_tasks_$d")
            remove("plan_title_$d")
        }
    }

    /* ---------------- Convenience APIs added for Workout dashboard ---------------- */

    /**
     * Append a new task to the date and return it.
     * This is used by "Quick Add" buttons on the Workout page to bump Planned +1.
     *
     * @param dayTitle If provided, update the day's title at the same time; if null, keep existing.
     */
    fun addTask(
        date: LocalDate,
        title: String,
        type: String? = null,
        target: Int? = null,
        dayTitle: String? = null
    ): PlanTask {
        val current = getTasks(date)
        val newTask = PlanTask(title = title, type = type, target = target)
        val effectiveTitle = dayTitle ?: getDayTitle(date)
        setTasks(date, effectiveTitle, current + newTask)
        return newTask
    }

    /** Remove one task by id. No-op if not found. */
    fun removeTask(date: LocalDate, taskId: String) {
        val filtered = getTasks(date).filterNot { it.taskId == taskId }
        setTasks(date, getDayTitle(date), filtered)
    }

    /** Replace only the day's title without touching tasks. */
    fun setDayTitle(date: LocalDate, title: String?) {
        val d = df.format(date)
        val keyTitle = "plan_title_$d"
        sp.edit {
            if (title == null) remove(keyTitle) else putString(keyTitle, title)
        }
    }

    /** Handy helper used by dashboards. */
    fun count(date: LocalDate): Int = getTasks(date).size
    /**
     * Add a "synthetic" placeholder task for Quick Add from the Workout page.
     * This increases the planned count instantly.
     */
    fun addSyntheticTask(
        date: LocalDate,
        title: String,
        target: Int? = null
    ): PlanTask {
        // reuse normal addTask() but tag as synthetic
        return addTask(date, title = title, type = "synthetic", target = target)
    }

    /**
     * Remove one "synthetic" task if present.
     * This is used when deleting a session added via Quick Add,
     * so the Planned count decreases by 1.
     *
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
}
