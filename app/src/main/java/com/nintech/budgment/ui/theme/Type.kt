package com.nintech.budgment.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Escala tipográfica de la app: cinco tamaños (34 / 22 / 16 / 14 / 12), nada por debajo de 12 sp.
 * Las pantallas usan estos estilos en vez de tamaños sueltos.
 *
 * | Estilo        | Uso                                             |
 * |---------------|-------------------------------------------------|
 * | displaySmall  | Cifra principal (saldo total) y nombre de la app |
 * | headlineSmall | Título de pantalla sin barra superior            |
 * | titleMedium   | Título de fila o tarjeta, montos en listas       |
 * | bodyLarge     | Texto de los campos de formulario                |
 * | bodyMedium    | Texto de apoyo y descripciones                   |
 * | labelLarge    | Encabezado de sección, enlaces, botones          |
 * | bodySmall     | Notas y datos secundarios                        |
 * | labelMedium   | Etiqueta pequeña con énfasis                     |
 */
val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Cifras tabulares: todos los dígitos ocupan el mismo ancho, así los montos se alinean en columnas
 * y no "bailan" al cambiar de valor. Se aplica a cualquier texto que muestre dinero.
 */
val TextStyle.tabular: TextStyle
    get() = copy(fontFeatureSettings = "tnum")
