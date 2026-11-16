package com.nintech.budgment.data.backup

import com.nintech.budgment.data.local.security.Pbkdf2
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class BackupCryptoTest {

    // Pocas iteraciones: aquí se prueba el formato, no la resistencia de la contraseña.
    private val iterations = 10_000
    private val password = "contraseña-segura".toCharArray()

    private fun encrypt(plain: ByteArray, password: CharArray = this.password): ByteArray {
        val out = ByteArrayOutputStream()
        BackupCrypto.encrypt(out, password, createdAt = 1_700_000_000_000, schemaVersion = 6, iterations = iterations)
            .use { it.write(plain) }
        return out.toByteArray()
    }

    private fun decrypt(file: ByteArray, password: CharArray = this.password): ByteArray =
        BackupCrypto.decrypt(ByteArrayInputStream(file), password).second.use { it.readBytes() }

    private fun bytes(size: Int) = ByteArray(size).also { Random(size.toLong()).nextBytes(it) }

    private fun assertFails(expected: BackupError, block: () -> Unit) {
        try {
            block()
            fail("Se esperaba $expected")
        } catch (e: BackupException) {
            assertEquals(expected, e.error)
        }
    }

    @Test
    fun `lo cifrado se recupera igual en cualquier tamano`() {
        val chunk = BackupCrypto.CHUNK_SIZE
        listOf(0, 1, 1000, chunk - 1, chunk, chunk + 1, 2 * chunk, 3 * chunk + 17).forEach { size ->
            val plain = bytes(size)
            assertArrayEquals("tamaño $size", plain, decrypt(encrypt(plain)))
        }
    }

    @Test
    fun `la cabecera se lee sin contrasena`() {
        val header = BackupCrypto.readHeader(ByteArrayInputStream(encrypt(bytes(10))))
        assertEquals(BackupCrypto.FORMAT_VERSION, header.formatVersion)
        assertEquals(1_700_000_000_000, header.createdAt)
        assertEquals(6, header.schemaVersion)
        assertEquals(iterations, header.iterations)
    }

    @Test
    fun `el contenido no queda a la vista y cada archivo es distinto`() {
        val plain = "saldo secreto ".repeat(100).toByteArray()
        val a = encrypt(plain)
        val b = encrypt(plain)
        assertFalse(String(a, Charsets.ISO_8859_1).contains("saldo secreto"))
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun `otra contrasena no abre el respaldo`() {
        val file = encrypt(bytes(5000))
        assertFails(BackupError.WRONG_PASSWORD) { decrypt(file, "otra-contraseña".toCharArray()) }
    }

    @Test
    fun `un archivo cualquiera no es un respaldo`() {
        assertFails(BackupError.NOT_A_BACKUP) { decrypt(bytes(500)) }
        assertFails(BackupError.NOT_A_BACKUP) { decrypt(ByteArray(3)) }
        assertFails(BackupError.NOT_A_BACKUP) { decrypt(ByteArray(0)) }
    }

    @Test
    fun `un formato mas nuevo se reconoce`() {
        val file = encrypt(bytes(10))
        file[8] = (BackupCrypto.FORMAT_VERSION + 1).toByte()
        assertFails(BackupError.NEWER_VERSION) { decrypt(file) }
    }

    @Test
    fun `cambiar un byte del contenido se detecta`() {
        val file = encrypt(bytes(3 * BackupCrypto.CHUNK_SIZE))
        val damaged = file.copyOf().also { it[it.size - 100] = (it[it.size - 100] + 1).toByte() }
        assertFails(BackupError.CORRUPT) { decrypt(damaged) }
    }

    @Test
    fun `cambiar la fecha de la cabecera invalida el contenido`() {
        val file = encrypt(bytes(100))
        file[12] = (file[12] + 1).toByte() // dentro de createdAt
        assertFails(BackupError.WRONG_PASSWORD) { decrypt(file) }
    }

    @Test
    fun `un archivo cortado se detecta`() {
        val chunk = BackupCrypto.CHUNK_SIZE
        val file = encrypt(bytes(3 * chunk))
        val encryptedChunk = chunk + 16
        // Cortado justo al final del segundo bloque: los bloques presentes son válidos, pero falta el último.
        val atBoundary = file.copyOf(BackupCrypto.HEADER_SIZE + 2 * encryptedChunk)
        assertFails(BackupError.CORRUPT) { decrypt(atBoundary) }
        assertFails(BackupError.CORRUPT) { decrypt(file.copyOf(file.size - 5)) }
        // Solo la cabecera, sin ningún bloque.
        assertFails(BackupError.CORRUPT) { decrypt(file.copyOf(BackupCrypto.HEADER_SIZE)) }
    }

    @Test
    fun `los bloques no se pueden reordenar`() {
        val chunk = BackupCrypto.CHUNK_SIZE
        val file = encrypt(bytes(3 * chunk))
        val size = chunk + 16
        val start = BackupCrypto.HEADER_SIZE
        val swapped = file.copyOf()
        System.arraycopy(file, start + size, swapped, start, size)
        System.arraycopy(file, start, swapped, start + size, size)
        assertFails(BackupError.WRONG_PASSWORD) { decrypt(swapped) } // falla ya en el primer bloque
    }

    @Test
    fun `el calculo propio de PBKDF2 coincide con el del sistema`() {
        val salt = bytes(16)
        listOf("contraseña con ñ y 🙂", "a", "x".repeat(200)).forEach { text ->
            val expected = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(PBEKeySpec(text.toCharArray(), salt, 1500, 256)).encoded
            assertArrayEquals(expected, Pbkdf2.deriveManually(text.toCharArray(), salt, 1500, 32))
        }
        // Más de un bloque de salida.
        val long = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec("clave".toCharArray(), salt, 10, 80 * 8)).encoded
        assertArrayEquals(long, Pbkdf2.deriveManually("clave".toCharArray(), salt, 10, 80))
    }
}
