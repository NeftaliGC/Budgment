package com.nintech.budgment.ui.categories

import androidx.compose.ui.graphics.Color
import androidx.annotation.StringRes
import com.nintech.budgment.R

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

/** Nombre legible del color de la paleta, para el lector de pantalla. */
@StringRes
fun colorNameRes(hex: String): Int? = when (hex.uppercase()) {
    "#F44336" -> R.string.color_red
    "#E91E63" -> R.string.color_pink
    "#9C27B0" -> R.string.color_purple
    "#3F51B5" -> R.string.color_indigo
    "#2196F3" -> R.string.color_blue
    "#009688" -> R.string.color_teal
    "#4CAF50" -> R.string.color_green
    "#8BC34A" -> R.string.color_light_green
    "#FFC107" -> R.string.color_amber
    "#FF9800" -> R.string.color_orange
    "#795548" -> R.string.color_brown
    "#607D8B" -> R.string.color_blue_grey
    else -> null
}

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