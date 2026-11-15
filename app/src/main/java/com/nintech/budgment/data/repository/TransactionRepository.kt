package com.nintech.budgment.data.repository

import com.nintech.budgment.data.local.dao.TransactionDao
import com.nintech.budgment.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

interface TransactionRepository {
    fun observeTransactions(userId: String): Flow<List<TransactionEntity>>
    suspend fun getTransaction(id: String): TransactionEntity?
    suspend fun createTransaction(userId: String, accountId: String, categoryId: String, amount: Long, description: String?, date: Long)
    suspend fun updateTransaction(transaction: TransactionEntity, accountId: String, categoryId: String, amount: Long, description: String?, date: Long)
    suspend fun deleteTransaction(id: String)
    suspend fun restoreTransaction(id: String)
}

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountRepository: AccountRepository
) : TransactionRepository {

    override fun observeTransactions(userId: String): Flow<List<TransactionEntity>> =
        transactionDao.observeByUser(userId)

    override suspend fun getTransaction(id: String): TransactionEntity? =
        transactionDao.getById(id)

    override suspend fun createTransaction(
        userId: String, accountId: String, categoryId: String, amount: Long, description: String?, date: Long
    ) {
        transactionDao.insert(
            TransactionEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                accountId = accountId,
                categoryId = categoryId,
                amount = amount,
                description = description,
                date = date
            )
        )
        accountRepository.adjustBalance(accountId, amount)
    }

    override suspend fun updateTransaction(
        transaction: TransactionEntity, accountId: String, categoryId: String, amount: Long, description: String?, date: Long
    ) {
        // Revierte el efecto en la cuenta original y aplica el nuevo en la cuenta (nueva o misma)
        accountRepository.adjustBalance(transaction.accountId, -transaction.amount)
        accountRepository.adjustBalance(accountId, amount)

        transactionDao.update(
            transaction.copy(
                accountId = accountId,
                categoryId = categoryId,
                amount = amount,
                description = description,
                date = date,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteTransaction(id: String) {
        val transaction = transactionDao.getById(id) ?: return
        accountRepository.adjustBalance(transaction.accountId, -transaction.amount)
        transactionDao.softDelete(id)
    }

    override suspend fun restoreTransaction(id: String) {
        val transaction = transactionDao.getById(id) ?: return
        if (transaction.deletedAt == null) return
        transactionDao.restore(id)
        accountRepository.adjustBalance(transaction.accountId, transaction.amount)
    }
}