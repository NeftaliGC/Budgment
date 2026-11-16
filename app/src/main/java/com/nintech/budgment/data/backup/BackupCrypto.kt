package com.nintech.budgment.data.backup

import com.nintech.budgment.data.local.security.Pbkdf2
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Por qué no se pudo leer un respaldo. */
enum class BackupError {
    /** El archivo no es un respaldo de Budgment. */
    NOT_A_BACKUP,
    /** Lo creó una versión más reciente de la app. */
    NEWER_VERSION,
    WRONG_PASSWORD,
    CORRUPT
}

class BackupException(val error: BackupError, cause: Throwable? = null) : IOException(error.name, cause)

/** Datos sin cifrar al inicio del archivo. Quedan autenticados: alterarlos invalida el contenido. */
class BackupHeader(
    val formatVersion: Int,
    val createdAt: Long,
    /** Versión del esquema de la base de datos guardada. */
    val schemaVersion: Int,
    val iterations: Int,
    val salt: ByteArray,
    val noncePrefix: ByteArray
) {
    fun encode(): ByteArray {
        val bytes = ByteArrayOutputStream(BackupCrypto.HEADER_SIZE)
        DataOutputStream(bytes).apply {
            write(BackupCrypto.MAGIC)
            writeByte(formatVersion)
            writeLong(createdAt)
            writeInt(schemaVersion)
            writeInt(iterations)
            write(salt)
            write(noncePrefix)
        }
        return bytes.toByteArray()
    }
}

/**
 * Formato del archivo de respaldo: una cabecera seguida del contenido cifrado con AES-256-GCM.
 * La clave sale de la contraseña con PBKDF2-HMAC-SHA256 y una sal nueva en cada archivo.
 *
 * El contenido va en bloques de [CHUNK_SIZE] para no tener todo en memoria. Cada bloque lleva
 * su propia etiqueta de autenticación; su número y si es el último forman parte del nonce, así
 * que no se pueden reordenar, quitar ni cortar el archivo sin que se note.
 */
object BackupCrypto {
    /**
     * 1: el contenido es la base de datos comprimida con gzip.
     * 2: el contenido es un ZIP con la base de datos y, si se incluyeron, las fotos de recibos.
     */
    const val FORMAT_VERSION = 2
    const val DEFAULT_ITERATIONS = 600_000
    const val MIN_PASSWORD_LENGTH = 8

    internal val MAGIC = "BDGMBKUP".toByteArray(Charsets.US_ASCII)
    private const val SALT_SIZE = 16
    private const val NONCE_PREFIX_SIZE = 7
    internal const val HEADER_SIZE = 8 + 1 + 8 + 4 + 4 + SALT_SIZE + NONCE_PREFIX_SIZE
    internal const val CHUNK_SIZE = 64 * 1024
    private const val TAG_SIZE = 16
    private const val MIN_ITERATIONS = 10_000
    // Un archivo manipulado no debe poder dejar el teléfono calculando durante horas.
    private const val MAX_ITERATIONS = 5_000_000

