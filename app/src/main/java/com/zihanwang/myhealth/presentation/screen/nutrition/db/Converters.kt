package com.zihanwang.myhealth.presentation.screen.nutrition.db

import androidx.room.TypeConverter
import com.zihanwang.myhealth.presentation.screen.nutrition.MealType
import java.time.LocalDate

// Room converters for LocalDate and MealType.
class Converters {
    @TypeConverter
    fun fromDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun dateToString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun fromMealType(value: String?): MealType? = value?.let { MealType.valueOf(it) }

    @TypeConverter
    fun mealTypeToString(type: MealType?): String? = type?.name
}
