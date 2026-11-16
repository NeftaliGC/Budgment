package com.nintech.budgment.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nintech.budgment.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val HOME_GOALS_EXPANDED_KEY = booleanPreferencesKey("home_goals_expanded")
private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

/** Detalles de presentación que se recuerdan entre aperturas de la app. */
interface UiPreferences {
    /** Sección «Metas de ahorro» de Inicio desplegada. */
    val homeGoalsExpanded: Flow<Boolean>
    suspend fun setHomeGoalsExpanded(expanded: Boolean)

    /** Tema claro, oscuro o el del teléfono. Es del dispositivo: no cambia al cerrar sesión. */
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}

@Singleton
class UiPreferencesImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UiPreferences {

    override val homeGoalsExpanded: Flow<Boolean> = dataStore.data.map { it[HOME_GOALS_EXPANDED_KEY] ?: true }

    override suspend fun setHomeGoalsExpanded(expanded: Boolean) {
        dataStore.edit { it[HOME_GOALS_EXPANDED_KEY] = expanded }
    }

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[THEME_MODE_KEY] } ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }
}
