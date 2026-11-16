package com.nintech.budgment.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.repository.AccountRepository
import com.nintech.budgment.data.repository.CategoryRepository
import com.nintech.budgment.data.repository.TransactionRepository
import com.nintech.budgment.data.repository.TransferRepository
import com.nintech.budgment.data.session.SessionManager
import com.nintech.budgment.ui.transactions.buildTransactionDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import javax.inject.Inject

sealed interface ExportStep {
    data object Idle : ExportStep
    data object Working : ExportStep
    data class Done(val count: Int) : ExportStep
    data object Failed : ExportStep
}

/** Exporta los movimientos del usuario a un archivo CSV que él guarda donde quiera. */
@HiltViewModel
class ExportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transferRepository: TransferRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _step = MutableStateFlow<ExportStep>(ExportStep.Idle)
    val step: StateFlow<ExportStep> = _step.asStateFlow()

    fun dismiss() {
        _step.value = ExportStep.Idle
    }

    /** Resultado del selector; [target] null si el usuario lo canceló. */
    fun onFilePicked(target: Uri?, labels: CsvLabels) {
        if (target == null) return
        _step.value = ExportStep.Working
        viewModelScope.launch {
            _step.value = try {
                val userId = sessionManager.currentUserId.first() ?: error("Sin sesión")
                // Igual que la lista de Movimientos: incluye el historial de cuentas y categorías eliminadas.
                val items = buildTransactionDetails(
                    transactionRepository.observeTransactions(userId).first(),
                    accountRepository.observeAccountsIncludingDeleted(userId).first(),
                    categoryRepository.observeCategoriesIncludingDeleted(userId).first(),
                    transferRepository.observeTransfers(userId).first()
                )
                withContext(Dispatchers.IO) {
                    val csv = buildTransactionsCsv(items, labels)
                    val output = context.contentResolver.openOutputStream(target)
                        ?: throw FileNotFoundException("No se pudo abrir el destino")
                    output.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                }
                ExportStep.Done(items.size)
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo exportar", e)
                runCatching { DocumentsContract.deleteDocument(context.contentResolver, target) }
                ExportStep.Failed
            }
        }
    }

    private companion object {
        const val TAG = "ExportViewModel"
    }
}
