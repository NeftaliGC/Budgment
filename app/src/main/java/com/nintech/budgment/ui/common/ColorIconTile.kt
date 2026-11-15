package com.nintech.budgment.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nintech.budgment.ui.theme.Night

/**
 * Devuelve blanco o Night, el que tenga mayor contraste (WCAG) sobre este color.
 * Sirve para dibujar contenido encima de colores elegidos por el usuario.
 */
fun Color.readableContentColor(): Color {
    val background = luminance()
    val contrastWithWhite = 1.05f / (background + 0.05f)
    val contrastWithNight = (background + 0.05f) / (Night.luminance() + 0.05f)
    return if (contrastWithWhite >= contrastWithNight) Color.White else Night
}

/**
 * Mosaico de color sólido con un icono encima. El icono se dibuja en el color legible sobre el
 * mosaico, así cualquier color de cuenta o categoría funciona en tema claro y oscuro.
 */
@Composable
fun ColorIconTile(
    color: Color,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    cornerRadius: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color.readableContentColor(),
                modifier = Modifier.size(size / 2)
            )
        }
    }
}
