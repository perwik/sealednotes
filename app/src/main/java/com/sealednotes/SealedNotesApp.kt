package com.sealednotes

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.sealednotes.data.SealedNotesDatabase
import com.sealednotes.data.repository.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SealedNotesApp : Application() {

    val database: SealedNotesDatabase by lazy {
        SealedNotesDatabase.getInstance(this)
    }

    val repository: NoteRepository by lazy {
        NoteRepository(
            noteDao = database.noteDao(),
            checklistItemDao = database.checklistItemDao(),
            labelDao = database.labelDao()
        )
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        System.loadLibrary("sqlcipher")
        super.onCreate()
        createNotificationChannel()
        purgeOldTrashedNotes()
    }

    private fun purgeOldTrashedNotes() {
        appScope.launch {
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(TRASH_RETENTION_DAYS)
            repository.deleteOldTrashedNotes(cutoff)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            "Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Note and checklist reminders"
            enableVibration(true)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val REMINDER_CHANNEL_ID = "sealednotes_reminders"
        const val TRASH_RETENTION_DAYS: Long = 30
    }
}
