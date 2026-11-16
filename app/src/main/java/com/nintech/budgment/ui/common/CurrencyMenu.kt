package com.nintech.budgment.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nintech.budgment.R
import com.nintech.budgment.data.model.SupportedCurrency

/**
 * Selector compacto de la moneda en la que se muestran los totales. Va junto al monto al que
 * afecta. La moneda por defecto se define en Configuración; esto solo cambia la vista.
 */
@Composable
fun CurrencyMenu(
    selectedCode: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.currency_menu_content_description, selectedCode)

    Box(modifier) {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(start = 12.dp, end = 4.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
            modifier = Modifier.semantics { contentDescription = description }
        ) {
            Text(selectedCode, style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SupportedCurrency.ALL.forEach { currency ->
                DropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.currency_option_format, currency.code, currency.displayName))
                    },
                    trailingIcon = {
                        if (currency.code == selectedCode) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(currency.code)
                    }
                )
            }
        }
    }
}
