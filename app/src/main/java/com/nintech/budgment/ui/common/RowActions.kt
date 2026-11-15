package com.nintech.budgment.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.nintech.budgment.R
import kotlinx.coroutines.launch

/**
 * Menú "más opciones" de una fila. Eliminar queda a dos toques para que no se dispare
 * por accidente al tocar la fila para editarla.
 */
@Composable
fun RowActionsMenu(deleteLabel: String, onDeleteClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.common_more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(deleteLabel, color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                onClick = {
                    expanded = false
                    onDeleteClick()
                }
            )
        }
    }
}

/**
 * Devuelve una acción de eliminar que borra de inmediato y muestra un aviso con "Deshacer".
 * Si se elimina otro elemento mientras el aviso sigue visible, el anterior se descarta
 * (ese borrado queda firme) y se muestra el nuevo.
 */
@Composable
fun rememberUndoableDelete(
    snackbarHostState: SnackbarHostState,
    message: String,
    onDelete: (String) -> Unit,
    onRestore: (String) -> Unit
): (String) -> Unit {
    val scope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.common_undo)
    val currentMessage by rememberUpdatedState(message)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnRestore by rememberUpdatedState(onRestore)

    return remember(snackbarHostState, scope, undoLabel) {
        { id: String ->
            currentOnDelete(id)
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = currentMessage,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) currentOnRestore(id)
            }
            Unit
        }
    }
}
