package com.example.myhealth.presentation.screen.exercisesession.course

/**
 * Lightweight course metadata used by the Courses screen.
 * Keep it public to avoid 'public exposes internal type' warnings.
 */
data class CourseLite(
    val id: String,
    val title: String,
    val weeks: Int,
    val daysPerWeek: Int
)

/**
 * Very small in-memory repository with sample data.
 * Replace with your real source later.
 */
class CoursesRepository {
    private val all = listOf(
        CourseLite(id = "fatloss_4w", title = "4-week Fat Loss Journey", weeks = 4, daysPerWeek = 5),
        CourseLite(id = "lean_6w", title = "Lean Strength", weeks = 6, daysPerWeek = 4),
        CourseLite(id = "hiit_3w", title = "HIIT Booster", weeks = 3, daysPerWeek = 3)
    )

    fun myCourses(): List<CourseLite> = listOf(all[0]) // demo: user already joined the first
    fun discover(): List<CourseLite> = all.drop(0)    // demo: others are discoverable
    fun findById(id: String?): CourseLite? = all.firstOrNull { it.id == id }
}
