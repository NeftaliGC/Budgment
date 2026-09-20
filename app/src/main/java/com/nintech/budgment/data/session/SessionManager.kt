package com.nintech.budgment.data.session

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.sessionDataStore by preferencesDataStore(name = "budgment_session")

private val USER_ID_KEY = stringPreferencesKey("user_id")

interface SessionManager {
    val currentUserId: Flow<String?>
    suspend fun login(userId: String, rememberMe: Boolean)
    suspend fun logout()
}

@Singleton
class SessionManagerImpl @Inject constructor(
    private val dataStore: androidx.datastore.core.DataStore<Preferences>
) : SessionManager {

    // Sesión que NO se guarda en disco — vive solo mientras el proceso está vivo (cuando rememberMe = false)
    private val inMemoryUserId = MutableStateFlow<String?>(null)

    override val currentUserId: Flow<String?> = combine(
        dataStore.data.map { it[USER_ID_KEY] },
        inMemoryUserId
    ) { persisted, memory -> persisted ?: memory }

    override suspend fun login(userId: String, rememberMe: Boolean) {
        if (rememberMe) {
            dataStore.edit { it[USER_ID_KEY] = userId }
        } else {
            inMemoryUserId.value = userId
        }
    }

    override suspend fun logout() {
        inMemoryUserId.value = null
        dataStore.edit { it.remove(USER_ID_KEY) }
    }
}