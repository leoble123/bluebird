package com.bluebird.messaging.core.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bluebird.messaging.core.data.db.BluebirdDatabase
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Proves the committed schema in app/schemas matches the code: builds a database exactly as the exported JSON
 * describes, then opens it with the app's Room database, which refuses to open anything that doesn't match.
 *
 * When a migration is added, extend this to build the previous version, open it (so the migration runs), and
 * check the data survived.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseSchemaTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    @After
    fun clean() {
        context.deleteDatabase(DB)
    }

    @Test
    fun exportedVersion1SchemaMatchesTheCode() = runTest {
        createFromExportedSchema(version = 1)

        val db = BluebirdDatabase.build(context, name = DB, allowMainThread = true)
        try {
            // Any query forces Room to open and verify the database (identity hash, tables, indexes).
            assertEquals(0, db.messaging().conversationCount())
            db.preferences().put(com.bluebird.messaging.core.data.db.PreferenceEntity("probe", "ok"))
            assertEquals("ok", db.preferences().get("probe"))
        } finally {
            db.close()
        }
    }

    @Test
    fun everyTableTheCodeCreatesMatchesTheExportedSchemaExactly() = runTest {
        val exported = JSONObject(schemaFile(BluebirdDatabaseVersion).readText()).getJSONObject("database")
        val expected = buildMap {
            val entities = exported.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                // SQLite stores the statement without Room's "IF NOT EXISTS".
                put(table, entity.getString("createSql").replace("\${TABLE_NAME}", table).replace("IF NOT EXISTS ", ""))
            }
        }

        val db = BluebirdDatabase.build(context, name = DB, allowMainThread = true)
        try {
            db.messaging().conversationCount() // create the tables
            val actual = buildMap {
                db.openHelper.readableDatabase
                    .query("SELECT name, sql FROM sqlite_master WHERE type = 'table' AND name IN (${expected.keys.joinToString { "'$it'" }})")
                    .use { c -> while (c.moveToNext()) put(c.getString(0), c.getString(1)) }
            }
            assertEquals(expected, actual)
        } finally {
            db.close()
        }
    }

    private fun createFromExportedSchema(version: Int) {
        val json = JSONObject(schemaFile(version).readText()).getJSONObject("database")
        val path = context.getDatabasePath(DB).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { sqlite ->
            val entities = json.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                sqlite.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) {
                    sqlite.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = json.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) sqlite.execSQL(setup.getString(i))
            sqlite.version = json.getInt("version")
        }
    }

    /** Gradle runs unit tests from the module directory, where the schemas are committed. */
    private fun schemaFile(version: Int): File =
        File("schemas/${BluebirdDatabase::class.java.name}/$version.json").also {
            check(it.exists()) { "Exported schema not found at ${it.absolutePath}" }
        }

    private companion object {
        const val DB = "schema-test.db"
        /** The database version currently declared in [BluebirdDatabase]. */
        const val BluebirdDatabaseVersion = 1
    }
}
