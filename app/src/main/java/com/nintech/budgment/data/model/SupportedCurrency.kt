package com.nintech.budgment.data.model

import java.util.Currency

enum class SupportedCurrency(val code: String) {
    MXN("MXN"),
    USD("USD"),
    EUR("EUR"),
    CAD("CAD"),
    GBP("GBP"),
    JPY("JPY"),
    CHF("CHF");

    val displayName: String
        get() = try {
            Currency.getInstance(code).displayName
        } catch (e: IllegalArgumentException) {
            code
        }

    companion object {
        val ALL: List<SupportedCurrency> = entries

        fun fromCode(code: String?): SupportedCurrency? =
            ALL.firstOrNull { it.code == code?.uppercase() }
    }
}