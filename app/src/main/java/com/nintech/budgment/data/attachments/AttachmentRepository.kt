package com.nintech.budgment.data.attachments

import com.nintech.budgment.data.local.dao.AttachmentDao
import com.nintech.budgment.data.local.entity.AttachmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Cuántas fotos hay guardadas y cuánto ocupan. */
data class AttachmentUsage(val count: Int = 0, val bytes: Long = 0)

@Singleton
class AttachmentRepository @Inject constructor(
    private val dao: AttachmentDao,
    private val store: AttachmentStore,
    private val settings: AttachmentSettings
) {
    suspend fun getByTransaction(transactionId: String): List<AttachmentEntity> =
        dao.getByTransaction(transactionId)

    fun file(attachment: AttachmentEntity): File = store.file(attachment.filePath)

    /** Movimientos que tienen fotos, para ofrecer «Ver fotos» en las listas. */
    val transactionIdsWithPhotos: Flow<Set<String>> = dao.observeTransactionIds().map { it.toSet() }

    /** Rutas de las fotos de un movimiento, de la más antigua a la más reciente. */
    suspend fun photoPaths(transactionId: String): List<String> = withContext(Dispatchers.IO) {
        dao.getByTransaction(transactionId).map { file(it) }.filter { it.exists() }.map { it.path }
    }

    /** Se recalcula cada vez que cambia el número de fotos. */
    val usage: Flow<AttachmentUsage> = dao.observeCount().map { count ->
        withContext(Dispatchers.IO) { AttachmentUsage(count, store.storedFiles().sumOf { it.length() }) }
    }

    /**
     * Aplica al movimiento [transactionId] los cambios hechos en el formulario: guarda las fotos
     * nuevas ([added], aún en la carpeta temporal) y borra las quitadas.
     */
    suspend fun applyChanges(transactionId: String, added: List<File>, removed: List<AttachmentEntity>) =
        withContext(Dispatchers.IO) {
            delete(removed)
            added.filter { it.exists() }.forEach { pending ->
                val id = UUID.randomUUID().toString()
                // Primero el archivo: una fila sin archivo se vería como una foto rota.
                val name = store.commit(pending, id)
                dao.insert(AttachmentEntity(id = id, transactionId = transactionId, filePath = name))
            }
        }

    private suspend fun delete(attachments: List<AttachmentEntity>) {
        if (attachments.isEmpty()) return
        // SQLite admite un número limitado de parámetros por consulta.
        attachments.chunked(500).forEach { chunk -> dao.deleteByIds(chunk.map { it.id }) }
        attachments.forEach { store.delete(it.filePath) }
    }

    /** Borra todas las fotos de un usuario; se usa al eliminar su cuenta. */
    suspend fun deleteAllOfUser(userId: String) = withContext(Dispatchers.IO) {
        delete(dao.getByUser(userId))
    }

    /** Fotos que se borrarían ahora mismo si el plazo fuera de [months] meses. */
    suspend fun countExpiredWith(months: Int, now: Long = System.currentTimeMillis()): Int {
        val cutoff = AttachmentPolicy.retentionCutoff(now, months) ?: return 0
        return dao.getOlderThan(cutoff).size
    }

    /**
     * Limpieza periódica. Borra:
     * - las fotos que ya cumplieron el plazo elegido por el usuario;
     * - las de movimientos eliminados hace más de un día (ya no se puede deshacer);
     * - los registros sin archivo (p. ej. tras restaurar un respaldo sin fotos) y los archivos
     *   sin registro.
     *
     * Devuelve cuántas fotos borró.
     */
    suspend fun cleanUp(now: Long = System.currentTimeMillis()): Int = withContext(Dispatchers.IO) {
        val expired = AttachmentPolicy.retentionCutoff(now, settings.retentionMonths.first())
            ?.let { dao.getOlderThan(it) }
            .orEmpty()
        val ofDeleted = dao.getOfTransactionsDeletedBefore(now - TimeUnit.DAYS.toMillis(1))
        val toDelete = (expired + ofDeleted).distinctBy { it.id }
        delete(toDelete)

        val remaining = dao.getAll()
        val missingFile = remaining.filterNot { store.file(it.filePath).exists() }
        delete(missingFile)

        // Margen de una hora: una foto recién guardada puede tener archivo y aún no registro.
        val known = remaining.mapTo(HashSet()) { it.filePath }
        store.storedFiles()
            .filter { it.name !in known && it.lastModified() < now - TimeUnit.HOURS.toMillis(1) }
            .forEach { it.delete() }
        store.clearStaleCaptures(olderThan = now - TimeUnit.DAYS.toMillis(1))

        toDelete.size
    }
}
