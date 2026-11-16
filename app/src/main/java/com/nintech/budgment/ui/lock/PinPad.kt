package com.nintech.budgment.ui.lock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.nintech.budgment.R

/**
 * Puntos que indican cuántos dígitos se llevan. [shakeKey] distinto de 0 los sacude (PIN
 * incorrecto); cada valor nuevo dispara una sacudida.
 */
@Composable
fun PinDots(
    slots: Int,
    filled: Int,
    color: Color,
    shakeKey: Int,
    modifier: Modifier = Modifier
) {
    val offset = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(shakeKey) {
        if (shakeKey == 0) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.Reject)
        offset.animateTo(0f, keyframes {
            durationMillis = 360
            -18f at 45; 16f at 105; -12f at 165; 8f at 225; -4f at 285
        })
    }
    val description = pluralStringResource(R.plurals.pin_digits_entered, filled, filled)
    Row(
        modifier = modifier
            .offset { IntOffset(offset.value.dp.roundToPx(), 0) }
            .clearAndSetSemantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(slots) { index ->
            val isFilled = index < filled
            Box(
                Modifier
                    .size(14.dp)
                    .then(
                        if (isFilled) Modifier.background(color, CircleShape)
                        else Modifier.border(1.5.dp, color.copy(alpha = 0.6f), CircleShape)
                    )
            )
        }
    }
}

/**
 * Teclado numérico de 3 × 4. [extraKey] ocupa el hueco inferior izquierdo (p. ej. biometría).
 */
@Composable
fun PinPad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    contentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    extraKey: (@Composable () -> Unit)? = null
) {
    val rows = listOf("123", "456", "789")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { digit -> DigitKey(digit, contentColor, enabled) { onDigit(digit) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(KEY_SIZE), contentAlignment = Alignment.Center) { extraKey?.invoke() }
            DigitKey('0', contentColor, enabled) { onDigit('0') }
            Box(Modifier.size(KEY_SIZE), contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = onDelete,
                    enabled = enabled,
                    modifier = Modifier.size(KEY_SIZE),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = contentColor)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = stringResource(R.string.pin_key_delete))
                }
            }
        }
    }
}

@Composable
private fun DigitKey(digit: Char, contentColor: Color, enabled: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = Modifier.size(KEY_SIZE),
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor,
            disabledContentColor = contentColor.copy(alpha = 0.38f)
        )
    ) {
        Text(digit.toString(), style = MaterialTheme.typography.headlineMedium)
    }
}

/** Tamaño de cada tecla: más que los 48 dp mínimos, como un teclado de teléfono. */
val KEY_SIZE = 72.dp
