package com.nintech.budgment.ui.transfers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.repository.AccountRepository
import com.nintech.budgment.data.repository.ExchangeRateRepository
import com.nintech.budgment.data.repository.TransferRepository
import com.nintech.budgment.data.session.SessionManager
import com.nintech.budgment.ui.common.centsToInputText
import com.nintech.budgment.ui.common.parseAmountToCents
import com.nintech.budgment.ui.transactions.AmountError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransferFormState(
    val fromAccountId: String? = null,
    val toAccountId: String? = null,
    /** Lo que sale del origen, en su moneda. */
    val amountText: String = "",
    /** Lo que llega al destino, en su moneda. Solo se pide si las monedas difieren. */
    val amountInText: String = "",
    /** El usuario escribió el monto que llega: ya no se recalcula con el tipo de cambio. */
    val amountInEdited: Boolean = false,
    /** No hubo tipo de cambio para sugerir el monto que llega. */
    val rateMissing: Boolean = false,
    val dateMillis: Long = System.currentTimeMillis(),
    val description: String = "",
    val accounts: List<AccountEntity> = emptyList(),
    /** Al editar: cuenta y monto que ya salieron, para calcular el saldo resultante sin contarlos dos veces. */
    val originalFromAccountId: String? = null,
    val originalAmountOut: Long = 0,
    val amountError: AmountError? = null,
    val amountInError: AmountError? = null,
    val sameAccountError: Boolean = false,
    val isEditing: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = true
) {
    val fromAccount: AccountEntity? get() = accounts.firstOrNull { it.id == fromAccountId }
    val toAccount: AccountEntity? get() = accounts.firstOrNull { it.id == toAccountId }

    val currenciesDiffer: Boolean
        get() {
            val from = fromAccount ?: return false
            val to = toAccount ?: return false
            return from.currency != to.currency
        }

    /** Saldo del origen tras la transferencia, o null si el monto aún no es válido. */
    val fromBalanceAfter: Long?
        get() {
            val from = fromAccount ?: return null
            val cents = parseAmountToCents(amountText)?.takeIf { it > 0 } ?: return null
            val alreadyOut = if (from.id == originalFromAccountId) originalAmountOut else 0
            return from.balanceCache + alreadyOut - cents
        }
}

