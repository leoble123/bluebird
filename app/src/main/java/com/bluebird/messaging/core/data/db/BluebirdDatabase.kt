package com.bluebird.messaging.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlin.coroutines.CoroutineContext

@Database(
    entities = [
        ContactEntity::class,
        ConversationEntity::class,
        ParticipantEntity::class,
        MessageEntity::class,
        ReactionEntity::class,
        AttachmentEntity::class,
        DraftEntity::class,
        PreferenceEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class BluebirdDatabase : RoomDatabase() {
    abstract fun messaging(): MessagingDao
    abstract fun preferences(): PreferencesDao

    companion object {
        const val NAME = "bluebird.db"

        /**
         * Every schema change ships a Migration here. Destructive migration is deliberately never enabled: losing a
         * person's messages because of an app update is not acceptable.
         */
        val MIGRATIONS: Array<Migration> = emptyArray()

        /**
         * Opens the database. Queries run on Room's own background executors (never the main thread) unless
         * [queryContext] is given, which tests use to run queries deterministically. A null [name] creates an
         * in-memory database. [allowMainThread] exists only for JVM tests, where the test thread is the main thread;
         * the app never sets it.
         */
        fun build(
            context: Context,
            name: String? = NAME,
            queryContext: CoroutineContext? = null,
            allowMainThread: Boolean = false,
        ): BluebirdDatabase {
            val builder = if (name == null) {
                Room.inMemoryDatabaseBuilder(context, BluebirdDatabase::class.java)
            } else {
                Room.databaseBuilder(context, BluebirdDatabase::class.java, name)
            }
            return builder
                .addMigrations(*MIGRATIONS)
                .addCallback(EnforceForeignKeys)
                .apply { if (queryContext != null) setQueryCoroutineContext(queryContext) }
                .apply { if (allowMainThread) allowMainThreadQueries() }
                .build()
        }
    }

    /** SQLite leaves foreign keys off by default; the cascades above depend on them. */
    private object EnforceForeignKeys : Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }
}
