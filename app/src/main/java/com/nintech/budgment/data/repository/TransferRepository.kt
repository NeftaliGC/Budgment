package com.nintech.budgment.data.repository

import androidx.room.withTransaction
import com.nintech.budgment.data.local.BudgmentDatabase
import com.nintech.budgment.data.local.dao.TransactionDao
import com.nintech.budgment.data.local.dao.TransferDao
import com.nintech.budgment.data.local.entity.TransactionEntity
import com.nintech.budgment.data.local.entity.TransferEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/** Una transferencia con sus dos movimientos: la salida (monto negativo) y la entrada (positivo). */
data class TransferWithTransactions(
    val transfer: TransferEntity,
    val outgoing: TransactionEntity,
    val incoming: TransactionEntity
)

/**
 * Movimiento de dinero entre dos cuentas del usuario. Se guarda como dos movimientos sin
 * categoría (salida en el origen, entrada en el destino) enlazados por un [TransferEntity]:
 * así los saldos cuadran y no cuenta como gasto ni ingreso en presupuestos.
 *
 * Si las cuentas usan monedas distintas, [amountIn] es lo que llega al destino en su moneda.
 */
interface TransferRepository {
    fun observeTransfers(userId: String): Flow<List<TransferEntity>>
    suspend fun getTransfer(id: String): TransferWithTransactions?

    /** La transferencia a la que pertenece un movimiento, o null si es un gasto o ingreso normal. */
    suspend fun getTransferIdForTransaction(transactionId: String): String?

    suspend fun createTransfer(
        userId: String, fromAccountId: String, toAccountId: String,
        amountOut: Long, amountIn: Long, description: String?, date: Long
    )

    suspend fun updateTransfer(
        id: String, fromAccountId: String, toAccountId: String,
        amountOut: Long, amountIn: Long, description: String?, date: Long
    )

    suspend fun deleteTransfer(id: String)
    suspend fun restoreTransfer(id: String)
}

class TransferRepositoryImpl @Inject constructor(
    private val database: BudgmentDatabase,
    private val transferDao: TransferDao,
    private val transactionDao: TransactionDao,
    private val accountRepository: AccountRepository
) : TransferRepository {

    override fun observeTransfers(userId: String): Flow<List<TransferEntity>> =
        transferDao.observeByUser(userId)

    override suspend fun getTransfer(id: String): TransferWithTransactions? {
        val transfer = transferDao.getById(id) ?: return null
        val outgoing = transactionDao.getById(transfer.fromTransactionId) ?: return null
        val incoming = transactionDao.getById(transfer.toTransactionId) ?: return null
        return TransferWithTransactions(transfer, outgoing, incoming)
    }

    override suspend fun getTransferIdForTransaction(transactionId: String): String? =
        transferDao.getByTransactionId(transactionId)?.id

    override suspend fun createTransfer(
        userId: String, fromAccountId: String, toAccountId: String,
        amountOut: Long, amountIn: Long, description: String?, date: Long
    ) {
        require(fromAccountId != toAccountId) { "Origen y destino deben ser cuentas distintas" }
        require(amountOut > 0 && amountIn > 0) { "Los montos deben ser positivos" }

        val outgoing = TransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            accountId = fromAccountId,
            categoryId = null,
            amount = -amountOut,
            description = description,
            date = date
        )
        val incoming = outgoing.copy(
            id = UUID.randomUUID().toString(),
            accountId = toAccountId,
            amount = amountIn
        )
        // Todo o nada: una transferencia a medias descuadraría los saldos.
        database.withTransaction {
            transactionDao.insert(outgoing)
            transactionDao.insert(incoming)
            transferDao.insert(
                TransferEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    fromAccountId = fromAccountId,
                    toAccountId = toAccountId,
                    fromTransactionId = outgoing.id,
                    toTransactionId = incoming.id
                )
            )
            accountRepository.adjustBalance(fromAccountId, -amountOut)
            accountRepository.adjustBalance(toAccountId, amountIn)
        }
    }

    override suspend fun updateTransfer(
        id: String, fromAccountId: String, toAccountId: String,
        amountOut: Long, amountIn: Long, description: String?, date: Long
    ) {
        require(fromAccountId != toAccountId) { "Origen y destino deben ser cuentas distintas" }
        require(amountOut > 0 && amountIn > 0) { "Los montos deben ser positivos" }

        database.withTransaction {
            val current = getTransfer(id) ?: return@withTransaction
            val now = System.currentTimeMillis()

            // Revierte el efecto anterior en ambas cuentas y aplica el nuevo.
            accountRepository.adjustBalance(current.outgoing.accountId, -current.outgoing.amount)
            accountRepository.adjustBalance(current.incoming.accountId, -current.incoming.amount)
            accountRepository.adjustBalance(fromAccountId, -amountOut)
            accountRepository.adjustBalance(toAccountId, amountIn)

            transactionDao.update(
                current.outgoing.copy(
                    accountId = fromAccountId, amount = -amountOut,
                    description = description, date = date, updatedAt = now
                )
            )
            transactionDao.update(
                current.incoming.copy(
                    accountId = toAccountId, amount = amountIn,
                    description = description, date = date, updatedAt = now
                )
            )
            transferDao.updateAccounts(id, fromAccountId, toAccountId)
        }
    }

    override suspend fun deleteTransfer(id: String) {
        database.withTransaction {
            val current = getTransfer(id) ?: return@withTransaction
            listOf(current.outgoing, current.incoming)
                .filter { it.deletedAt == null }
                .forEach { tx ->
                    accountRepository.adjustBalance(tx.accountId, -tx.amount)
                    transactionDao.softDelete(tx.id)
                }
        }
    }

    override suspend fun restoreTransfer(id: String) {
        database.withTransaction {
            val current = getTransfer(id) ?: return@withTransaction
            listOf(current.outgoing, current.incoming)
                .filter { it.deletedAt != null }
                .forEach { tx ->
                    transactionDao.restore(tx.id)
                    accountRepository.adjustBalance(tx.accountId, tx.amount)
                }
        }
    }
}
