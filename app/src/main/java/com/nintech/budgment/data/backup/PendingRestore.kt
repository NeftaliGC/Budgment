package com.nintech.budgment.data.backup

import android.content.Context
import android.util.Log
import com.nintech.budgment.data.local.PreMigrationBackup
import java.io.File
import java.io.IOException

/**
 * Restaurar un respaldo se hace en dos tiempos. Con la app abierta solo se deja la base
 * recuperada junto a la actual ([stage]); el cambio ocurre en el siguiente arranque
 * ([applyIfAny]), antes de que Room abra la base, cuando nadie la está usando.
 *
 * Si el proceso muere a medias, la base recuperada sigue ahí y se aplica al volver a abrir.
 */
object PendingRestore {

    private const val TAG = "PendingRestore"
    private const val SUFFIX = ".restore"

    private fun stagedFile(dbFile: File) = File(dbFile.path + SUFFIX)

    /** Deja [source] listo para reemplazar la base de datos en el próximo arranque. */
    fun stage(context: Context, databaseName: String, source: File) {
        val dbFile = context.getDatabasePath(databaseName)
        dbFile.parentFile?.mkdirs()
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
    fun applyIfAny(context: Context, databaseName: String): Boolean {
        val dbFile = context.getDatabasePath(databaseName)
        File(dbFile.path + "$SUFFIX.tmp").delete()
        val staged = stagedFile(dbFile)
        if (!staged.exists()) return false

        PreMigrationBackup.backupBeforeRestore(context, databaseName)
        // El -wal de la base anterior se borra antes de cambiar el archivo: si quedara, SQLite
        // intentaría aplicarlo sobre la base restaurada.
        listOf("-wal", "-shm", "-journal").forEach { File(dbFile.path + it).delete() }
        if (!staged.renameTo(dbFile)) {
            Log.e(TAG, "No se pudo aplicar la restauración")
            return false
        }
        Log.i(TAG, "Respaldo restaurado")
        return true
    }
}
