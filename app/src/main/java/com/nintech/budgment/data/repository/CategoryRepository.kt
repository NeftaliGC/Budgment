package com.nintech.budgment.data.repository

import com.nintech.budgment.data.local.dao.CategoryDao
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.model.CategoryType
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

interface CategoryRepository {
    fun observeCategories(userId: String): Flow<List<CategoryEntity>>
    suspend fun getCategory(id: String): CategoryEntity?
    suspend fun createCategory(userId: String, name: String, type: CategoryType)
    suspend fun updateCategory(category: CategoryEntity, name: String, type: CategoryType)
    suspend fun deleteCategory(id: String)
}

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun observeCategories(userId: String): Flow<List<CategoryEntity>> =
        categoryDao.observeCategories(userId)

    override suspend fun getCategory(id: String): CategoryEntity? =
        categoryDao.getById(id)

    override suspend fun createCategory(userId: String, name: String, type: CategoryType) {
        categoryDao.insert(
            CategoryEntity(id = UUID.randomUUID().toString(), userId = userId, name = name, type = type)
        )
    }

    override suspend fun updateCategory(category: CategoryEntity, name: String, type: CategoryType) {
        categoryDao.update(category.copy(name = name, type = type, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteCategory(id: String) {
        categoryDao.softDelete(id)
    }
}