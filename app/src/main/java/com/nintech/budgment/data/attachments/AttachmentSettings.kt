package com.nintech.budgment.data.attachments

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val RETENTION_MONTHS_KEY = intPreferencesKey("attachment_retention_months")

/** Cuánto tiempo se conservan las fotos de recibos. Es del dispositivo: no se borra al cerrar sesión. */
@Singleton
class AttachmentSettings @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    /** Meses, o [AttachmentPolicy.KEEP_FOREVER]. */
    val retentionMonths: Flow<Int> = dataStore.data.map {
        it[RETENTION_MONTHS_KEY]?.takeIf { months -> months in AttachmentPolicy.RETENTION_OPTIONS_MONTHS }
            ?: AttachmentPolicy.DEFAULT_RETENTION_MONTHS
    }

    suspend fun setRetentionMonths(months: Int) {
        require(months in AttachmentPolicy.RETENTION_OPTIONS_MONTHS)
        dataStore.edit { it[RETENTION_MONTHS_KEY] = months }
    }
}
