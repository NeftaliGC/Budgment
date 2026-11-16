package com.nintech.budgment.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nintech.budgment.data.local.security.PasswordHasher
import com.nintech.budgment.security.AppLockPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val PIN_HASH_KEY = stringPreferencesKey("lock_pin_hash")
private val PIN_LENGTH_KEY = intPreferencesKey("lock_pin_length")
private val BIOMETRIC_KEY = booleanPreferencesKey("lock_biometric")
private val TIMEOUT_KEY = intPreferencesKey("lock_timeout_seconds")
private val FAILED_ATTEMPTS_KEY = intPreferencesKey("lock_failed_attempts")
private val LOCKOUT_UNTIL_KEY = longPreferencesKey("lock_lockout_until")

/**
 * Configuración del bloqueo de la app. Es del dispositivo, no de la cuenta: no se borra al
 * cerrar sesión. El PIN se guarda como hash PBKDF2 con sal, igual que las contraseñas.
 */
data class AppLockConfig(
    val enabled: Boolean = false,
    val pinLength: Int = 0,
    val biometricEnabled: Boolean = false,
    val timeoutSeconds: Int = AppLockPolicy.DEFAULT_TIMEOUT_SECONDS,
    /** Instante (reloj del sistema) hasta el que no se aceptan intentos; 0 si no hay espera. */
    val lockoutUntil: Long = 0
)

sealed interface PinCheck {
    data object Correct : PinCheck
    /** [attemptsLeft]: fallos que quedan antes de la primera espera (0 si ya la hay). */
    data class Wrong(val attemptsLeft: Int, val lockoutUntil: Long) : PinCheck
    data class LockedOut(val until: Long) : PinCheck
}

interface AppLockSettings {
    val config: Flow<AppLockConfig>
    suspend fun setPin(pin: String)
    suspend fun disable()
    suspend fun verifyPin(pin: String, now: Long = System.currentTimeMillis()): PinCheck
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun setTimeoutSeconds(seconds: Int)
}

@Singleton
class AppLockSettingsImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : AppLockSettings {

    override val config: Flow<AppLockConfig> = dataStore.data.map {
        val hash = it[PIN_HASH_KEY]
        AppLockConfig(
            enabled = hash != null,
            pinLength = it[PIN_LENGTH_KEY] ?: 0,
            biometricEnabled = hash != null && (it[BIOMETRIC_KEY] ?: false),
            timeoutSeconds = it[TIMEOUT_KEY]?.takeIf { t -> t in AppLockPolicy.TIMEOUT_OPTIONS_SECONDS }
                ?: AppLockPolicy.DEFAULT_TIMEOUT_SECONDS,
            lockoutUntil = it[LOCKOUT_UNTIL_KEY] ?: 0
        )
    }

    override suspend fun setPin(pin: String) {
        require(AppLockPolicy.isValidPin(pin))
        val hash = withContext(Dispatchers.Default) { PasswordHasher.hash(pin) }
        dataStore.edit {
            it[PIN_HASH_KEY] = hash
            it[PIN_LENGTH_KEY] = pin.length
            it.remove(FAILED_ATTEMPTS_KEY)
            it.remove(LOCKOUT_UNTIL_KEY)
        }
    }

    override suspend fun disable() {
        dataStore.edit {
            it.remove(PIN_HASH_KEY)
            it.remove(PIN_LENGTH_KEY)
            it.remove(BIOMETRIC_KEY)
            it.remove(FAILED_ATTEMPTS_KEY)
            it.remove(LOCKOUT_UNTIL_KEY)
        }
    }

    override suspend fun verifyPin(pin: String, now: Long): PinCheck {
        val prefs = dataStore.data.first()
        val hash = prefs[PIN_HASH_KEY] ?: return PinCheck.Correct
        val lockoutUntil = prefs[LOCKOUT_UNTIL_KEY] ?: 0
        if (now < lockoutUntil) return PinCheck.LockedOut(lockoutUntil)

        if (withContext(Dispatchers.Default) { PasswordHasher.verify(pin, hash) }) {
            dataStore.edit {
                it.remove(FAILED_ATTEMPTS_KEY)
                it.remove(LOCKOUT_UNTIL_KEY)
            }
            return PinCheck.Correct
        }
        val failed = (prefs[FAILED_ATTEMPTS_KEY] ?: 0) + 1
        val wait = AppLockPolicy.lockoutMillis(failed)
        val until = if (wait > 0) now + wait else 0
        dataStore.edit {
            it[FAILED_ATTEMPTS_KEY] = failed
            if (until > 0) it[LOCKOUT_UNTIL_KEY] = until
        }
        return PinCheck.Wrong(
            attemptsLeft = (AppLockPolicy.FREE_ATTEMPTS - failed).coerceAtLeast(0),
            lockoutUntil = until
        )
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[BIOMETRIC_KEY] = enabled }
    }

    override suspend fun setTimeoutSeconds(seconds: Int) {
        require(seconds in AppLockPolicy.TIMEOUT_OPTIONS_SECONDS)
        dataStore.edit { it[TIMEOUT_KEY] = seconds }
    }
}
