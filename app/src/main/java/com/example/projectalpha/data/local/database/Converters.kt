package com.example.projectalpha.data.local.database // Ensure this matches the directory structure

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime // <<< ADD IMPORT
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter // <<< ADD IMPORT

class Converters { // Ensure the class name is exactly "Converters"
    // For List<String> (e.g., daysOfWeek in HabitEntity)
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        // Handle empty string from DB correctly, map to emptyList or null as appropriate
        return if (value.isNullOrEmpty()) emptyList() else value.split(",").map { it.trim() }
    }

    // For LocalDateTime
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let { LocalDateTime.ofEpochSecond(it, 0, ZoneOffset.UTC) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.toEpochSecond(ZoneOffset.UTC)
    }

    // For LocalDate
    @TypeConverter
    fun fromDateStamp(value: Long?): LocalDate? { // Changed name for clarity from your previous
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun localDateToTimestamp(date: LocalDate?): Long? { // Changed name for clarity
        return date?.toEpochDay()
    }

    // For LocalTime  <<< --- ADD THESE ---
    @TypeConverter
    fun fromLocalTimeToString(time: LocalTime?): String? {
        return time?.format(DateTimeFormatter.ISO_LOCAL_TIME)
    }

    @TypeConverter
    fun stringToLocalTime(value: String?): LocalTime? {
        return value?.let { LocalTime.parse(it, DateTimeFormatter.ISO_LOCAL_TIME) }
    }
}
