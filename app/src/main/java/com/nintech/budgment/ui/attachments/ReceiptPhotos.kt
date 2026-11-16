package com.nintech.budgment.ui.attachments

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nintech.budgment.R
import com.nintech.budgment.data.attachments.AttachmentPolicy
import com.nintech.budgment.ui.transactions.FormPhoto
import com.nintech.budgment.ui.transactions.PhotoError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val THUMBNAIL_PX = 256

/** Fotos del recibo de un movimiento: miniaturas, botón para tomar otra y visor a pantalla completa. */
@Composable
fun ReceiptPhotosField(
    photos: List<FormPhoto>,
    canAdd: Boolean,
    processing: Boolean,
    error: PhotoError?,
    retentionMonths: Int,
    onAddClick: () -> Unit,
    onRemove: (FormPhoto) -> Unit,
    modifier: Modifier = Modifier
) {
    // Se guarda la ruta y no la foto: sobrevive a girar la pantalla.
    var viewing by rememberSaveable { mutableStateOf<String?>(null) }

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.receipt_photos_label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.receipt_photos_count, photos.size, AttachmentPolicy.MAX_PER_TRANSACTION),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (photos.isNotEmpty() || processing) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                photos.forEachIndexed { index, photo ->
                    PhotoThumbnail(
                        path = photo.path,
                        description = stringResource(R.string.receipt_photos_item, index + 1),
                        onClick = { viewing = photo.path }
                    )
                }
                if (processing) {
                    Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onAddClick, enabled = canAdd) {
            Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.receipt_photos_add))
        }

        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    when (error) {
                        PhotoError.NO_CAMERA -> R.string.receipt_photos_error_no_camera
                        PhotoError.FAILED -> R.string.receipt_photos_error_failed
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = if (retentionMonths == AttachmentPolicy.KEEP_FOREVER) {
                stringResource(R.string.receipt_photos_note_forever)
            } else {
                pluralStringResource(R.plurals.receipt_photos_note, retentionMonths, retentionMonths)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    val index = photos.indexOfFirst { it.path == viewing }
    if (index >= 0) {
        ReceiptPhotoViewer(
            paths = photos.map { it.path },
            startIndex = index,
            onDismiss = { viewing = null },
            onDelete = { path ->
                viewing = null
                photos.firstOrNull { it.path == path }?.let(onRemove)
            }
        )
    }
}

@Composable
private fun PhotoThumbnail(path: String, description: String, onClick: () -> Unit) {
    val bitmap = rememberPhoto(path, THUMBNAIL_PX)
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(role = Role.Image, onClickLabel = stringResource(R.string.receipt_photos_open), onClick = onClick)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Las fotos a pantalla completa sobre fondo negro. Cada una se amplía pellizcando y se mueve
 * arrastrando; con varias, se pasa de una a otra con las flechas.
 *
 * [onDelete] null: solo se pueden ver. Si no, quitar una foto pide confirmación.
 */
@Composable
fun ReceiptPhotoViewer(
    paths: List<String>,
    onDismiss: () -> Unit,
    startIndex: Int = 0,
    onDelete: ((String) -> Unit)? = null
) {
    if (paths.isEmpty()) return
    var index by rememberSaveable { mutableStateOf(startIndex.coerceIn(paths.indices)) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val path = paths[index.coerceIn(paths.indices)]

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val bitmap = rememberPhoto(path, AttachmentPolicy.MAX_SIDE_PX)
        // Al cambiar de foto se vuelve al tamaño normal.
        var scale by remember(path) { mutableFloatStateOf(1f) }
        var offset by remember(path) { mutableStateOf(Offset.Zero) }
        var size by remember { mutableStateOf(IntSize.Zero) }

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(R.string.receipt_photos_item, index + 1),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { size = it }
                        .pointerInput(path) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                // La foto no se puede arrastrar fuera de la pantalla.
                                val maxX = size.width * (scale - 1) / 2
                                val maxY = size.height * (scale - 1) / 2
                                offset = Offset(
                                    (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    (offset.y + pan.y).coerceIn(-maxY, maxY)
                                )
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )
            } else {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            }
            Row(
                modifier = Modifier.fillMaxWidth().systemBarsPadding().padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, stringResource(R.string.receipt_photos_close), tint = Color.White)
                }
                if (onDelete != null) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, stringResource(R.string.receipt_photos_delete), tint = Color.White)
                    }
                }
            }
            if (paths.size > 1) {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { index-- }, enabled = index > 0) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            stringResource(R.string.receipt_photos_previous),
                            tint = if (index > 0) Color.White else Color.Gray
                        )
                    }
                    Text(
                        text = stringResource(R.string.receipt_photos_count, index + 1, paths.size),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    IconButton(onClick = { index++ }, enabled = index < paths.lastIndex) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            stringResource(R.string.receipt_photos_next),
                            tint = if (index < paths.lastIndex) Color.White else Color.Gray
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.receipt_photos_delete_confirm_title)) },
            text = { Text(stringResource(R.string.receipt_photos_delete_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(path)
                }) {
                    Text(stringResource(R.string.receipt_photos_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

/** Carga la foto fuera del hilo principal, reducida para que su lado mayor ronde [maxSide]. */
@Composable
private fun rememberPhoto(path: String, maxSide: Int): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path, maxSide) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = AttachmentPolicy.sampleSize(bounds.outWidth, bounds.outHeight, maxSide)
                }
                BitmapFactory.decodeFile(path, options)?.asImageBitmap()
            }.getOrNull()
        }
    }
    return bitmap
}
