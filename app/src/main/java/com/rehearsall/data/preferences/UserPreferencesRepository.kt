package com.rehearsall.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rehearsall.domain.model.OverlayMode
import com.rehearsall.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads/writes user preferences via DataStore.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SKIP_INCREMENT_MS = longPreferencesKey("skip_increment_ms")
        val LOOP_CROSSFADE = booleanPreferencesKey("loop_crossfade")
        val WAVEFORM_OVERLAY = stringPreferencesKey("waveform_overlay")
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        when (prefs[THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val skipIncrementMs: Flow<Long> = dataStore.data.map { prefs ->
        prefs[SKIP_INCREMENT_MS] ?: 5000L
    }

    val loopCrossfade: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[LOOP_CROSSFADE] ?: true
    }

    val waveformOverlay: Flow<OverlayMode> = dataStore.data.map { prefs ->
        when (prefs[WAVEFORM_OVERLAY]) {
            "LOOPS" -> OverlayMode.LOOPS
            "CHUNKS" -> OverlayMode.CHUNKS
            else -> OverlayMode.NONE
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[THEME_MODE] = mode.name
        }
    }

    suspend fun setSkipIncrementMs(ms: Long) {
        dataStore.edit { prefs ->
            prefs[SKIP_INCREMENT_MS] = ms
        }
    }

    suspend fun setLoopCrossfade(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[LOOP_CROSSFADE] = enabled
        }
    }

    suspend fun setWaveformOverlay(mode: OverlayMode) {
        dataStore.edit { prefs ->
            prefs[WAVEFORM_OVERLAY] = mode.name
        }
    }
}
