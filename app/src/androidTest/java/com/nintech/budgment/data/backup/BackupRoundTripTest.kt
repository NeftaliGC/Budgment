package com.nintech.budgment.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nintech.budgment.data.local.ALL_MIGRATIONS
import com.nintech.budgment.data.local.BudgmentDatabase
import com.nintech.budgment.data.local.DATABASE_VERSION
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.local.entity.UserEntity
import com.nintech.budgment.data.model.AccountType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Recorre el respaldo completo con una base de datos real: copiarla mientras está abierta,
 * cifrarla, descifrarla, dejarla pendiente y abrirla con Room tras el «reinicio».
 *
 * Usa bases y carpetas con nombres propios: nunca toca los datos de la app.
 */
@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val sourceName = "backup-test-source.db"
    private val targetName = "backup-test-target.db"
    private val photosName = "backup-test-attachments"
    private val photosDir = File(context.filesDir, photosName)
    private val workDir = File(context.cacheDir, "backup-test")
    private val password = "contraseña de prueba".toCharArray()

    private fun open(name: String) =
        Room.databaseBuilder(context, BudgmentDatabase::class.java, name).addMigrations(*ALL_MIGRATIONS).build()

    @Before
    @After
    fun clean() {
        context.deleteDatabase(sourceName)
        context.deleteDatabase(targetName)
        File(context.getDatabasePath(targetName).path + ".restore").delete()
        photosDir.deleteRecursively()
        File(context.filesDir, "$photosName.restore").deleteRecursively()
        workDir.deleteRecursively()
        workDir.mkdirs()
    }

    @Test
    fun unRespaldoRestauradoConservaLosDatosYAbreConRoom() = runBlocking {
        val source = open(sourceName)
        source.userDao().insert(
            UserEntity(id = "u1", name = "Ana", username = "ana", passwordHash = "hash", defaultCurrency = "MXN")
        )
        repeat(3) { i ->
            source.accountDao().insert(
                AccountEntity(
                    id = "a$i", userId = "u1", name = "Cuenta $i", accountType = AccountType.DEBIT,
                    currency = "MXN", balanceCache = 1000L * i
                )
            )
        }
        source.accountDao().softDelete("a2")

        // La base sigue abierta, como en la app: lo recién escrito aún puede estar en el -wal.
        val snapshot = File(workDir, "snapshot.db")
        val version = DatabaseSnapshot.create(context.getDatabasePath(sourceName), snapshot)
        assertEquals(DATABASE_VERSION, version)

        // La base original sigue funcionando después de copiarla.
        source.accountDao().updateBalanceCache("a0", 77)
        assertEquals(77L, source.accountDao().getById("a0")?.balanceCache)
        source.close()

        val backup = File(workDir, "respaldo.budgment")
        GZIPOutputStream(BackupCrypto.encrypt(backup.outputStream(), password, 123L, version, iterations = 10_000))
            .use { zip -> snapshot.inputStream().use { it.copyTo(zip) } }

        val restored = File(workDir, "restored.db")
        val (header, plain) = BackupCrypto.decrypt(backup.inputStream(), password)
        GZIPInputStream(plain).use { zip -> restored.outputStream().use { zip.copyTo(it) } }
        assertEquals(123L, header.createdAt)
        assertEquals(DATABASE_VERSION, header.schemaVersion)

        val info = DatabaseSnapshot.inspect(restored, DATABASE_VERSION)
        assertEquals(SnapshotInfo(schemaVersion = DATABASE_VERSION, accounts = 2, transactions = 0), info)

        // El destino ya tiene datos distintos, que la restauración debe reemplazar por completo.
        open(targetName).apply {
            userDao().insert(
                UserEntity(id = "otro", name = "Otro", username = "otro", passwordHash = "x", defaultCurrency = "USD")
            )
            close()
        }
        // Las fotos que había se reemplazan por las del respaldo.
        photosDir.mkdirs()
        File(photosDir, "vieja.jpg").writeText("vieja")
        val restoredPhotos = File(workDir, "fotos").apply { mkdirs() }
        File(restoredPhotos, "nueva.jpg").writeText("nueva")

        PendingRestore.stage(context, targetName, restored, restoredPhotos, photosName)
        assertTrue(PendingRestore.applyIfAny(context, targetName, photosName))
        assertFalse(PendingRestore.applyIfAny(context, targetName, photosName)) // ya no queda nada pendiente
        assertEquals(listOf("nueva.jpg"), photosDir.list()?.toList())

        // Room valida el esquema al abrir: si la copia no fuera fiel, fallaría aquí.
        val target = open(targetName)
        assertEquals("Ana", target.userDao().getById("u1")?.name)
        assertEquals(null, target.userDao().getById("otro"))
        // El cambio hecho después de la copia no forma parte del respaldo.
        assertEquals(0L, target.accountDao().getById("a0")?.balanceCache)
        assertEquals(1000L, target.accountDao().getById("a1")?.balanceCache)
        assertTrue(target.accountDao().getById("a2")?.deletedAt != null)
        target.close()
    }

    @Test
    fun unArchivoQueNoEsUnaBaseDeDatosSeRechaza() {
        val garbage = File(workDir, "basura.db").apply { writeBytes(ByteArray(4096) { it.toByte() }) }
        try {
            DatabaseSnapshot.inspect(garbage, DATABASE_VERSION)
            throw AssertionError("Se esperaba un error")
        } catch (e: BackupException) {
            assertEquals(BackupError.CORRUPT, e.error)
        }
    }
}
