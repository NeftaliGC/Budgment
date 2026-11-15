package com.nintech.budgment.data.repository

import com.nintech.budgment.data.local.dao.AccountDao
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.model.AccountType
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

interface AccountRepository {
    fun observeAccounts(userId: String): Flow<List<AccountEntity>>

    /** Incluye las cuentas eliminadas, para resolver nombre y moneda de movimientos del historial. */
    fun observeAccountsIncludingDeleted(userId: String): Flow<List<AccountEntity>>
    suspend fun getAccount(id: String): AccountEntity?
    suspend fun createAccount(
        userId: String, name: String, accountType: AccountType, currency: String,
        creditLimit: Long?, statementDay: Int?, paymentDueDay: Int?,
        icon: String? = null, color: String? = null,
        initialBalance: Long = 0
    )
    suspend fun updateAccount(
        account: AccountEntity, name: String, accountType: AccountType, currency: String,
        creditLimit: Long?, statementDay: Int?, paymentDueDay: Int?,
        icon: String? = null, color: String? = null
    )

    /**
     * Elimina la cuenta solo si su saldo es cero. Sus movimientos se conservan en el historial.
     * @return false si la cuenta tiene saldo y no se eliminó.
     */
    suspend fun deleteAccount(id: String): Boolean

    suspend fun adjustBalance(accountId: String, delta: Long)
}

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun observeAccounts(userId: String): Flow<List<AccountEntity>> =
        accountDao.observeAccounts(userId)

    override fun observeAccountsIncludingDeleted(userId: String): Flow<List<AccountEntity>> =
        accountDao.observeAllAccounts(userId)

    override suspend fun getAccount(id: String): AccountEntity? =
        accountDao.getById(id)

    override suspend fun createAccount(
        userId: String, name: String, accountType: AccountType, currency: String,
        creditLimit: Long?, statementDay: Int?, paymentDueDay: Int?,
        icon: String?, color: String?,
        initialBalance: Long
    ) {
        accountDao.insert(
            AccountEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                accountType = accountType,
                currency = currency,
                icon = icon,
                color = color,
                balanceCache = initialBalance,
                creditLimit = creditLimit,
                statementDay = statementDay,
                paymentDueDay = paymentDueDay
            )
        )
    }

    override suspend fun updateAccount(
        account: AccountEntity, name: String, accountType: AccountType, currency: String,
        creditLimit: Long?, statementDay: Int?, paymentDueDay: Int?,
        icon: String?, color: String?
    ) {
        accountDao.update(
            account.copy(
                name = name,
                accountType = accountType,
                currency = currency,
                icon = icon,
                color = color,
                creditLimit = creditLimit,
                statementDay = statementDay,
                paymentDueDay = paymentDueDay,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteAccount(id: String): Boolean {
        val account = accountDao.getById(id) ?: return false
        if (account.balanceCache != 0L) return false
        accountDao.softDelete(id)
        return true
    }

    override suspend fun adjustBalance(accountId: String, delta: Long) {
        val account = accountDao.getById(accountId) ?: return
        accountDao.updateBalanceCache(accountId, account.balanceCache + delta)
    }
}