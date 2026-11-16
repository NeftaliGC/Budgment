package com.nintech.budgment.data.local.security

import android.util.Base64
import java.security.SecureRandom

object PasswordHasher {
    private const val ITERATIONS = 120_000

    fun hash(password: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val digest = pbkdf2(password, salt, ITERATIONS)
        return "$ITERATIONS:${Base64.encodeToString(salt, Base64.NO_WRAP)}:${Base64.encodeToString(digest, Base64.NO_WRAP)}"
    }

    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 3) return false
        val iterations = parts[0].toIntOrNull() ?: return false
        val salt = Base64.decode(parts[1], Base64.NO_WRAP)
        val expected = Base64.decode(parts[2], Base64.NO_WRAP)
        return pbkdf2(password, salt, iterations).contentEquals(expected)
    }

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int): ByteArray =
        Pbkdf2.derive(password.toCharArray(), salt, iterations)
}