    /** Lee y valida la cabecera. No necesita la contraseña. */
    fun readHeader(input: InputStream): BackupHeader {
        val data = DataInputStream(input)
        try {
            val magic = ByteArray(MAGIC.size).also { data.readFully(it) }
            if (!magic.contentEquals(MAGIC)) throw BackupException(BackupError.NOT_A_BACKUP)
            val formatVersion = data.readUnsignedByte()
            if (formatVersion > FORMAT_VERSION) throw BackupException(BackupError.NEWER_VERSION)
            if (formatVersion < 1) throw BackupException(BackupError.CORRUPT)
            val createdAt = data.readLong()
            val schemaVersion = data.readInt()
            val iterations = data.readInt()
            if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS) throw BackupException(BackupError.CORRUPT)
            val salt = ByteArray(SALT_SIZE).also { data.readFully(it) }
            val noncePrefix = ByteArray(NONCE_PREFIX_SIZE).also { data.readFully(it) }
            return BackupHeader(formatVersion, createdAt, schemaVersion, iterations, salt, noncePrefix)
        } catch (e: EOFException) {
            throw BackupException(BackupError.NOT_A_BACKUP, e)
        }
    }

    /**
     * Escribe la cabecera en [output] y devuelve un flujo que cifra lo que se le escriba.
     * Hay que cerrarlo para que el archivo quede completo.
     */
    fun encrypt(
        output: OutputStream,
        password: CharArray,
        createdAt: Long,
        schemaVersion: Int,
        iterations: Int = DEFAULT_ITERATIONS,
        random: SecureRandom = SecureRandom(),
        formatVersion: Int = FORMAT_VERSION
    ): OutputStream {
        val header = BackupHeader(
            formatVersion = formatVersion,
            createdAt = createdAt,
            schemaVersion = schemaVersion,
            iterations = iterations,
            salt = ByteArray(SALT_SIZE).also(random::nextBytes),
            noncePrefix = ByteArray(NONCE_PREFIX_SIZE).also(random::nextBytes)
        )
        val headerBytes = header.encode()
        val key = deriveKey(password, header)
        output.write(headerBytes)
        return EncryptingStream(output, ChunkCipher(key, header.noncePrefix, headerBytes))
    }

    /** Lee la cabecera de [input] y devuelve un flujo con el contenido descifrado. */
    fun decrypt(input: InputStream, password: CharArray): Pair<BackupHeader, InputStream> {
        val header = readHeader(input)
        val key = deriveKey(password, header)
        return header to DecryptingStream(input, ChunkCipher(key, header.noncePrefix, header.encode()))
    }

    private fun deriveKey(password: CharArray, header: BackupHeader): SecretKeySpec =
        SecretKeySpec(Pbkdf2.derive(password, header.salt, header.iterations), "AES")

    private class ChunkCipher(
        private val key: SecretKeySpec,
        private val noncePrefix: ByteArray,
        private val headerBytes: ByteArray
    ) {
        private val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        fun process(mode: Int, index: Long, last: Boolean, data: ByteArray, length: Int): ByteArray {
            if (index > 0xFFFFFFFFL) throw IOException("Respaldo demasiado grande")
            val nonce = ByteArray(12)
            noncePrefix.copyInto(nonce)
            nonce[7] = (index ushr 24).toByte()
            nonce[8] = (index ushr 16).toByte()
            nonce[9] = (index ushr 8).toByte()
            nonce[10] = index.toByte()
            nonce[11] = if (last) 1 else 0
            cipher.init(mode, key, GCMParameterSpec(TAG_SIZE * 8, nonce))
            cipher.updateAAD(headerBytes)
            return cipher.doFinal(data, 0, length)
        }
    }

    private class EncryptingStream(
        private val output: OutputStream,
        private val cipher: ChunkCipher
    ) : OutputStream() {
        private val buffer = ByteArray(CHUNK_SIZE)
        private var filled = 0
        private var index = 0L
        private var closed = false

        override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)

        override fun write(b: ByteArray, off: Int, len: Int) {
            if (closed) throw IOException("Flujo cerrado")
            var offset = off
            var remaining = len
            while (remaining > 0) {
                // Un bloque lleno solo se emite cuando llega algo más: el último lo marca close().
                if (filled == CHUNK_SIZE) emit(last = false)
                val n = minOf(remaining, CHUNK_SIZE - filled)
                System.arraycopy(b, offset, buffer, filled, n)
                filled += n
                offset += n
                remaining -= n
            }
        }

        private fun emit(last: Boolean) {
            output.write(cipher.process(Cipher.ENCRYPT_MODE, index++, last, buffer, filled))
            filled = 0
        }

        override fun flush() = output.flush()

        override fun close() {
            if (closed) return
            closed = true
            output.use {
                emit(last = true)
                it.flush()
            }
        }
    }

    private class DecryptingStream(
        private val input: InputStream,
        private val cipher: ChunkCipher
    ) : InputStream() {
        private val segment = ByteArray(CHUNK_SIZE + TAG_SIZE)
        /** Primer byte del siguiente bloque, ya leído para saber si el actual era el último. */
        private var lookahead = -1
        private var plain = ByteArray(0)
        private var position = 0
        private var index = 0L
        private var finished = false

        override fun read(): Int {
            val one = ByteArray(1)
            return if (read(one, 0, 1) == -1) -1 else one[0].toInt() and 0xFF
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (len == 0) return 0
            while (position == plain.size) {
                if (finished) return -1
                nextChunk()
            }
            val n = minOf(len, plain.size - position)
            System.arraycopy(plain, position, b, off, n)
            position += n
            return n
        }

        private fun nextChunk() {
            var filled = 0
            if (lookahead >= 0) {
                segment[0] = lookahead.toByte()
                filled = 1
                lookahead = -1
            }
            while (filled < segment.size) {
                val n = input.read(segment, filled, segment.size - filled)
                if (n == -1) break
                filled += n
            }
            val last = filled < segment.size || input.read().also { lookahead = it } == -1
            if (filled < TAG_SIZE) throw BackupException(BackupError.CORRUPT)
            plain = try {
                cipher.process(Cipher.DECRYPT_MODE, index, last, segment, filled)
            } catch (e: GeneralSecurityException) {
                // Con otra contraseña la clave es otra y ya el primer bloque no se autentica.
                throw BackupException(if (index == 0L) BackupError.WRONG_PASSWORD else BackupError.CORRUPT, e)
            }
            position = 0
            index++
            finished = last
        }

        override fun close() = input.close()
    }
}
