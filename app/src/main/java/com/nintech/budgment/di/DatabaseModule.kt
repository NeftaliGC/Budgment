package com.nintech.budgment.di

import android.content.Context
import androidx.room.Room
import com.nintech.budgment.data.local.ALL_MIGRATIONS
import com.nintech.budgment.data.local.BudgmentDatabase
import com.nintech.budgment.data.local.dao.AccountDao
import com.nintech.budgment.data.local.dao.AttachmentDao
import com.nintech.budgment.data.local.dao.BudgetCategoryDao
import com.nintech.budgment.data.local.dao.BudgetDao
import com.nintech.budgment.data.local.dao.CategoryDao
import com.nintech.budgment.data.local.dao.ExchangeRateDao
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

private const val DATABASE_NAME = "budgment.db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideBudgmentDatabase(
        @ApplicationContext context: Context
    ): BudgmentDatabase =
        Room.databaseBuilder(
            context,
            BudgmentDatabase::class.java,
            DATABASE_NAME
        )
            // Desde v3 cada cambio de esquema lleva su migración (data/local/Migrations.kt).
            .addMigrations(*ALL_MIGRATIONS)
            // Solo mientras desarrollamos: versiones anteriores a v3 no tienen migración y
            // se recrean desde cero. QUITAR antes de publicar la app.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

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
    fun provideNotificationDao(database: BudgmentDatabase): NotificationDao =
        database.notificationDao()
}