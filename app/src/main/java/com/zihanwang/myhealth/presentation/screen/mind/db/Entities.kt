package com.zihanwang.myhealth.presentation.screen.mind.db

import androidx.room.*
import java.time.LocalDate

@Entity(tableName = "mind_sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val minutes: Int,
    val tag: String
)

@Entity(tableName = "mind_moods")
data class MoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val mood: String // "great"/"good"/"okay"/"bad"/"stressed"
)