@HiltViewModel
class TransferFormViewModel @Inject constructor(
    private val transferRepository: TransferRepository,
    private val accountRepository: AccountRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transferId: String? = savedStateHandle.get<String>("transferId")?.ifBlank { null }
    private val presetToAccountId: String? = savedStateHandle.get<String>("toAccountId")?.ifBlank { null }

    private val _state = MutableStateFlow(TransferFormState(isEditing = transferId != null, toAccountId = presetToAccountId))
    val state: StateFlow<TransferFormState> = _state.asStateFlow()

    private var conversionJob: Job? = null

    init {
        viewModelScope.launch {
            val userId = sessionManager.currentUserId.filterNotNull().first()

            // Se carga antes de observar las cuentas para que el formulario aparezca ya relleno.
            transferId?.let { transferRepository.getTransfer(it) }?.let { existing ->
                val amountOut = -existing.outgoing.amount
                _state.update {
                    it.copy(
                        fromAccountId = existing.outgoing.accountId,
                        toAccountId = existing.incoming.accountId,
                        amountText = centsToInputText(amountOut),
                        amountInText = centsToInputText(existing.incoming.amount),
                        // Lo guardado manda: no se reemplaza por el tipo de cambio de hoy.
                        amountInEdited = true,
                        dateMillis = existing.outgoing.date,
                        description = existing.outgoing.description.orEmpty(),
                        originalFromAccountId = existing.outgoing.accountId,
                        originalAmountOut = amountOut
                    )
                }
            }

            // Se observan (no se leen una vez) para que una cuenta creada desde aquí aparezca al volver.
            accountRepository.observeAccounts(userId).collect { accounts ->
                _state.update { s ->
                    val from = s.fromAccountId?.takeIf { id -> accounts.any { it.id == id } }
                        // Si el destino ya viene elegido, el origen propuesto es otra cuenta.
                        ?: accounts.firstOrNull { it.id != s.toAccountId }?.id
                        ?: accounts.firstOrNull()?.id
                    val to = s.toAccountId?.takeIf { id -> accounts.any { it.id == id } }
                        ?: accounts.firstOrNull { it.id != from }?.id
                    s.copy(accounts = accounts, fromAccountId = from, toAccountId = to, isLoading = false)
                }
                refreshSuggestedAmountIn()
            }
        }
    }

    fun onFromSelected(accountId: String) {
        // Elegir como origen la cuenta destino equivale a intercambiarlas.
        updateAccounts { s ->
            accountId to (if (accountId == s.toAccountId) s.fromAccountId else s.toAccountId)
        }
    }

    fun onToSelected(accountId: String) {
        updateAccounts { s ->
            (if (accountId == s.fromAccountId) s.toAccountId else s.fromAccountId) to accountId
        }
    }

    fun onSwap() {
        updateAccounts { s -> s.toAccountId to s.fromAccountId }
    }

    /**
     * Cambia origen y destino. Si con ello cambia el par de monedas, el monto que llega escrito
     * antes ya no aplica y se vuelve a sugerir con el tipo de cambio.
     */
    private fun updateAccounts(pick: (TransferFormState) -> Pair<String?, String?>) {
        _state.update { s ->
            val (from, to) = pick(s)
            val next = s.copy(fromAccountId = from, toAccountId = to, sameAccountError = false)
            val pairChanged = s.fromAccount?.currency != next.fromAccount?.currency ||
                s.toAccount?.currency != next.toAccount?.currency
            if (pairChanged) next.copy(amountInEdited = false, amountInText = "", amountInError = null) else next
        }
        refreshSuggestedAmountIn()
    }

    fun onAmountChange(value: String) {
        _state.update { it.copy(amountText = value, amountError = null) }
        refreshSuggestedAmountIn()
    }

    fun onAmountInChange(value: String) {
        // Si lo vacía, vuelve a sugerirse con el tipo de cambio.
        _state.update { it.copy(amountInText = value, amountInEdited = value.isNotBlank(), amountInError = null) }
        if (value.isBlank()) refreshSuggestedAmountIn()
    }

    fun onDateChange(dateMillis: Long) {
        _state.update { it.copy(dateMillis = dateMillis) }
    }

    fun onDescriptionChange(value: String) {
        _state.update { it.copy(description = value) }
    }

    /** Propone el monto que llega convirtiendo el que sale, mientras el usuario no lo haya escrito. */
    private fun refreshSuggestedAmountIn() {
        val s = _state.value
        if (s.amountInEdited || !s.currenciesDiffer) {
            if (!s.currenciesDiffer) _state.update { it.copy(rateMissing = false) }
            return
        }
        val from = s.fromAccount ?: return
        val to = s.toAccount ?: return
        val cents = parseAmountToCents(s.amountText)?.takeIf { it > 0 }

        conversionJob?.cancel()
        if (cents == null) {
            _state.update { it.copy(amountInText = "", rateMissing = false) }
            return
        }
        conversionJob = viewModelScope.launch {
            val converted = exchangeRateRepository.convert(cents, from.currency, to.currency)
            _state.update {
                if (it.amountInEdited) it
                else it.copy(
                    amountInText = converted?.let(::centsToInputText).orEmpty(),
                    rateMissing = converted == null
                )
            }
        }
    }

    fun save() {
        val s = _state.value
        val from = s.fromAccount
        val to = s.toAccount
        val cents = parseAmountToCents(s.amountText)
        val amountError = amountErrorFor(s.amountText, cents)

        val centsIn = if (s.currenciesDiffer) parseAmountToCents(s.amountInText) else cents
        val amountInError = if (s.currenciesDiffer) amountErrorFor(s.amountInText, centsIn) else null
        val sameAccount = from != null && from.id == to?.id

        if (from == null || to == null || sameAccount || amountError != null || amountInError != null ||
            cents == null || centsIn == null
        ) {
            _state.update {
                it.copy(amountError = amountError, amountInError = amountInError, sameAccountError = sameAccount)
            }
            return
        }

        val description = s.description.trim().ifBlank { null }
        viewModelScope.launch {
            if (transferId != null) {
                transferRepository.updateTransfer(transferId, from.id, to.id, cents, centsIn, description, s.dateMillis)
            } else {
                val userId = sessionManager.currentUserId.filterNotNull().first()
                transferRepository.createTransfer(userId, from.id, to.id, cents, centsIn, description, s.dateMillis)
            }
            _state.update { it.copy(isSaved = true) }
        }
    }

    private fun amountErrorFor(text: String, cents: Long?): AmountError? = when {
        text.isBlank() -> AmountError.REQUIRED
        cents == null || cents <= 0 -> AmountError.INVALID
        else -> null
    }
}
