package com.example.myhealth.presentation.screen.exercisesession.planaccess

import android.content.Context
import java.time.LocalDate

/**
 * Tiny key-value store to persist "is session completed?" flags per day.
 * Key format: "YYYY-MM-DD|<sessionId>" -> boolean
 *
 * This keeps implementation simple today and is easy to replace with Room or a backend later.
 */
class CompletedSessionsStore(context: Context) {
    private val prefs = context.getSharedPreferences("completed_sessions", Context.MODE_PRIVATE)

    private fun key(date: LocalDate, sessionId: String) = "${date}|${sessionId}"

    fun setCompleted(date: LocalDate, sessionId: String, completed: Boolean) {
        prefs.edit().putBoolean(key(date, sessionId), completed).apply()
    }

    fun isCompleted(date: LocalDate, sessionId: String): Boolean {
        return prefs.getBoolean(key(date, sessionId), false)
    }

    fun countCompleted(date: LocalDate, sessionIds: List<String>): Int {
        var c = 0
        for (id in sessionIds) if (isCompleted(date, id)) c++
        return c
    }
}
