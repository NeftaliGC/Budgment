package com.nintech.budgment.ui.common

import java.util.Locale

/**
 * Locale para fechas y montos, acorde al idioma en que se muestra la app.
 * Español usa el formato de México, como hasta ahora; inglés respeta la región del teléfono;
 * cualquier otro idioma ve la app en inglés, así que se formatea como inglés (EE. UU.).
 */
fun appLocale(): Locale {
    val device = Locale.getDefault()
    return when (device.language) {
        "es" -> Locale("es", "MX")
        "en" -> device
        else -> Locale.US
    }
}

fun isSpanish(): Boolean = appLocale().language == "es"
