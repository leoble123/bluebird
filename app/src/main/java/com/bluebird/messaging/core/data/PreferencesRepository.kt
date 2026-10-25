package com.bluebird.messaging.core.data

import com.bluebird.messaging.core.data.db.BluebirdDatabase
import com.bluebird.messaging.core.data.db.PreferenceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class ThemeMode { System, Light, Dark }

/** User preferences, persisted in the database's key-value table. Settings UI arrives in a later phase. */
class PreferencesRepository(db: BluebirdDatabase) {
    private val dao = db.preferences()

    fun observeThemeMode(): Flow<ThemeMode> =
        dao.observe(THEME_MODE).map { value -> ThemeMode.entries.firstOrNull { it.name == value } ?: ThemeMode.System }
            .distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) = dao.put(PreferenceEntity(THEME_MODE, mode.name))

    /** Whether sample conversations were already added once; they're never re-added after you delete them. */
    suspend fun isSampleDataSeeded(): Boolean = dao.get(SAMPLE_SEEDED) == "true"

    suspend fun markSampleDataSeeded() = dao.put(PreferenceEntity(SAMPLE_SEEDED, "true"))

    private companion object {
        const val THEME_MODE = "theme_mode"
        const val SAMPLE_SEEDED = "sample_data_seeded"
    }
}
