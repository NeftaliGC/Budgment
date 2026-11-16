package com.nintech.budgment.data.local.security

import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * PBKDF2 con HMAC-SHA256. Usa la implementación del sistema cuando existe; en Android 7
 * (API 24 y 25) el sistema no la trae y se calcula aquí con el mismo resultado.
 */
object Pbkdf2 {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val HMAC = "HmacSHA256"

    fun derive(password: CharArray, salt: ByteArray, iterations: Int, keyBytes: Int = 32): ByteArray =
        try {
            val spec = PBEKeySpec(password, salt, iterations, keyBytes * 8)
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } catch (_: NoSuchAlgorithmException) {
            deriveManually(password, salt, iterations, keyBytes)
        }

    /** RFC 8018, sección 5.2. La contraseña se codifica en UTF-8, igual que hace el sistema. */
    internal fun deriveManually(password: CharArray, salt: ByteArray, iterations: Int, keyBytes: Int): ByteArray {
        require(password.isNotEmpty() && iterations > 0 && keyBytes > 0)
        val mac = Mac.getInstance(HMAC)
        mac.init(SecretKeySpec(String(password).toByteArray(Charsets.UTF_8), HMAC))
        val out = ByteArray(keyBytes)
        var offset = 0
        var block = 1
        while (offset < keyBytes) {
            mac.update(salt)
            mac.update(byteArrayOf((block ushr 24).toByte(), (block ushr 16).toByte(), (block ushr 8).toByte(), block.toByte()))
            var u = mac.doFinal()
            val t = u.copyOf()
            repeat(iterations - 1) {
                u = mac.doFinal(u)
                for (i in t.indices) t[i] = (t[i].toInt() xor u[i].toInt()).toByte()
            }
            val n = minOf(t.size, keyBytes - offset)
            t.copyInto(out, offset, 0, n)
            offset += n
            block++
        }
        return out
    }
}
