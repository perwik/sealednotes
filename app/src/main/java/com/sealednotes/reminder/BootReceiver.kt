package com.sealednotes.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sealednotes.SealedNotesApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-schedules all pending reminders after the device reboots.
 * AlarmManager alarms are lost on reboot, so we need to restore them.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val app = context.applicationContext as SealedNotesApp
        val repository = app.repository

        CoroutineScope(Dispatchers.IO).launch {
            val now = System.currentTimeMillis()
            // Get all notes with future reminders
            // For notes with past reminders, we fire them immediately
            val allReminders = repository.getDueReminders(Long.MAX_VALUE)

            for (note in allReminders) {
                val reminderTime = note.reminderTime ?: continue
                if (reminderTime > now) {
                    // Future reminder — reschedule
                    ReminderScheduler.scheduleReminder(
                        context, note.id, note.title, note.content, reminderTime
                    )
                } else {
                    // Past reminder — fire now
                    ReminderScheduler.scheduleReminder(
                        context, note.id, note.title, note.content, now + 1000
                    )
                }
            }
        }
    }
}
