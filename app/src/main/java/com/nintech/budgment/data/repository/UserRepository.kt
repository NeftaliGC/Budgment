package com.nintech.budgment.data.repository

import com.nintech.budgment.data.local.dao.UserDao
import com.nintech.budgment.data.local.entity.UserEntity
import javax.inject.Inject

interface UserRepository {
    suspend fun getUser(userId: String): UserEntity?
}

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {
    override suspend fun getUser(userId: String): UserEntity? = userDao.getById(userId)
}