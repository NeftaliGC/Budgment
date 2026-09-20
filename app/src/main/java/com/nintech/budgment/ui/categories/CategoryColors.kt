package com.nintech.budgment.ui.categories

import androidx.compose.ui.graphics.Color

val CategoryColorPalette: List<String> = listOf(
    "#F44336", // Rojo
    "#E91E63", // Rosa
    "#9C27B0", // Púrpura
    "#3F51B5", // Índigo
    "#2196F3", // Azul
    "#009688", // Teal
    "#4CAF50", // Verde
    "#8BC34A", // Verde claro
    "#FFC107", // Ámbar
    "#FF9800", // Naranja
    "#795548", // Café
    "#607D8B"  // Gris azulado
)

fun parseHexColor(raw: String?): Color? {
    val clean = raw?.removePrefix("#") ?: return null
    return runCatching {
        when {
            clean.length == 6 -> Color(("FF$clean").toLong(16).toInt())
            clean.length == 8 -> Color(clean.toLong(16).toInt())
            else -> null
        }
    }.getOrNull()
}