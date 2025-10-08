package com.example.myhealth.presentation.screen.exercisesession

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * ViewModel for Guided workout runtime state.
 * - Holds ActiveWorkout, current index, timers, and simple set tracking.
 * - Navigation-agnostic: host screen decides when to show Guided/Summary.
 */
class GuidedWorkoutViewModel : ViewModel() {

    var activeWorkout by mutableStateOf<ActiveWorkout?>(null)
        private set

    var currentExerciseIndex by mutableStateOf(0)
        private set

    var isWorkoutRunning by mutableStateOf(false)
        private set

    var totalElapsedMs by mutableStateOf(0L)
        private set

    private var lastTickMs: Long = 0L

    /** Start a guided workout from a plan day definition. */
    fun startFromPlan(day: PlanDay) {
        val items = day.items.map { label ->
            when {
                label.contains("strength", true) -> ActiveItem.Strength(name = label)
                label.contains("core", true) -> ActiveItem.Core(name = label)
                label.contains("mobility", true) || label.contains("stretch", true) -> ActiveItem.Mobility(name = label)
                label.contains("hiit", true) -> ActiveItem.Cardio(name = label)
                else -> ActiveItem.Cardio(name = label)
            }
        }.toMutableList()

        activeWorkout = ActiveWorkout(
            dayTitle = day.title,
            items = items,
            startedAt = System.currentTimeMillis()
        )
        currentExerciseIndex = 0
        isWorkoutRunning = false
        totalElapsedMs = 0L
        lastTickMs = 0L
    }

    /** Toggle play/pause for the whole workout and the current item. */
    fun toggleRunPause(nowElapsedRealtimeMs: Long) {
        val aw = activeWorkout ?: return
        val item = aw.items[currentExerciseIndex]
        if (!isWorkoutRunning) {
            isWorkoutRunning = true
            item.isRunning = true
            lastTickMs = nowElapsedRealtimeMs
        } else {
            if (lastTickMs != 0L) {
                val delta = nowElapsedRealtimeMs - lastTickMs
                totalElapsedMs += delta
                item.elapsedMs += delta
            }
            isWorkoutRunning = false
            item.isRunning = false
            lastTickMs = 0L
        }
    }

    /** Called by UI ticker (e.g. every 1s) while running. */
    fun onTick(nowElapsedRealtimeMs: Long) {
        val aw = activeWorkout ?: return
        if (!isWorkoutRunning) return
        if (lastTickMs == 0L) {
            lastTickMs = nowElapsedRealtimeMs
            return
        }
        val delta = nowElapsedRealtimeMs - lastTickMs
        totalElapsedMs += delta
        aw.items[currentExerciseIndex].elapsedMs += delta
        lastTickMs = nowElapsedRealtimeMs
    }

    fun markDone() {
        val aw = activeWorkout ?: return
        val idx = currentExerciseIndex
        aw.items[idx].status = ItemStatus.DONE
        aw.items[idx].isRunning = false
        if (idx < aw.items.lastIndex) currentExerciseIndex = idx + 1
    }

    fun skip() {
        val aw = activeWorkout ?: return
        val idx = currentExerciseIndex
        aw.items[idx].status = ItemStatus.SKIPPED
        aw.items[idx].isRunning = false
        if (idx < aw.items.lastIndex) currentExerciseIndex = idx + 1
    }

    fun addStrengthSet(reps: Int?, weightKg: Float?) {
        val aw = activeWorkout ?: return
        val item = aw.items[currentExerciseIndex]
        if (item is ActiveItem.Strength) {
            item.sets += StrengthSet(reps, weightKg)
        }
    }

    /** Finalize with user inputs (RPE/notes) and return a summary snapshot. */
    fun finish(overallRpe: Int, notes: String?): WorkoutSummaryData? {
        val aw = activeWorkout ?: return null
        isWorkoutRunning = false
        aw.finishedAt = System.currentTimeMillis()
        aw.overallRpe = overallRpe
        aw.overallNotes = notes

        val done = aw.items.count { it.status == ItemStatus.DONE }
        val completion = if (aw.items.isEmpty()) 0f else done.toFloat() / aw.items.size

        return WorkoutSummaryData(
            title = aw.dayTitle,
            totalMs = totalElapsedMs,
            items = aw.items.toList(),
            completionRate = completion,
            overallRpe = overallRpe,
            notes = notes
        )
    }

    /** Clear runtime state (call when exiting Guided or after saving). */
    fun clear() {
        activeWorkout = null
        currentExerciseIndex = 0
        isWorkoutRunning = false
        totalElapsedMs = 0L
        lastTickMs = 0L
    }
}

/* -------------------- Runtime data for guided execution ------------------- */

data class ActiveWorkout(
    val dayTitle: String,
    val items: MutableList<ActiveItem>,
    val startedAt: Long,
    var finishedAt: Long? = null,
    var overallRpe: Int? = null,
    var overallNotes: String? = null
)

enum class ItemStatus { PENDING, DONE, SKIPPED }

sealed class ActiveItem(
    open val name: String,
    open var elapsedMs: Long = 0L,
    open var isRunning: Boolean = false,
    open var status: ItemStatus = ItemStatus.PENDING
) {
    data class Cardio(
        override val name: String,
        override var elapsedMs: Long = 0L,
        var rpe: Int? = null,
        var notes: String? = null,
        override var isRunning: Boolean = false,
        override var status: ItemStatus = ItemStatus.PENDING
    ) : ActiveItem(name, elapsedMs, isRunning, status)

    data class Mobility(
        override val name: String,
        override var elapsedMs: Long = 0L,
        var notes: String? = null,
        override var isRunning: Boolean = false,
        override var status: ItemStatus = ItemStatus.PENDING
    ) : ActiveItem(name, elapsedMs, isRunning, status)

    data class Core(
        override val name: String,
        override var elapsedMs: Long = 0L,
        var notes: String? = null,
        override var isRunning: Boolean = false,
        override var status: ItemStatus = ItemStatus.PENDING
    ) : ActiveItem(name, elapsedMs, isRunning, status)

    data class Strength(
        override val name: String,
        val sets: MutableList<StrengthSet> = mutableListOf(),
        override var elapsedMs: Long = 0L,
        var notes: String? = null,
        override var isRunning: Boolean = false,
        override var status: ItemStatus = ItemStatus.PENDING
    ) : ActiveItem(name, elapsedMs, isRunning, status)
}

data class StrengthSet(val reps: Int?, val weightKg: Float?)

data class WorkoutSummaryData(
    val title: String,
    val totalMs: Long,
    val items: List<ActiveItem>,
    val completionRate: Float,
    val overallRpe: Int,
    val notes: String?
)
