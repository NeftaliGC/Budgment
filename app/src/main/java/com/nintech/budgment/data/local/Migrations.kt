package com.nintech.budgment.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: la primera versión solo tenía usuarios, cuentas y categorías; la v2 agrega el resto
 * de las tablas (vacías). SQL tomado del esquema exportado `2.json`.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `budgets` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, "
                + "`name` TEXT NOT NULL, `amountLimit` INTEGER NOT NULL, `scope` TEXT NOT NULL, "
                + "`periodInterval` INTEGER NOT NULL, `periodUnit` TEXT NOT NULL, `startDate` INTEGER, "
                + "`endDate` INTEGER, `rollover` INTEGER NOT NULL, `notifyThresholdPercent` INTEGER, "
                + "`autoCreateRecurring` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER, "
                + "`deletedAt` INTEGER, PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_budgets_userId` ON `budgets` (`userId`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `exchange_rates` (`id` TEXT NOT NULL, `fromCurrency` TEXT NOT NULL, "
                + "`toCurrency` TEXT NOT NULL, `rateMicros` INTEGER NOT NULL, `date` INTEGER NOT NULL, "
                + "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_exchange_rates_fromCurrency_toCurrency_date` ON `exchange_rates` (`fromCurrency`, "
                + "`toCurrency`, `date`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `goals` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `accountId` TEXT, "
                + "`name` TEXT NOT NULL, `targetAmount` INTEGER NOT NULL, `currentAmount` INTEGER NOT NULL, "
                + "`targetDate` INTEGER, `icon` TEXT, `color` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER, "
                + "`deletedAt` INTEGER, PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , "
                + "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_goals_userId` ON `goals` (`userId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_goals_accountId` ON `goals` (`accountId`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recurring_transactions` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, "
                + "`accountId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `baseAmount` INTEGER NOT NULL, "
                + "`currency` TEXT, `periodInterval` INTEGER NOT NULL, `periodUnit` TEXT NOT NULL, "
                + "`anchorDate` INTEGER NOT NULL, `nextOccurrence` INTEGER NOT NULL, `endDate` INTEGER, "
                + "`active` INTEGER NOT NULL, `notifyBeforeDays` INTEGER, `createdAt` INTEGER NOT NULL, "
                + "`updatedAt` INTEGER, `deletedAt` INTEGER, PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , "
                + "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_recurring_transactions_userId` ON `recurring_transactions` (`userId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_recurring_transactions_accountId` ON `recurring_transactions` (`accountId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_recurring_transactions_nextOccurrence` ON `recurring_transactions` (`nextOccurrence`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tags` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `name` TEXT NOT NULL, "
                + "`color` TEXT, PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_userId_name` ON `tags` (`userId`, `name`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `transactions` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, "
                + "`accountId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `recurringId` TEXT, `amount` INTEGER NOT NULL, "
                + "`currency` TEXT, `exchangeRateMicros` INTEGER, `description` TEXT, `date` INTEGER NOT NULL, "
                + "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER, `deletedAt` INTEGER, PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , "
                + "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`recurringId`) REFERENCES `recurring_transactions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transactions_userId_date` ON `transactions` (`userId`, `date`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transactions_recurringId` ON `transactions` (`recurringId`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `transfers` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, "
                + "`fromAccountId` TEXT NOT NULL, `toAccountId` TEXT NOT NULL, `fromTransactionId` TEXT NOT NULL, "
                + "`toTransactionId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , "
                + "FOREIGN KEY(`fromAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`toAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`fromTransactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , "
                + "FOREIGN KEY(`toTransactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transfers_userId` ON `transfers` (`userId`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_transfers_fromTransactionId` ON `transfers` (`fromTransactionId`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_transfers_toTransactionId` ON `transfers` (`toTransactionId`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `attachments` (`id` TEXT NOT NULL, `transactionId` TEXT NOT NULL, "
                + "`filePath` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_attachments_transactionId` ON `attachments` (`transactionId`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `budget_categories` (`budgetId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, "
                + "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`budgetId`, `categoryId`), "
                + "FOREIGN KEY(`budgetId`) REFERENCES `budgets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , "
                + "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_budget_categories_categoryId` ON `budget_categories` (`categoryId`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `transaction_tags` (`transactionId` TEXT NOT NULL, `tagId` TEXT NOT NULL, "
                + "PRIMARY KEY(`transactionId`, `tagId`), "
                + "FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , "
                + "FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transaction_tags_tagId` ON `transaction_tags` (`tagId`)"
        )
    }
}

/**
 * v2 → v3: `transactions.categoryId` pasa a admitir null (las transferencias no tienen categoría).
 * SQLite no puede quitar un NOT NULL: se crea la tabla nueva, se copian los datos, se borra la
 * vieja y se renombra. Las tablas que apuntan a `transactions` siguen apuntando por nombre.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `transactions_new` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, "
                + "`accountId` TEXT NOT NULL, `categoryId` TEXT, `recurringId` TEXT, `amount` INTEGER NOT NULL, "
                + "`currency` TEXT, `exchangeRateMicros` INTEGER, `description` TEXT, `date` INTEGER NOT NULL, "
                + "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER, `deletedAt` INTEGER, PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , "
                + "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`recurringId`) REFERENCES `recurring_transactions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
        )
        db.execSQL(
            "INSERT INTO `transactions_new` (`id`, `userId`, `accountId`, `categoryId`, `recurringId`, `amount`, "
                + "`currency`, `exchangeRateMicros`, `description`, `date`, `createdAt`, `updatedAt`, "
                + "`deletedAt`) SELECT `id`, `userId`, `accountId`, `categoryId`, `recurringId`, `amount`, `currency`, "
                + "`exchangeRateMicros`, `description`, `date`, `createdAt`, `updatedAt`, "
                + "`deletedAt` FROM `transactions`"
        )
        db.execSQL("DROP TABLE `transactions`")
        db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transactions_userId_date` ON `transactions` (`userId`, `date`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transactions_recurringId` ON `transactions` (`recurringId`)"
        )
    }
}
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

/** v5 → v6: metas con moneda propia y aportes manuales. Agrega una columna y una tabla. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `goals` ADD COLUMN `currency` TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `goal_contributions` (`id` TEXT NOT NULL, `goalId` TEXT NOT NULL, " +
                "`amount` INTEGER NOT NULL, `note` TEXT, `date` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`), FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_contributions_goalId` ON `goal_contributions` (`goalId`)")
    }
}

/**
 * Cadena completa desde la primera versión: cualquier instalación, por vieja que sea, llega a la
 * actual sin perder datos. Cada cambio de esquema agrega aquí su migración.
 */
val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
