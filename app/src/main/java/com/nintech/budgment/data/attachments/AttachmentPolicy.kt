package com.nintech.budgment.data.attachments

import java.util.Calendar
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Reglas de las fotos de recibos, pensadas para que ocupen poco: se reducen al guardarlas, hay
 * un máximo por movimiento y se borran solas pasado el plazo que elija el usuario.
 */
object AttachmentPolicy {
    const val MAX_PER_TRANSACTION = 3

    /** Lado mayor de la foto guardada: suficiente para leer un recibo. */
    const val MAX_SIDE_PX = 1600
    const val JPEG_QUALITY = 80

    /** Meses que se conserva una foto; [KEEP_FOREVER] = no borrar. */
    const val KEEP_FOREVER = 0
    val RETENTION_OPTIONS_MONTHS = listOf(3, 6, 12, KEEP_FOREVER)
    const val DEFAULT_RETENTION_MONTHS = 3

    /**
     * Las fotos tomadas antes de este instante ya cumplieron el plazo. null = se conservan todas.
     * Se cuenta en meses de calendario: con 3 meses, una foto del 16 de agosto se borra el 16 de
     * noviembre.
     */
    fun retentionCutoff(now: Long, months: Int): Long? {
        if (months <= 0) return null
        return Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.MONTH, -months)
        }.timeInMillis
    }

    /** Tamaño al que se reduce una imagen para que su lado mayor no pase de [maxSide]. Nunca la agranda. */
    fun scaledSize(width: Int, height: Int, maxSide: Int = MAX_SIDE_PX): Pair<Int, Int> {
        val longest = max(width, height)
        if (longest <= maxSide) return width to height
        val factor = maxSide.toDouble() / longest
        return max(1, (width * factor).roundToInt()) to max(1, (height * factor).roundToInt())
    }

    /**
     * Factor (potencia de 2) con el que se puede decodificar la imagen sin bajar de [maxSide]:
     * evita cargar en memoria una foto de 50 megapíxeles para luego reducirla.
     */
    fun sampleSize(width: Int, height: Int, maxSide: Int = MAX_SIDE_PX): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxSide) sample *= 2
        return sample
    }
}
