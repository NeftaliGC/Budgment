package com.nintech.budgment.data.backup

import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import java.io.File

/** Lo que contiene una base de datos recuperada de un respaldo. */
data class SnapshotInfo(
    val schemaVersion: Int,
    val accounts: Int,
    val transactions: Int
)

object DatabaseSnapshot {

    private val REQUIRED_TABLES = listOf("room_master_table", "users", "accounts", "transactions")

    /**
     * Copia la base de datos en uso a [target] como un solo archivo coherente, aunque la app la
     * tenga abierta y con cambios aún en el -wal. Devuelve la versión del esquema.
     *
     * Abre una base nueva, le adjunta la que está en uso y copia tablas e índices dentro de una
     * transacción: lo copiado corresponde a un mismo instante. Mientras dura, otras escrituras
     * esperan; son milisegundos con el tamaño de datos de la app.
     */
    fun create(liveDb: File, target: File): Int {
        target.delete()
        val db = SQLiteDatabase.openOrCreateDatabase(target, null)
        try {
            db.execSQL("ATTACH DATABASE '${liveDb.path.replace("'", "''")}' AS live")
            val version: Int
            db.beginTransaction()
            try {
                // android_metadata ya la creó el sistema al abrir la base nueva.
                val objects = db.rawQuery(
                    "SELECT type, name, sql FROM live.sqlite_master " +
                        "WHERE sql IS NOT NULL AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata'",
                    null
                ).use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            add(Triple(cursor.getString(0), cursor.getString(1), cursor.getString(2)))
                        }
                    }
                }
                val (tables, others) = objects.partition { it.first == "table" }
                tables.forEach { (_, _, sql) -> db.execSQL(sql) }
                tables.forEach { (_, name, _) -> db.execSQL("INSERT INTO main.`$name` SELECT * FROM live.`$name`") }
                // Los índices al final: es más rápido crearlos con los datos ya copiados.
                others.forEach { (_, _, sql) -> db.execSQL(sql) }
                version = DatabaseUtils.longForQuery(db, "PRAGMA live.user_version", null).toInt()
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            db.execSQL("DETACH DATABASE live")
            db.version = version
            return version
        } finally {
            db.close()
        }
    }

    /**
     * Comprueba que [file] sea una base de datos de Budgment íntegra y cuenta lo que trae.
     * Lanza [BackupException] si no lo es.
     */
    fun inspect(file: File, maxSchemaVersion: Int): SnapshotInfo {
        try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val version = db.version
                if (version > maxSchemaVersion) throw BackupException(BackupError.NEWER_VERSION)
                if (version < 1) throw BackupException(BackupError.CORRUPT)
                if (DatabaseUtils.stringForQuery(db, "PRAGMA integrity_check", null) != "ok") {
                    throw BackupException(BackupError.CORRUPT)
                }
                REQUIRED_TABLES.forEach { table ->
                    val found = DatabaseUtils.longForQuery(
                        db, "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(table)
                    )
                    if (found == 0L) throw BackupException(BackupError.CORRUPT)
                }
                return SnapshotInfo(
                    schemaVersion = version,
                    accounts = count(db, "accounts"),
                    transactions = count(db, "transactions")
                )
            }
        } catch (e: SQLiteException) {
            throw BackupException(BackupError.CORRUPT, e)
        }
    }

    private fun count(db: SQLiteDatabase, table: String): Int =
        DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM `$table` WHERE deletedAt IS NULL", null).toInt()
}
