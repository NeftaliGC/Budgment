package com.nintech.budgment.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.attachments.AttachmentPolicy
import com.nintech.budgment.data.attachments.AttachmentRepository
import com.nintech.budgment.data.attachments.AttachmentSettings
import com.nintech.budgment.data.attachments.AttachmentUsage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhotosSettingsState(
    val retentionMonths: Int = AttachmentPolicy.DEFAULT_RETENTION_MONTHS,
    val usage: AttachmentUsage = AttachmentUsage(),
    /** Plazo elegido que borraría fotos ahora mismo: espera la confirmación del usuario. */
    val pendingMonths: Int? = null,
    val pendingDeletions: Int = 0
)

@HiltViewModel
class PhotosSettingsViewModel @Inject constructor(
    private val settings: AttachmentSettings,
    private val repository: AttachmentRepository
) : ViewModel() {

    private val pending = MutableStateFlow<Pair<Int, Int>?>(null)

    val state: StateFlow<PhotosSettingsState> =
        combine(settings.retentionMonths, repository.usage, pending) { months, usage, waiting ->
            PhotosSettingsState(months, usage, waiting?.first, waiting?.second ?: 0)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PhotosSettingsState())

    /** Acortar el plazo puede borrar fotos en el acto: en ese caso se pregunta antes. */
    fun onRetentionSelected(months: Int) {
        viewModelScope.launch {
            val deletions = repository.countExpiredWith(months)
            if (deletions > 0) pending.value = months to deletions else apply(months)
        }
    }

    fun onDeletionConfirmed() {
        val months = pending.value?.first ?: return
        pending.value = null
        viewModelScope.launch { apply(months) }
    }

    fun onDeletionDismissed() {
        pending.value = null
    }

    private suspend fun apply(months: Int) {
        settings.setRetentionMonths(months)
        repository.cleanUp()
    }
}
