package com.nintech.budgment.security

/** Reglas del bloqueo de la app, sin dependencias de Android para poder probarlas. */
object AppLockPolicy {
    const val MIN_PIN_LENGTH = 4
    const val MAX_PIN_LENGTH = 6

    /** Intentos fallidos permitidos antes de la primera espera. */
    const val FREE_ATTEMPTS = 5
    private const val FIRST_LOCKOUT_MS = 30_000L
    private const val MAX_LOCKOUT_MS = 15 * 60_000L

    /** Tiempo en segundo plano antes de volver a pedir el PIN. 0 = al salir. */
    val TIMEOUT_OPTIONS_SECONDS = listOf(0, 30, 60, 300)
    const val DEFAULT_TIMEOUT_SECONDS = 0

    fun isValidPin(pin: String): Boolean =
        pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && pin.all { it in '0'..'9' }

    /**
     * Espera tras [failedAttempts] fallos seguidos: ninguna hasta el quinto; luego 30 s, y se
     * duplica con cada fallo hasta 15 min. Frena probar los 10 000 PIN de 4 dígitos a mano.
     */
    fun lockoutMillis(failedAttempts: Int): Long {
        if (failedAttempts < FREE_ATTEMPTS) return 0
        val doublings = (failedAttempts - FREE_ATTEMPTS).coerceAtMost(10)
        return (FIRST_LOCKOUT_MS shl doublings).coerceAtMost(MAX_LOCKOUT_MS)
    }

    /** Si al volver de segundo plano tras [elapsedMs] hay que pedir el PIN otra vez. */
    fun shouldRelock(elapsedMs: Long, timeoutSeconds: Int): Boolean =
        elapsedMs < 0 || elapsedMs >= timeoutSeconds * 1000L
}
