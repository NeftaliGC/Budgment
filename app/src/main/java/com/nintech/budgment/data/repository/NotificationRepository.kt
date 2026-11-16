package com.nintech.budgment.data.repository

import android.content.Context
import com.nintech.budgment.R
import com.nintech.budgment.data.local.dao.AccountDao
import com.nintech.budgment.data.local.dao.CategoryDao
import com.nintech.budgment.data.local.dao.NotificationDao
import com.nintech.budgment.data.local.dao.RecurringTransactionDao
import com.nintech.budgment.data.local.entity.NotificationEntity
import com.nintech.budgment.data.local.entity.NotificationType
import com.nintech.budgment.ui.common.formatCents
import com.nintech.budgment.ui.common.formatShortDate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

interface NotificationRepository {
    fun observeInbox(userId: String): Flow<List<NotificationEntity>>
    fun observeUnreadCount(userId: String): Flow<Int>
    suspend fun markAllRead(userId: String)

    /** Guarda el aviso si no existía uno igual; devuelve true si es nuevo. */
    suspend fun insertIfNew(notification: NotificationEntity): Boolean
    suspend fun delete(notifications: List<NotificationEntity>)
    suspend fun restore(notifications: List<NotificationEntity>)

    /**
     * Agrega a la bandeja los recurrentes vencidos que aún no se habían avisado (uno por
     * ocurrencia) y limpia los avisos viejos. Se puede llamar las veces que sea: no duplica.
     */
    suspend fun syncRecurring(userId: String, now: Long = System.currentTimeMillis())
}

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationDao: NotificationDao,
    private val recurringDao: RecurringTransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao
) : NotificationRepository {

    override fun observeInbox(userId: String): Flow<List<NotificationEntity>> =
        notificationDao.observeByUser(userId)

    override fun observeUnreadCount(userId: String): Flow<Int> =
        notificationDao.observeUnreadCount(userId)

    override suspend fun markAllRead(userId: String) = notificationDao.markAllRead(userId)

    override suspend fun insertIfNew(notification: NotificationEntity): Boolean =
        notificationDao.insertIfNew(notification) != -1L

    override suspend fun delete(notifications: List<NotificationEntity>) =
        notificationDao.deleteByIds(notifications.map { it.id })

    override suspend fun restore(notifications: List<NotificationEntity>) =
        notificationDao.insertAll(notifications)

    override suspend fun syncRecurring(userId: String, now: Long) {
        notificationDao.deleteOlderThan(userId, now - RETENTION_MILLIS)

        recurringDao.getPending(userId, now).forEach { recurring ->
            val dedupKey = "recurring:${recurring.id}:${recurring.nextOccurrence}"
            val account = accountDao.getById(recurring.accountId)
            val category = categoryDao.getById(recurring.categoryId)
            val name = recurring.description?.takeIf { it.isNotBlank() } ?: category?.name.orEmpty()
            val currency = recurring.currency ?: account?.currency ?: "MXN"
            val amount = formatCents(abs(recurring.baseAmount), currency)
            val body = context.getString(
                if (recurring.baseAmount < 0) R.string.inbox_recurring_body_expense else R.string.inbox_recurring_body_income,
                amount,
                account?.name.orEmpty(),
                formatShortDate(recurring.nextOccurrence)
            )
            notificationDao.insertIfNew(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = NotificationType.RECURRING_DUE,
                    refId = recurring.id,
                    occurrence = recurring.nextOccurrence,
                    dedupKey = dedupKey,
                    title = context.getString(R.string.inbox_recurring_title, name),
                    body = body,
                    createdAt = now
                )
            )
        }
    }

    private companion object {
        /** Los avisos de más de 90 días se borran solos. */
        val RETENTION_MILLIS = TimeUnit.DAYS.toMillis(90)
    }
}
