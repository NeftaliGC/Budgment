package com.nintech.budgment.data.repository

import com.nintech.budgment.data.local.dao.UserDao
import com.nintech.budgment.data.local.entity.UserEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUser(userId: String): UserEntity?
    fun observeUser(userId: String): Flow<UserEntity?>
    suspend fun setDefaultCurrency(userId: String, currencyCode: String)
}

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {
    override suspend fun getUser(userId: String): UserEntity? = userDao.getById(userId)

    override fun observeUser(userId: String): Flow<UserEntity?> = userDao.observeById(userId)

    override suspend fun setDefaultCurrency(userId: String, currencyCode: String) {
        val user = userDao.getById(userId) ?: return
        userDao.update(user.copy(defaultCurrency = currencyCode, updatedAt = System.currentTimeMillis()))
    }
}