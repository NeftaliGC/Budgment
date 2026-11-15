package com.nintech.budgment.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nintech.budgment.ui.categories.colorNameRes
import com.nintech.budgment.ui.categories.parseHexColor

private const val PICKER_COLUMNS = 6
private val PickerItemSize = 44.dp

/**
 * Cuadrícula de iconos de selección única. Cada icono se anuncia con su nombre y su estado
 * (seleccionado o no) en el lector de pantalla.
 */
@Composable
fun <T> IconPicker(
    items: List<T>,
    icon: (T) -> ImageVector,
    label: @Composable (T) -> String,
    isSelected: (T) -> Boolean,
    onClick: (T) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Column(modifier.selectableGroup()) {
        items.chunked(PICKER_COLUMNS).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    val selected = isSelected(item)
                    val description = label(item)
                    Box(
                        modifier = Modifier
                            .size(PickerItemSize)
                            .clip(shape)
                            .background(
                                if (selected) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                if (selected) 2.dp else 1.dp,
                                if (selected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                shape
                            )
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { onClick(item) })
                            .semantics { contentDescription = description },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon(item),
                            contentDescription = null,
                            tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cuadrícula de colores de selección única. La primera opción (null) es el color automático.
 * La selección se marca con borde y con una marca de verificación, no solo con color.
 */
@Composable
fun ColorPicker(
    colors: List<String>,
    selectedHex: String?,
    onSelect: (String?) -> Unit,
    defaultLabel: String,
    modifier: Modifier = Modifier
) {
    Column(modifier.selectableGroup()) {
        (listOf<String?>(null) + colors).chunked(PICKER_COLUMNS).forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowColors.forEach { hex ->
                    val color = parseHexColor(hex)
                    val selected = selectedHex == hex
                    val description = when {
                        hex == null -> defaultLabel
                        else -> colorNameRes(hex)?.let { stringResource(it) } ?: hex
                    }
                    Box(
                        modifier = Modifier
                            .size(PickerItemSize)
                            .clip(CircleShape)
                            .background(color ?: MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                if (selected) 3.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                CircleShape
                            )
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(hex) })
                            .semantics { contentDescription = description },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            selected -> Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = color?.readableContentColor() ?: MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            color == null -> Icon(
                                imageVector = Icons.Filled.NotInterested,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
