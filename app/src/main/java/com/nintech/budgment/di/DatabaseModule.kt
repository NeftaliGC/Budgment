package com.nintech.budgment.di

import android.content.Context
import androidx.room.Room
import com.nintech.budgment.data.backup.PendingRestore
import com.nintech.budgment.data.local.ALL_MIGRATIONS
import com.nintech.budgment.data.local.BudgmentDatabase
import com.nintech.budgment.data.local.DATABASE_NAME
import com.nintech.budgment.data.local.DATABASE_VERSION
import com.nintech.budgment.data.local.PreMigrationBackup
import com.nintech.budgment.data.local.dao.AccountDao
import com.nintech.budgment.data.local.dao.AttachmentDao
import com.nintech.budgment.data.local.dao.BudgetCategoryDao
import com.nintech.budgment.data.local.dao.BudgetDao
import com.nintech.budgment.data.local.dao.CategoryDao
import com.nintech.budgment.data.local.dao.ExchangeRateDao
import com.nintech.budgment.data.local.dao.GoalContributionDao
import com.nintech.budgment.data.local.dao.GoalDao
import com.nintech.budgment.data.local.dao.NotificationDao
import com.nintech.budgment.data.local.dao.RecurringTransactionDao
import com.nintech.budgment.data.local.dao.TagDao
import com.nintech.budgment.data.local.dao.TransactionDao
import com.nintech.budgment.data.local.dao.TransactionTagDao
import com.nintech.budgment.data.local.dao.TransferDao
import com.nintech.budgment.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideBudgmentDatabase(
        @ApplicationContext context: Context
    ): BudgmentDatabase {
        // Si el usuario restauró un respaldo, se aplica ahora que nadie tiene abierta la base.
        PendingRestore.applyIfAny(context, DATABASE_NAME)
        // Antes de migrar se guarda una copia de la base tal como está.
        PreMigrationBackup.backupIfUpgrading(context, DATABASE_NAME, DATABASE_VERSION)
        return Room.databaseBuilder(
            context,
            BudgmentDatabase::class.java,
            DATABASE_NAME
        )
            // Cada versión del esquema tiene su migración (data/local/Migrations.kt), desde la v1.
            // Sin fallback destructivo: si faltara una migración la app falla al abrir en vez de
            // borrar los datos, y la copia de seguridad previa queda intacta.
            .addMigrations(*ALL_MIGRATIONS)
            .build()
    }

    @Provides
    fun provideUserDao(database: BudgmentDatabase): UserDao =
        database.userDao()

    @Provides
    fun provideAccountDao(database: BudgmentDatabase): AccountDao =
        database.accountDao()

    @Provides
    fun provideCategoryDao(database: BudgmentDatabase): CategoryDao =
        database.categoryDao()

    @Provides
    fun provideTransactionDao(database: BudgmentDatabase): TransactionDao =
        database.transactionDao()

    @Provides
    fun provideRecurringTransactionDao(database: BudgmentDatabase): RecurringTransactionDao =
        database.recurringTransactionDao()

    @Provides
    fun provideTransferDao(database: BudgmentDatabase): TransferDao =
        database.transferDao()

    @Provides
    fun provideBudgetDao(database: BudgmentDatabase): BudgetDao =
        database.budgetDao()

    @Provides
    fun provideBudgetCategoryDao(database: BudgmentDatabase): BudgetCategoryDao =
        database.budgetCategoryDao()

    @Provides
    fun provideGoalDao(database: BudgmentDatabase): GoalDao =
        database.goalDao()

    @Provides
    fun provideTagDao(database: BudgmentDatabase): TagDao =
        database.tagDao()

    @Provides
    fun provideTransactionTagDao(database: BudgmentDatabase): TransactionTagDao =
        database.transactionTagDao()

    @Provides
    fun provideAttachmentDao(database: BudgmentDatabase): AttachmentDao =
        database.attachmentDao()

    @Provides
    fun provideExchangeRateDao(database: BudgmentDatabase): ExchangeRateDao =
        database.exchangeRateDao()

    @Provides
    fun provideGoalContributionDao(database: BudgmentDatabase): GoalContributionDao =
        database.goalContributionDao()

    @Provides
    fun provideNotificationDao(database: BudgmentDatabase): NotificationDao =
        database.notificationDao()
}