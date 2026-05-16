package com.sealednotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sealednotes.crypto.KeyManager
import com.sealednotes.data.dao.ChecklistItemDao
import com.sealednotes.data.dao.LabelDao
import com.sealednotes.data.dao.NoteDao
import com.sealednotes.data.entity.ChecklistItem
import com.sealednotes.data.entity.Label
import com.sealednotes.data.entity.Note
import com.sealednotes.data.entity.NoteLabelCrossRef
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        Note::class,
        ChecklistItem::class,
        Label::class,
        NoteLabelCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SealedNotesDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun checklistItemDao(): ChecklistItemDao
    abstract fun labelDao(): LabelDao

    companion object {
        private const val DATABASE_NAME = "sealednotes.db"

        @Volatile
        private var INSTANCE: SealedNotesDatabase? = null

        fun getInstance(context: Context): SealedNotesDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): SealedNotesDatabase {
            val keyManager = KeyManager(context)
            val passphrase = keyManager.getOrCreateDatabaseKey()
            val factory = SupportOpenHelperFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                SealedNotesDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .build()
        }

        /**
         * Returns the path to the encrypted database file.
         * Used for backup/export operations.
         */
        fun getDatabasePath(context: Context): String {
            return context.getDatabasePath(DATABASE_NAME).absolutePath
        }

        fun getDatabaseFile(context: Context): java.io.File {
            return context.getDatabasePath(DATABASE_NAME)
        }

        /**
         * Closes the open instance and clears the cache so the next [getInstance]
         * call rebuilds against the current key + database file. Used after
         * key/DB import. The caller is expected to restart the app afterwards.
         */
        fun closeAndReset() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
