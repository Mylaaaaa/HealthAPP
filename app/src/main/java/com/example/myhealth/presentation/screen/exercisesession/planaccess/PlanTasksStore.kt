package com.example.myhealth.presentation.screen.exercisesession.planaccess

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Lightweight store for "today's planned tasks", written by the Plan screen
 * and read by the Workout dashboard.
 *
 * You can later replace this SharedPreferences-based implementation with Room or
 * a remote DB without touching the UI layer. Only this class needs to be swapped.
 *
 * Storage:
 *  - plan_tasks_<yyyy-MM-dd> : each line encodes a task with fields "taskId§title§type§target"
 *  - plan_title_<yyyy-MM-dd> : the day card title shown on Plan (e.g., "MON – fat-loss focus")
 */
class PlanTasksStore(private val context: Context) {

    data class PlanTask(
        val taskId: String = UUID.randomUUID().toString(),
        val title: String,
        val type: String? = null,
        val target: Int? = null
    )

    private val sp get() = context.getSharedPreferences("plan_tasks_store", Context.MODE_PRIVATE)
    private val df = DateTimeFormatter.ISO_LOCAL_DATE

    /** Persist the list of tasks for a specific date and (optionally) the Plan day title. */
    fun setTasks(date: LocalDate, dayTitle: String?, tasks: List<PlanTask>) {
        val d = df.format(date)
        val keyTasks = "plan_tasks_$d"
        val keyTitle = "plan_title_$d"

        val serialized = tasks.joinToString("\n") { t ->
            listOf(t.taskId, t.title, t.type ?: "", t.target?.toString() ?: "").joinToString("§")
        }

        sp.edit().putString(keyTasks, serialized).apply()
        if (dayTitle != null) sp.edit().putString(keyTitle, dayTitle).apply()
    }

    /** Read tasks for a specific date. Returns an empty list if not set. */
    fun getTasks(date: LocalDate): List<PlanTask> {
        val key = "plan_tasks_${df.format(date)}"
        val raw = sp.getString(key, null) ?: return emptyList()
        return raw.lineSequence()
            .filter { it.isNotBlank() }
            .map { line ->
                val p = line.split("§")
                PlanTask(
                    taskId = p.getOrNull(0) ?: UUID.randomUUID().toString(),
                    title  = p.getOrNull(1) ?: "Planned task",
                    type   = p.getOrNull(2)?.ifBlank { null },
                    target = p.getOrNull(3)?.toIntOrNull()
                )
            }.toList()
    }

    /** Read the Plan day title for that date (e.g., "MON – fat-loss focus"). */
    fun getDayTitle(date: LocalDate): String? {
        val key = "plan_title_${df.format(date)}"
        return sp.getString(key, null)
    }

    /** Remove both tasks and title for that date. */
    fun clear(date: LocalDate) {
        val d = df.format(date)
        sp.edit().remove("plan_tasks_$d").remove("plan_title_$d").apply()
    }
}
