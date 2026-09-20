package com.nintech.budgment.data.repository

import com.nintech.budgment.data.local.dao.AccountDao
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.model.AccountType
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

interface AccountRepository {
    fun observeAccounts(userId: String): Flow<List<AccountEntity>>
    suspend fun getAccount(id: String): AccountEntity?
    suspend fun createAccount(
        userId: String, name: String, accountType: AccountType, currency: String,
        creditLimit: Long?, statementDay: Int?, paymentDueDay: Int?
    )
    suspend fun updateAccount(
        account: AccountEntity, name: String, accountType: AccountType, currency: String,
        creditLimit: Long?, statementDay: Int?, paymentDueDay: Int?
    )
    suspend fun deleteAccount(id: String)

    suspend fun adjustBalance(accountId: String, delta: Long)
}

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun observeAccounts(userId: String): Flow<List<AccountEntity>> =
        accountDao.observeAccounts(userId)

    override suspend fun getAccount(id: String): AccountEntity? =
        accountDao.getById(id)

    override suspend fun createAccount(
        userId: String, name: String, accountType: AccountType, currency: String,
        creditLimit: Long?, statementDay: Int?, paymentDueDay: Int?
    ) {
        accountDao.insert(
            AccountEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                accountType = accountType,
                currency = currency,
                creditLimit = creditLimit,
                statementDay = statementDay,
                paymentDueDay = paymentDueDay
            )
        )
    }

    override suspend fun updateAccount(
        account: AccountEntity, name: String, accountType: AccountType, currency: String,
        creditLimit: Long?, statementDay: Int?, paymentDueDay: Int?
    ) {
        accountDao.update(
            account.copy(
                name = name,
                accountType = accountType,
                currency = currency,
                creditLimit = creditLimit,
                statementDay = statementDay,
                paymentDueDay = paymentDueDay,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteAccount(id: String) {
        accountDao.softDelete(id)
    }

    override suspend fun adjustBalance(accountId: String, delta: Long) {
        val account = accountDao.getById(accountId) ?: return
        accountDao.updateBalanceCache(accountId, account.balanceCache + delta)
    }
}