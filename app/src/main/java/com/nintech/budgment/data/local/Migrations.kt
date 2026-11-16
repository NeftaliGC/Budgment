package com.nintech.budgment.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v3 → v4: recurrentes con nombre y «más tarde», e índices de llaves foráneas que faltaban.
 * Solo agrega columnas e índices: conserva todos los datos.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `recurring_transactions` ADD COLUMN `description` TEXT")
        db.execSQL("ALTER TABLE `recurring_transactions` ADD COLUMN `snoozedUntil` INTEGER")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_recurring_transactions_categoryId` " +
                "ON `recurring_transactions` (`categoryId`)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transfers_fromAccountId` ON `transfers` (`fromAccountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transfers_toAccountId` ON `transfers` (`toAccountId`)")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_3_4)
