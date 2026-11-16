package com.nintech.budgment.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
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
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.exitProcess

/** Un respaldo ya descifrado y comprobado, listo para reemplazar los datos actuales. */
data class RestorePreview(
    val createdAt: Long,
    val accounts: Int,
    val transactions: Int
)

/**
 * Respaldo a un archivo que el usuario guarda donde quiera (teléfono, Drive, USB…) y su
 * restauración. El archivo es la base de datos completa, comprimida y cifrada con una contraseña
 * ([BackupCrypto]); incluye a todos los usuarios registrados en el teléfono.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) {
    // Propio y no el de la pantalla: aplicar una restauración cierra la sesión, y eso destruye
    // la pantalla que la pidió antes de terminar.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val workDir: File get() = File(context.cacheDir, "backup").apply { mkdirs() }
    private val restoredDb: File get() = File(workDir, "restore.db")

    /** Escribe el respaldo en [target]. Si falla, borra el archivo a medias y lanza [IOException]. */
    suspend fun export(target: Uri, password: CharArray) = withContext(Dispatchers.IO) {
        val snapshot = File(workDir, "snapshot.db")
        try {
            val schemaVersion = DatabaseSnapshot.create(context.getDatabasePath(DATABASE_NAME), snapshot)
            val output = context.contentResolver.openOutputStream(target)
                ?: throw FileNotFoundException("No se pudo abrir el destino")
            val encrypted = BackupCrypto.encrypt(output, password, System.currentTimeMillis(), schemaVersion)
            GZIPOutputStream(encrypted).use { zip ->
                snapshot.inputStream().use { it.copyTo(zip) }
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
                    GZIPInputStream(plain).use { zip ->
                        restoredDb.outputStream().use { zip.copyTo(it) }
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
            val info = DatabaseSnapshot.inspect(restoredDb, DATABASE_VERSION)
            RestorePreview(header.createdAt, info.accounts, info.transactions)
        } catch (e: Exception) {
            discardRestore()
            throw e
        }
    }

    /** Borra el respaldo descifrado si el usuario no siguió adelante. */
    fun discardRestore() {
        restoredDb.delete()
        File(restoredDb.path + "-journal").delete()
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
                PendingRestore.stage(context, DATABASE_NAME, restoredDb)
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
    }
}
