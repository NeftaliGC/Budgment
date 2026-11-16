package com.nintech.budgment.data.attachments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Carpeta, dentro de `filesDir`, con las fotos ya guardadas. Queda fuera del respaldo automático de Android. */
const val ATTACHMENTS_DIR = "attachments"

/**
 * Archivos de las fotos de recibos.
 *
 * - Definitivas: `filesDir/attachments/<id>.jpg`. En la base de datos solo se guarda el nombre,
 *   así un respaldo sirve en otro teléfono.
 * - En curso (recién tomadas, de un formulario aún sin guardar): `cacheDir/captures/`.
 */
@Singleton
class AttachmentStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val directory: File get() = File(context.filesDir, ATTACHMENTS_DIR).apply { mkdirs() }
    private val captures: File get() = File(context.cacheDir, CAPTURES_DIR).apply { mkdirs() }

    fun file(name: String): File = File(directory, name)

    /** Archivo vacío donde la cámara dejará la foto, y la dirección que se le pasa. */
    fun newCapture(): Pair<File, Uri> {
        val file = File(captures, "camara-${UUID.randomUUID()}.jpg")
        file.createNewFile()
        return file to FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    /**
     * Convierte la foto que dejó la cámara en una copia reducida y enderezada, y borra la
     * original. Devuelve null si la cámara no dejó una imagen válida.
     */
    fun process(capture: File): File? {
        try {
            if (capture.length() == 0L) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(capture.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val options = BitmapFactory.Options().apply {
                inSampleSize = AttachmentPolicy.sampleSize(bounds.outWidth, bounds.outHeight)
            }
            val decoded = BitmapFactory.decodeFile(capture.path, options) ?: return null
            val (width, height) = AttachmentPolicy.scaledSize(decoded.width, decoded.height)
            // La cámara guarda la foto «acostada» y anota el giro aparte: aquí se aplica de verdad.
            val matrix = Matrix().apply {
                postScale(width.toFloat() / decoded.width, height.toFloat() / decoded.height)
                postRotate(rotationOf(capture))
            }
            val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            val target = File(captures, "foto-${UUID.randomUUID()}.jpg")
            target.outputStream().use { result.compress(Bitmap.CompressFormat.JPEG, AttachmentPolicy.JPEG_QUALITY, it) }
            if (result !== decoded) decoded.recycle()
            result.recycle()
            return target
        } catch (_: Exception) {
            return null
        } catch (_: OutOfMemoryError) {
            return null
        } finally {
            capture.delete()
        }
    }

    private fun rotationOf(file: File): Float =
        when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

    /** Mueve una foto en curso a su lugar definitivo y devuelve el nombre con que queda. */
    fun commit(pending: File, id: String): String {
        val name = "$id.jpg"
        val target = file(name)
        if (!pending.renameTo(target)) {
            pending.copyTo(target, overwrite = true)
            pending.delete()
        }
        return name
    }

    fun delete(name: String) {
        file(name).delete()
    }

    fun storedFiles(): List<File> = directory.listFiles { file -> file.isFile }?.toList().orEmpty()

    /** Borra las fotos en curso que quedaron de un formulario que nunca se guardó. */
    fun clearStaleCaptures(olderThan: Long) {
        captures.listFiles()?.filter { it.lastModified() < olderThan }?.forEach { it.delete() }
    }

    private companion object {
        const val CAPTURES_DIR = "captures"
    }
}
