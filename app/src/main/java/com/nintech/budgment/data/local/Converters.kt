package com.nintech.budgment.data.local

import androidx.room.TypeConverter
import com.nintech.budgment.data.model.AccountType
import com.nintech.budgment.data.model.BudgetScope
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.data.model.PeriodUnit

class Converters {
    @TypeConverter
    fun fromAccountType(value: AccountType): String = value.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    @TypeConverter
    fun fromCategoryType(value: CategoryType): String = value.name

    @TypeConverter
    fun toCategoryType(value: String): CategoryType = CategoryType.valueOf(value)

    @TypeConverter
    fun fromPeriodUnit(value: PeriodUnit): String = value.name

    @TypeConverter
    fun toPeriodUnit(value: String): PeriodUnit = PeriodUnit.valueOf(value)

    @TypeConverter
    fun fromBudgetScope(value: BudgetScope): String = value.name

    @TypeConverter
    fun toBudgetScope(value: String): BudgetScope = BudgetScope.valueOf(value)
}