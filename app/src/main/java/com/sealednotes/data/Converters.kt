package com.sealednotes.data

import androidx.room.TypeConverter
import com.sealednotes.data.entity.ReminderRecurrence

class Converters {

    @TypeConverter
    fun fromReminderRecurrence(value: ReminderRecurrence?): String? {
        return value?.name
    }

    @TypeConverter
    fun toReminderRecurrence(value: String?): ReminderRecurrence? {
        return value?.let {
            try {
                ReminderRecurrence.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
