package com.sealednotes.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra(ReminderReceiver.EXTRA_NOTE_ID, -1)
        val title = intent.getStringExtra(ReminderReceiver.EXTRA_NOTE_TITLE) ?: "Reminder"
        val content = intent.getStringExtra(ReminderReceiver.EXTRA_NOTE_CONTENT) ?: ""
        val minutes = intent.getIntExtra(ReminderReceiver.EXTRA_SNOOZE_MINUTES, 15)

        if (noteId == -1L) return

        // Dismiss the current notification
        NotificationManagerCompat.from(context).cancel(noteId.toInt())

        // Schedule a new alarm for the snooze duration
        val snoozeTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        ReminderScheduler.scheduleReminder(context, noteId, title, content, snoozeTime)
    }
}
