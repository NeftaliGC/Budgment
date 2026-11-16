package com.nintech.budgment.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val HOME_GOALS_EXPANDED_KEY = booleanPreferencesKey("home_goals_expanded")

/** Detalles de presentación que se recuerdan entre aperturas de la app. */
interface UiPreferences {
    /** Sección «Metas de ahorro» de Inicio desplegada. */
    val homeGoalsExpanded: Flow<Boolean>
    suspend fun setHomeGoalsExpanded(expanded: Boolean)
}

@Singleton
class UiPreferencesImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UiPreferences {

    override val homeGoalsExpanded: Flow<Boolean> = dataStore.data.map { it[HOME_GOALS_EXPANDED_KEY] ?: true }

    override suspend fun setHomeGoalsExpanded(expanded: Boolean) {
        dataStore.edit { it[HOME_GOALS_EXPANDED_KEY] = expanded }
    }
}
