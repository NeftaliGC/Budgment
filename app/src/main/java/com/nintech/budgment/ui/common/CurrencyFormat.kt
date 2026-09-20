package com.nintech.budgment.ui.common

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun formatCents(cents: Long, currencyCode: String): String = try {
    NumberFormat.getCurrencyInstance(Locale("es", "MX")).apply {
        currency = Currency.getInstance(currencyCode)
    }.format(cents / 100.0)
} catch (e: Exception) {
    "%.2f %s".format(cents / 100.0, currencyCode)
}