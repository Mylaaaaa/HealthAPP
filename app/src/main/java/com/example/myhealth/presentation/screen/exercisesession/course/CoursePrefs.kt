package com.example.myhealth.presentation.screen.exercisesession.course

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Minimal progress storage using SharedPreferences.
 * Keys are namespaced by courseId to avoid collisions.
 */
class CoursePrefs(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("course_prefs", Context.MODE_PRIVATE)

    fun getActiveCourseId(): String? = sp.getString("active_course", null)
    fun setActiveCourseId(id: String) = sp.edit { putString("active_course", id) }

    fun getWeekIndex(courseId: String?): Int {
        if (courseId == null) return 0
        return sp.getInt("wkIdx_$courseId", 0).coerceAtLeast(0)
    }

    fun setWeekIndex(courseId: String, index: Int) {
        sp.edit { putInt("wkIdx_$courseId", index.coerceAtLeast(0)) }
    }

    fun getWeekDone(courseId: String?): Int {
        if (courseId == null) return 0
        return sp.getInt("wkDone_$courseId", 0).coerceAtLeast(0)
    }

    fun setWeekDone(courseId: String, value: Int) {
        sp.edit { putInt("wkDone_$courseId", value.coerceAtLeast(0)) }
    }
}
