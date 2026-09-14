package com.nintech.budgment.data.local

import androidx.room.TypeConverter
import com.nintech.budgment.data.model.AccountType
import com.nintech.budgment.data.model.CategoryType

class Converters {
    @TypeConverter
    fun fromAccountType(value: AccountType): String = value.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    @TypeConverter
    fun fromCategoryType(value: CategoryType): String = value.name

    @TypeConverter
    fun toCategoryType(value: String): CategoryType = CategoryType.valueOf(value)
}