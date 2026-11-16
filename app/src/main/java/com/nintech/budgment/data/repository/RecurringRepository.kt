package com.nintech.budgment.data.repository

import androidx.room.withTransaction
import com.nintech.budgment.data.local.BudgmentDatabase
import com.nintech.budgment.data.local.dao.RecurringTransactionDao
import com.nintech.budgment.data.local.dao.TransactionDao
import com.nintech.budgment.data.local.entity.RecurringTransactionEntity
import com.nintech.budgment.data.local.entity.TransactionEntity
import com.nintech.budgment.data.model.CalendarMath
import com.nintech.budgment.data.model.Recurrence
import com.nintech.budgment.data.model.RecurrenceFrequency
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/**
 * Movimientos que se repiten (renta, nómina, suscripciones). Nunca se registran solos: cuando
 * vence una ocurrencia queda pendiente hasta que el usuario la registra, la pospone o la omite.
 */
interface RecurringRepository {
    fun observeRecurring(userId: String): Flow<List<RecurringTransactionEntity>>
    suspend fun getRecurring(id: String): RecurringTransactionEntity?

    /** Pendientes de confirmar en [now], para el aviso diario. */
    suspend fun getPending(userId: String, now: Long = System.currentTimeMillis()): List<RecurringTransactionEntity>

    /** Crea un recurrente cuya primera ocurrencia por confirmar es la próxima a partir de hoy. */
    suspend fun createRecurring(
        userId: String, accountId: String, categoryId: String, amount: Long, description: String?,
        frequency: RecurrenceFrequency, anchorDate: Long
    )

    /**
     * Registra un movimiento y lo convierte en el primero de una serie: la siguiente ocurrencia
     * queda pendiente para cuando venza.
     */
    suspend fun createTransactionAndRecurring(
        userId: String, accountId: String, categoryId: String, amount: Long, description: String?,
        frequency: RecurrenceFrequency, date: Long, transactionId: String = UUID.randomUUID().toString()
    )

    /** Si cambia la frecuencia o la fecha, la serie se recalcula desde hoy. */
    suspend fun updateRecurring(
        recurring: RecurringTransactionEntity, accountId: String, categoryId: String, amount: Long,
        description: String?, frequency: RecurrenceFrequency, anchorDate: Long
    )

    /** Al reanudar, las ocurrencias que vencieron en pausa no se piden. */
    suspend fun setActive(id: String, active: Boolean)
    suspend fun deleteRecurring(id: String)
    suspend fun restoreRecurring(id: String)

    /**
     * Registra la ocurrencia [occurrence] con los datos dados y avanza a la siguiente.
     * No hace nada si esa ocurrencia ya se resolvió (p. ej. por un doble toque).
     * @return true si se registró.
     */
    suspend fun registerOccurrence(
        id: String, occurrence: Long, accountId: String, categoryId: String, amount: Long,
        description: String?, date: Long, transactionId: String = UUID.randomUUID().toString()
    ): Boolean

    /** Registra la ocurrencia tal cual la define el recurrente. */
    suspend fun registerOccurrenceAsIs(id: String, occurrence: Long): Boolean

    /** «Omitir esta vez»: avanza sin registrar nada. */
    suspend fun skipOccurrence(id: String, occurrence: Long)

    /** «Más tarde»: deja de anunciarse hasta [until]. */
    suspend fun snooze(id: String, until: Long)
}

