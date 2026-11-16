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

/** v4 → v5: bandeja de notificaciones. Solo crea una tabla nueva. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `notifications` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, " +
                "`type` TEXT NOT NULL, `refId` TEXT, `occurrence` INTEGER, `dedupKey` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, `body` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `readAt` INTEGER, " +
                "PRIMARY KEY(`id`), FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_userId` ON `notifications` (`userId`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_notifications_userId_dedupKey` " +
                "ON `notifications` (`userId`, `dedupKey`)"
        )
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_3_4, MIGRATION_4_5)
