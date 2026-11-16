package com.nintech.budgment.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.nintech.budgment.data.attachments.AttachmentStore
import com.nintech.budgment.data.local.DATABASE_NAME
import com.nintech.budgment.data.local.DATABASE_VERSION
import com.nintech.budgment.data.session.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.exitProcess

/** Un respaldo ya descifrado y comprobado, listo para reemplazar los datos actuales. */
data class RestorePreview(
    val createdAt: Long,
    val accounts: Int,
    val transactions: Int,
    val photos: Int
)

/**
 * Respaldo a un archivo que el usuario guarda donde quiera (teléfono, Drive, USB…) y su
 * restauración. El archivo lleva la base de datos completa y, si el usuario quiere, las fotos de
 * recibos; va comprimido y cifrado con una contraseña ([BackupCrypto]). Incluye a todos los
 * usuarios registrados en el teléfono.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val attachmentStore: AttachmentStore
) {
    // Propio y no el de la pantalla: aplicar una restauración cierra la sesión, y eso destruye
    // la pantalla que la pidió antes de terminar.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val workDir: File get() = File(context.cacheDir, "backup").apply { mkdirs() }
    private val restoredDb: File get() = File(workDir, "restore.db")
    private val restoredPhotos: File get() = File(workDir, "restore-photos")

    /**
     * Escribe el respaldo en [target], con o sin las fotos de recibos. Si falla, borra el archivo
     * a medias y lanza [IOException].
     */
    suspend fun export(target: Uri, password: CharArray, includePhotos: Boolean) = withContext(Dispatchers.IO) {
        val snapshot = File(workDir, "snapshot.db")
        try {
            val schemaVersion = DatabaseSnapshot.create(context.getDatabasePath(DATABASE_NAME), snapshot)
            val output = context.contentResolver.openOutputStream(target)
                ?: throw FileNotFoundException("No se pudo abrir el destino")
            val encrypted = BackupCrypto.encrypt(output, password, System.currentTimeMillis(), schemaVersion)
            ZipOutputStream(encrypted).use { zip ->
                zip.putNextEntry(ZipEntry(ENTRY_DATABASE))
                snapshot.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                if (includePhotos) {
                    // Un JPEG ya está comprimido: se guarda tal cual en vez de gastar tiempo en él.
                    zip.setLevel(Deflater.NO_COMPRESSION)
                    attachmentStore.storedFiles().filter { PHOTO_NAME.matches(it.name) }.forEach { photo ->
                        zip.putNextEntry(ZipEntry(ENTRY_PHOTOS + photo.name))
                        photo.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        } catch (e: Exception) {
            discardTarget(target)
            throw e as? IOException ?: IOException(e)
        } finally {
            snapshot.delete()
            File(snapshot.path + "-journal").delete()
        }
    }

    /** Borra un archivo de respaldo que quedó vacío o a medias. */
    fun discardTarget(target: Uri) {
        runCatching { DocumentsContract.deleteDocument(context.contentResolver, target) }
    }

    /** Fecha y versión de un respaldo, sin necesidad de la contraseña. Lanza [BackupException]. */
    suspend fun readHeader(source: Uri): BackupHeader = withContext(Dispatchers.IO) {
        val header = open(source).use { BackupCrypto.readHeader(it) }
        if (header.schemaVersion > DATABASE_VERSION) throw BackupException(BackupError.NEWER_VERSION)
        header
    }

    /**
     * Descifra [source] a un archivo temporal y comprueba que sea una base de datos válida.
     * No cambia nada todavía: eso lo hace [applyRestoreAndRestart]. Lanza [BackupException] si la
     * contraseña o el archivo no sirven, o [IOException] si no se pudo leer.
     */
    suspend fun prepareRestore(source: Uri, password: CharArray): RestorePreview = withContext(Dispatchers.IO) {
        discardRestore()
        try {
            val input = open(source)
            val (header, decrypted) = try {
                BackupCrypto.decrypt(input, password)
            } catch (e: Exception) {
                input.close()
                throw e
            }
            decrypted.use { plain ->
                try {
                    if (header.formatVersion == 1) {
                        GZIPInputStream(plain).use { zip ->
                            restoredDb.outputStream().use { zip.copyTo(it) }
                        }
                    } else {
                        unzip(plain)
                    }
                } catch (e: ZipException) {
                    throw BackupException(BackupError.CORRUPT, e)
                } catch (e: EOFException) {
                    throw BackupException(BackupError.CORRUPT, e)
                }
                // Hasta el final, para comprobar que el archivo no esté cortado.
                val rest = ByteArray(8192)
                while (plain.read(rest) != -1) Unit
            }
            if (!restoredDb.exists()) throw BackupException(BackupError.CORRUPT)
            val info = DatabaseSnapshot.inspect(restoredDb, DATABASE_VERSION)
            RestorePreview(header.createdAt, info.accounts, info.transactions, restoredPhotos.list()?.size ?: 0)
        } catch (e: Exception) {
            discardRestore()
            throw e
        }
    }

    /** Saca la base de datos y las fotos del ZIP. No se cierra [input]: lo hace quien llama. */
    private fun unzip(input: InputStream) {
        restoredPhotos.mkdirs()
        val zip = ZipInputStream(input)
        while (true) {
            val entry = zip.nextEntry ?: break
            val name = entry.name
            when {
                name == ENTRY_DATABASE -> restoredDb.outputStream().use { zip.copyTo(it) }
                // El nombre se valida entero: un archivo manipulado no puede escribir en otra carpeta.
                name.startsWith(ENTRY_PHOTOS) && PHOTO_NAME.matches(name.removePrefix(ENTRY_PHOTOS)) ->
                    File(restoredPhotos, name.removePrefix(ENTRY_PHOTOS)).outputStream().use { zip.copyTo(it) }
            }
        }
    }

    /** Borra el respaldo descifrado si el usuario no siguió adelante. */
    fun discardRestore() {
        restoredDb.delete()
        File(restoredDb.path + "-journal").delete()
        restoredPhotos.deleteRecursively()
    }

    /**
     * Reemplaza los datos por el respaldo preparado con [prepareRestore] y reinicia la app.
     *
     * Primero cierra la sesión: el usuario actual puede no existir en el respaldo. El cambio de
     * base de datos ocurre al arrancar de nuevo ([PendingRestore]).
     */
    fun applyRestoreAndRestart() {
        scope.launch {
            try {
                check(restoredDb.exists()) { "No hay un respaldo preparado" }
                sessionManager.logout()
                PendingRestore.stage(context, DATABASE_NAME, restoredDb, restoredPhotos.takeIf { it.isDirectory })
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo restaurar el respaldo", e)
            } finally {
                discardRestore()
            }
            // También si falló: la sesión ya se cerró y la app vuelve a un estado limpio.
            restart()
        }
    }

    private fun restart() {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        launch?.component?.let { context.startActivity(Intent.makeRestartActivityTask(it)) }
        exitProcess(0)
    }

    private fun open(source: Uri) =
        context.contentResolver.openInputStream(source) ?: throw FileNotFoundException("No se pudo abrir el archivo")

    private companion object {
        const val TAG = "BackupManager"
        const val ENTRY_DATABASE = "budgment.db"
        const val ENTRY_PHOTOS = "attachments/"
        val PHOTO_NAME = Regex("[A-Za-z0-9-]{1,64}\\.jpg")
    }
}
