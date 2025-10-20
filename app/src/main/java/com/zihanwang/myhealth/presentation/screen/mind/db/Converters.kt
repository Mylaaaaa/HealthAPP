package com.zihanwang.myhealth.presentation.screen.mind.db

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter fun fromLocalDate(d: LocalDate?): String? = d?.toString()
    @TypeConverter fun toLocalDate(s: String?): LocalDate? = s?.let { LocalDate.parse(it) }
}
