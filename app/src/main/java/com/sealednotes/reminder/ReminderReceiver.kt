package com.sealednotes.reminder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sealednotes.MainActivity
import com.sealednotes.R
import com.sealednotes.SealedNotesApp
import com.sealednotes.data.entity.ReminderRecurrence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1)
        if (noteId == -1L) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as SealedNotesApp
                val note = app.repository.getNoteByIdOnce(noteId)?.note

                // Silently skip stale alarms: note gone, trashed, or reminder cleared.
                if (note == null || note.isTrashed) return@launch
                val currentReminderTime = note.reminderTime ?: return@launch

                showNotification(context, noteId, note.title, note.content)

                val recurrence = note.reminderRecurrence ?: ReminderRecurrence.NONE
                if (recurrence != ReminderRecurrence.NONE) {
                    val nextTime = computeNextOccurrence(currentReminderTime, recurrence)
                    app.repository.setReminder(noteId, nextTime, recurrence)
                    ReminderScheduler.scheduleReminder(
                        context, noteId, note.title, note.content, nextTime
                    )
                } else {
                    // One-shot: clear so the note no longer appears under "Reminders".
                    app.repository.setReminder(noteId, null, null)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun showNotification(context: Context, noteId: Long, title: String, content: String) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTE_ID, noteId)
        }
        val pendingOpen = PendingIntent.getActivity(
            context, noteId.toInt(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze15 = createSnoozeIntent(context, noteId, title, content, 15)
        val snooze60 = createSnoozeIntent(context, noteId, title, content, 60)

        val notification = NotificationCompat.Builder(context, SealedNotesApp.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title.ifEmpty { "SealedNotes Reminder" })
            .setContentText(content.take(200))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingOpen)
            .addAction(0, "15 min", snooze15)
            .addAction(0, "1 hour", snooze60)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(noteId.toInt(), notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted — silently drop.
        }
    }

    private fun createSnoozeIntent(
        context: Context,
        noteId: Long,
        title: String,
        content: String,
        minutes: Int,
    ): PendingIntent {
        val intent = Intent(context, SnoozeReceiver::class.java).apply {
            putExtra(EXTRA_NOTE_ID, noteId)
            putExtra(EXTRA_NOTE_TITLE, title)
            putExtra(EXTRA_NOTE_CONTENT, content)
            putExtra(EXTRA_SNOOZE_MINUTES, minutes)
        }
        val requestCode = noteId.toInt() * 100 + minutes
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun computeNextOccurrence(currentTime: Long, recurrence: ReminderRecurrence): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = currentTime }
        val now = System.currentTimeMillis()
        // Advance until strictly in the future — protects against the device having been
        // offline long enough that one step still lands in the past.
        do {
            when (recurrence) {
                ReminderRecurrence.DAILY -> cal.add(Calendar.DAY_OF_MONTH, 1)
                ReminderRecurrence.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                ReminderRecurrence.MONTHLY -> cal.add(Calendar.MONTH, 1)
                ReminderRecurrence.NONE -> return cal.timeInMillis
            }
        } while (cal.timeInMillis <= now)
        return cal.timeInMillis
    }

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_NOTE_TITLE = "note_title"
        const val EXTRA_NOTE_CONTENT = "note_content"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
    }
}
