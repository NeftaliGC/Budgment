package com.nintech.budgment.data.backup

import android.content.Context
import android.util.Log
import com.nintech.budgment.data.attachments.ATTACHMENTS_DIR
import com.nintech.budgment.data.local.PreMigrationBackup
import java.io.File
import java.io.IOException

/**
 * Restaurar un respaldo se hace en dos tiempos. Con la app abierta solo se deja la base
 * recuperada junto a la actual ([stage]); el cambio ocurre en el siguiente arranque
 * ([applyIfAny]), antes de que Room abra la base, cuando nadie la está usando.
 *
 * Si el proceso muere a medias, la base recuperada sigue ahí y se aplica al volver a abrir.
 *
 * Las fotos de recibos viajan con la base: siempre se reemplazan por las del respaldo (ninguna,
 * si no las traía), porque las anteriores pertenecen a movimientos que dejan de existir.
 */
object PendingRestore {

    private const val TAG = "PendingRestore"
    private const val SUFFIX = ".restore"

    private fun stagedFile(dbFile: File) = File(dbFile.path + SUFFIX)

    /**
     * Deja [source] listo para reemplazar la base de datos en el próximo arranque, junto con las
     * fotos de [attachments] (una carpeta; null si el respaldo no trae fotos). [attachmentsDirName]
     * es la carpeta de fotos de la app, dentro de `filesDir`; solo las pruebas usan otra.
     */
    fun stage(
        context: Context, databaseName: String, source: File, attachments: File? = null,
        attachmentsDirName: String = ATTACHMENTS_DIR
    ) {
        val dbFile = context.getDatabasePath(databaseName)
        dbFile.parentFile?.mkdirs()
        // Las fotos antes que la base: el archivo de la base es la señal de que todo está listo.
        // La carpeta se crea siempre, aunque quede vacía (ver applyIfAny).
        val stagedAttachments = File(context.filesDir, attachmentsDirName + SUFFIX)
        stagedAttachments.deleteRecursively()
        stagedAttachments.mkdirs()
        attachments?.listFiles()?.forEach { it.copyTo(File(stagedAttachments, it.name), overwrite = true) }
        // Primero a un temporal en la misma carpeta: el renombrado final es atómico, así nunca
        // queda un archivo a medias con el nombre que el arranque busca.
        val temp = File(dbFile.path + "$SUFFIX.tmp")
        source.copyTo(temp, overwrite = true)
        if (!temp.renameTo(stagedFile(dbFile))) {
            temp.delete()
            throw IOException("No se pudo preparar la restauración")
        }
    }

    /**
     * Si hay una restauración preparada, guarda una copia de la base actual y la reemplaza.
     * Debe llamarse antes de abrir la base de datos. Devuelve true si se restauró.
     */
    fun applyIfAny(context: Context, databaseName: String, attachmentsDirName: String = ATTACHMENTS_DIR): Boolean {
        val dbFile = context.getDatabasePath(databaseName)
        File(dbFile.path + "$SUFFIX.tmp").delete()
        val staged = stagedFile(dbFile)
        val stagedAttachments = File(context.filesDir, attachmentsDirName + SUFFIX)
        if (!staged.exists()) {
            // Fotos preparadas sin base: la preparación se interrumpió y no hay nada que aplicar.
            stagedAttachments.deleteRecursively()
            return false
        }

        PreMigrationBackup.backupBeforeRestore(context, databaseName)
        // El -wal de la base anterior se borra antes de cambiar el archivo: si quedara, SQLite
        // intentaría aplicarlo sobre la base restaurada.
        listOf("-wal", "-shm", "-journal").forEach { File(dbFile.path + it).delete() }
        // Si la carpeta preparada ya no está, es que este paso se completó en un arranque
        // anterior que se interrumpió justo después: no hay que repetirlo.
        if (stagedAttachments.exists()) {
            val attachments = File(context.filesDir, attachmentsDirName)
            attachments.deleteRecursively()
            if (!stagedAttachments.renameTo(attachments)) {
                Log.e(TAG, "No se pudieron restaurar las fotos")
                stagedAttachments.deleteRecursively()
            }
        }
        if (!staged.renameTo(dbFile)) {
            Log.e(TAG, "No se pudo aplicar la restauración")
            return false
        }
        Log.i(TAG, "Respaldo restaurado")
        return true
    }
}
