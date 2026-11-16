package com.nintech.budgment.ui.notifications

import com.nintech.budgment.data.local.entity.NotificationEntity
import com.nintech.budgment.data.local.entity.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class GroupByDayTest {

    private fun at(day: Int, hour: Int): Long = Calendar.getInstance().apply {
        clear(); set(2026, Calendar.NOVEMBER, day, hour, 0)
    }.timeInMillis

    private fun item(id: String, createdAt: Long) = InboxItem(
        NotificationEntity(
            id = id, userId = "u", type = NotificationType.RECURRING_DUE, refId = null,
            dedupKey = id, title = id, body = "", createdAt = createdAt
        ),
        isNew = false,
        action = InboxAction.RESOLVED
    )

    @Test
    fun `agrupa por dia con lo mas reciente primero`() {
        val days = groupByDay(
            listOf(item("a", at(14, 9)), item("b", at(15, 8)), item("c", at(15, 20)), item("d", at(13, 23)))
        )
        assertEquals(listOf(startOfDay(at(15, 0)), startOfDay(at(14, 0)), startOfDay(at(13, 0))), days.map { it.dayStart })
        assertEquals(listOf("c", "b"), days.first().items.map { it.notification.id })
    }

    @Test
    fun `la medianoche pertenece al dia que empieza`() {
        assertEquals(at(15, 0), startOfDay(at(15, 0)))
        assertEquals(at(15, 0), startOfDay(at(15, 23)))
    }
}
