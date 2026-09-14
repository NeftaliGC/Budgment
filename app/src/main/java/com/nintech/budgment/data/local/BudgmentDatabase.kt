package com.nintech.budgment.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nintech.budgment.data.local.dao.AccountDao
import com.nintech.budgment.data.local.dao.CategoryDao
import com.nintech.budgment.data.local.dao.UserDao
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        AccountEntity::class,
        CategoryEntity::class
    ],
    version = 1,
    exportSchema = true
)

@TypeConverters(Converters::class)
abstract class BudgmentDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
}