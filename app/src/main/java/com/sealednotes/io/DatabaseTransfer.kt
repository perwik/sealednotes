package com.sealednotes.io

import android.app.Application
import android.net.Uri
import com.sealednotes.data.SealedNotesDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Copies the encrypted SQLCipher database file in and out of the app via SAF URIs.
 * The .db file is opaque ciphertext — without the matching encryption key, it cannot be opened.
 */
class DatabaseTransfer(private val app: Application) {

    suspend fun exportTo(uri: Uri) = withContext(Dispatchers.IO) {
        // Close the DB so any pending WAL is consolidated and the file is in a stable state.
        SealedNotesDatabase.closeAndReset()

        val dbFile = SealedNotesDatabase.getDatabaseFile(app)
        if (!dbFile.exists()) {
            throw IllegalStateException("No database file to export yet")
        }

        val out = app.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Could not open destination for writing")
        out.use { os ->
            dbFile.inputStream().use { it.copyTo(os) }
        }
    }

    suspend fun importFrom(uri: Uri) = withContext(Dispatchers.IO) {
        SealedNotesDatabase.closeAndReset()

        val dbFile = SealedNotesDatabase.getDatabaseFile(app)
        // Remove sidecar files so SQLite can't replay them against the new DB.
        File(dbFile.absolutePath + "-wal").delete()
        File(dbFile.absolutePath + "-shm").delete()

        // Make sure the databases/ directory exists in case the user has never written before.
        dbFile.parentFile?.mkdirs()

        val input = app.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not open source for reading")
        input.use { ins ->
            dbFile.outputStream().use { ins.copyTo(it) }
        }
    }
}