class RecurringRepositoryImpl @Inject constructor(
    private val database: BudgmentDatabase,
    private val recurringDao: RecurringTransactionDao,
    private val transactionDao: TransactionDao,
    private val accountRepository: AccountRepository
) : RecurringRepository {

    override fun observeRecurring(userId: String): Flow<List<RecurringTransactionEntity>> =
        recurringDao.observeByUser(userId)

    override suspend fun getRecurring(id: String): RecurringTransactionEntity? = recurringDao.getById(id)

    override suspend fun getPending(userId: String, now: Long): List<RecurringTransactionEntity> =
        recurringDao.getPending(userId, now)

    override suspend fun createRecurring(
        userId: String, accountId: String, categoryId: String, amount: Long, description: String?,
        frequency: RecurrenceFrequency, anchorDate: Long
    ) {
        val anchor = CalendarMath.startOfDay(anchorDate)
        recurringDao.insert(
            newRecurring(
                userId, accountId, categoryId, amount, description, frequency, anchor,
                next = Recurrence.firstUpcoming(anchor, frequency.unit, frequency.interval)
            )
        )
    }

    override suspend fun createTransactionAndRecurring(
        userId: String, accountId: String, categoryId: String, amount: Long, description: String?,
        frequency: RecurrenceFrequency, date: Long, transactionId: String
    ) {
        val anchor = CalendarMath.startOfDay(date)
        // La ocurrencia del ancla ya queda registrada; tampoco se piden las que hayan pasado
        // entre esa fecha y hoy.
        val next = Recurrence.occurrenceAtOrAfter(
            anchor, frequency.unit, frequency.interval,
            maxOf(anchor + 1, CalendarMath.startOfDay(System.currentTimeMillis()))
        )
        val recurring = newRecurring(userId, accountId, categoryId, amount, description, frequency, anchor, next)
        database.withTransaction {
            recurringDao.insert(recurring)
            insertTransaction(recurring, accountId, categoryId, amount, description, date, transactionId)
        }
    }

    override suspend fun updateRecurring(
        recurring: RecurringTransactionEntity, accountId: String, categoryId: String, amount: Long,
        description: String?, frequency: RecurrenceFrequency, anchorDate: Long
    ) {
        val anchor = CalendarMath.startOfDay(anchorDate)
        val scheduleChanged = anchor != recurring.anchorDate ||
            frequency.unit != recurring.periodUnit || frequency.interval != recurring.periodInterval
        recurringDao.update(
            recurring.copy(
                accountId = accountId,
                categoryId = categoryId,
                baseAmount = amount,
                description = description,
                periodUnit = frequency.unit,
                periodInterval = frequency.interval,
                anchorDate = anchor,
                nextOccurrence = if (scheduleChanged) {
                    Recurrence.firstUpcoming(anchor, frequency.unit, frequency.interval)
                } else {
                    recurring.nextOccurrence
                },
                snoozedUntil = if (scheduleChanged) null else recurring.snoozedUntil,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun setActive(id: String, active: Boolean) {
        val recurring = recurringDao.getById(id) ?: return
        val next = if (active) {
            maxOf(
                recurring.nextOccurrence,
                Recurrence.firstUpcoming(recurring.anchorDate, recurring.periodUnit, recurring.periodInterval)
            )
        } else {
            recurring.nextOccurrence
        }
        recurringDao.update(
            recurring.copy(active = active, nextOccurrence = next, snoozedUntil = null, updatedAt = System.currentTimeMillis())
        )
    }

    override suspend fun deleteRecurring(id: String) = recurringDao.softDelete(id)

    override suspend fun restoreRecurring(id: String) = recurringDao.restore(id)

    override suspend fun registerOccurrence(
        id: String, occurrence: Long, accountId: String, categoryId: String, amount: Long,
        description: String?, date: Long, transactionId: String
    ): Boolean = database.withTransaction {
        val recurring = recurringDao.getById(id)
        if (recurring == null || recurring.nextOccurrence != occurrence) return@withTransaction false
        insertTransaction(recurring, accountId, categoryId, amount, description, date, transactionId)
        advance(recurring)
        true
    }

    override suspend fun registerOccurrenceAsIs(id: String, occurrence: Long): Boolean {
        val recurring = recurringDao.getById(id) ?: return false
        return registerOccurrence(
            id, occurrence, recurring.accountId, recurring.categoryId, recurring.baseAmount,
            recurring.description, Recurrence.transactionDateFor(occurrence)
        )
    }

    override suspend fun skipOccurrence(id: String, occurrence: Long) {
        database.withTransaction {
            val recurring = recurringDao.getById(id) ?: return@withTransaction
            if (recurring.nextOccurrence == occurrence) advance(recurring)
        }
    }

    override suspend fun snooze(id: String, until: Long) = recurringDao.snooze(id, until)

    private suspend fun insertTransaction(
        recurring: RecurringTransactionEntity, accountId: String, categoryId: String, amount: Long,
        description: String?, date: Long, transactionId: String
    ) {
        transactionDao.insert(
            TransactionEntity(
                id = transactionId,
                userId = recurring.userId,
                accountId = accountId,
                categoryId = categoryId,
                recurringId = recurring.id,
                amount = amount,
                description = description,
                date = date
            )
        )
        accountRepository.adjustBalance(accountId, amount)
    }

    private suspend fun advance(recurring: RecurringTransactionEntity) {
        val next = Recurrence.occurrenceAfter(
            recurring.anchorDate, recurring.periodUnit, recurring.periodInterval, recurring.nextOccurrence
        )
        recurringDao.update(recurring.copy(nextOccurrence = next, snoozedUntil = null, updatedAt = System.currentTimeMillis()))
    }

    private fun newRecurring(
        userId: String, accountId: String, categoryId: String, amount: Long, description: String?,
        frequency: RecurrenceFrequency, anchor: Long, next: Long
    ) = RecurringTransactionEntity(
        id = UUID.randomUUID().toString(),
        userId = userId,
        accountId = accountId,
        categoryId = categoryId,
        baseAmount = amount,
        description = description,
        periodUnit = frequency.unit,
        periodInterval = frequency.interval,
        anchorDate = anchor,
        nextOccurrence = next
    )
}
