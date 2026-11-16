package com.nintech.budgment.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File

/**
 * Antes de que Room actualice el esquema, copia la base de datos tal como está. Si una migración
 * resultara incorrecta (aunque no falle), los datos originales siguen en la copia y se pueden
 * recuperar en una versión posterior de la app.
 *
 * Las copias van a `noBackupFilesDir/db-backups`, fuera del respaldo automático de Android, y se
 * conservan las [KEEP] más recientes.
 */
object PreMigrationBackup {

    private const val TAG = "PreMigrationBackup"
    private const val DIR = "db-backups"
    private const val KEEP = 3

    /**
     * Copia [databaseName] si su versión es menor que [targetVersion]. No hace nada en una
     * instalación nueva ni si la base ya está al día. Nunca lanza: si la copia falla, la app sigue.
     */
    fun backupIfUpgrading(context: Context, databaseName: String, targetVersion: Int) {
        val dbFile = context.getDatabasePath(databaseName)
        if (!dbFile.exists()) return
        try {
            val currentVersion = readVersion(dbFile) ?: return
            if (currentVersion == 0 || currentVersion >= targetVersion) return

            val dir = File(context.noBackupFilesDir, DIR).apply { mkdirs() }
            val base = "${dbFile.nameWithoutExtension}-v$currentVersion-${System.currentTimeMillis()}"
            // Con WAL, los cambios recientes pueden estar en -wal: se copian los tres archivos juntos.
            listOf("", "-wal", "-shm").forEach { suffix ->
                val source = File(dbFile.path + suffix)
                if (source.exists()) source.copyTo(File(dir, "$base.db$suffix"), overwrite = true)
            }
            pruneOldBackups(dir)
            Log.i(TAG, "Copia antes de migrar v$currentVersion → v$targetVersion: $base.db")
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo copiar la base antes de migrar", e)
        }
    }

    /** Lee `PRAGMA user_version` sin modificar el archivo. */
    private fun readVersion(dbFile: File): Int? =
        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }

    private fun pruneOldBackups(dir: File) {
        dir.listFiles { file -> file.name.endsWith(".db") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(KEEP)
            ?.forEach { old ->
                listOf("", "-wal", "-shm").forEach { suffix -> File(old.path + suffix).delete() }
            }
    }
}
