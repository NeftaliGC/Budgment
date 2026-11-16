package com.nintech.budgment.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.local.entity.AttachmentEntity
import com.nintech.budgment.data.local.entity.BudgetCategoryEntity
import com.nintech.budgment.data.local.entity.BudgetEntity
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.local.entity.ExchangeRateEntity
import com.nintech.budgment.data.local.entity.GoalContributionEntity
import com.nintech.budgment.data.local.entity.GoalEntity
import com.nintech.budgment.data.local.entity.NotificationEntity
import com.nintech.budgment.data.local.entity.RecurringTransactionEntity
import com.nintech.budgment.data.local.entity.TagEntity
import com.nintech.budgment.data.local.entity.TransactionEntity
import com.nintech.budgment.data.local.entity.TransactionTagEntity
import com.nintech.budgment.data.local.entity.TransferEntity
import com.nintech.budgment.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        RecurringTransactionEntity::class,
        TransferEntity::class,
        BudgetEntity::class,
        BudgetCategoryEntity::class,
        GoalEntity::class,
        TagEntity::class,
        TransactionTagEntity::class,
        AttachmentEntity::class,
        ExchangeRateEntity::class,
        NotificationEntity::class,
        GoalContributionEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class BudgmentDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun transferDao(): TransferDao
    abstract fun budgetDao(): BudgetDao
    abstract fun budgetCategoryDao(): BudgetCategoryDao
    abstract fun goalDao(): GoalDao
    abstract fun tagDao(): TagDao
    abstract fun transactionTagDao(): TransactionTagDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun notificationDao(): NotificationDao
    abstract fun goalContributionDao(): GoalContributionDao
}