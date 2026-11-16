package com.nintech.budgment.ui.settings

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.backup.BackupError
import com.nintech.budgment.data.backup.BackupException
import com.nintech.budgment.data.backup.BackupManager
import com.nintech.budgment.data.backup.RestorePreview
import com.nintech.budgment.security.AppLockManager
import com.nintech.budgment.security.LockState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Qué muestra la sección de respaldo además de sus botones. */
sealed interface BackupStep {
    data object Idle : BackupStep

    /** Pide la contraseña del respaldo nuevo. */
    data object ExportPassword : BackupStep

    data object Exporting : BackupStep
    data object ExportDone : BackupStep
    data object ExportFailed : BackupStep

    /** Pide la contraseña del respaldo elegido. [checking]: se está descifrando. */
    data class RestorePassword(
        val createdAt: Long,
        val checking: Boolean = false,
        val wrongPassword: Boolean = false
    ) : BackupStep

    data class RestoreConfirm(val preview: RestorePreview) : BackupStep
    data object Restoring : BackupStep

    /** [error] null: no se pudo leer el archivo. */
    data class RestoreFailed(val error: BackupError?) : BackupStep
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    appLock: AppLockManager
) : ViewModel() {

    private val _step = MutableStateFlow<BackupStep>(BackupStep.Idle)
    val step: StateFlow<BackupStep> = _step.asStateFlow()

    /**
     * Al volver del selector de archivos la app puede quedar bloqueada: los diálogos esperan a
     * que el usuario la desbloquee para no aparecer encima de la pantalla de bloqueo.
     */
    val unlocked: StateFlow<Boolean> = appLock.state
        .map { it == LockState.UNLOCKED }
        .stateIn(viewModelScope, SharingStarted.Eagerly, appLock.state.value == LockState.UNLOCKED)

    private var exportPassword: CharArray? = null
    private var restoreSource: Uri? = null

    fun dismiss() {
        exportPassword = null
        restoreSource = null
        backupManager.discardRestore()
        _step.value = BackupStep.Idle
    }

    // Crear un respaldo

    fun onCreateClick() {
        _step.value = BackupStep.ExportPassword
    }

    /** La pantalla abre a continuación el selector para decidir dónde guardar el archivo. */
    fun onExportPasswordChosen(password: String) {
        exportPassword = password.toCharArray()
        _step.value = BackupStep.Idle
    }

    /** Resultado del selector; [target] null si el usuario lo canceló. */
    fun onExportFilePicked(target: Uri?) {
        val password = exportPassword
        exportPassword = null
        if (target == null) {
            _step.value = BackupStep.Idle
            return
        }
        if (password == null) {
            // El sistema cerró la app mientras el selector estaba abierto y la contraseña se perdió.
            backupManager.discardTarget(target)
            _step.value = BackupStep.ExportFailed
            return
        }
        _step.value = BackupStep.Exporting
        viewModelScope.launch {
            _step.value = try {
                backupManager.export(target, password)
                BackupStep.ExportDone
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo crear el respaldo", e)
                BackupStep.ExportFailed
            }
        }
    }

    // Restaurar un respaldo

    /** Resultado del selector; [source] null si el usuario lo canceló. */
    fun onRestoreFilePicked(source: Uri?) {
        if (source == null) return
        restoreSource = source
        viewModelScope.launch {
            _step.value = try {
                BackupStep.RestorePassword(createdAt = backupManager.readHeader(source).createdAt)
            } catch (e: Exception) {
                failure(e)
            }
        }
    }

    fun onRestorePasswordEntered(password: String) {
        val source = restoreSource ?: return
        val current = _step.value as? BackupStep.RestorePassword ?: return
        if (current.checking) return
        _step.value = current.copy(checking = true, wrongPassword = false)
        viewModelScope.launch {
            _step.value = try {
                BackupStep.RestoreConfirm(backupManager.prepareRestore(source, password.toCharArray()))
            } catch (e: BackupException) {
                // La contraseña se puede volver a intentar sin elegir el archivo otra vez.
                if (e.error == BackupError.WRONG_PASSWORD) current.copy(checking = false, wrongPassword = true)
                else failure(e)
            } catch (e: Exception) {
                failure(e)
            }
        }
    }

    fun onRestoreConfirmed() {
        if (_step.value !is BackupStep.RestoreConfirm) return
        _step.value = BackupStep.Restoring
        backupManager.applyRestoreAndRestart()
    }

    private fun failure(e: Exception): BackupStep {
        Log.w(TAG, "No se pudo abrir el respaldo", e)
        restoreSource = null
        return BackupStep.RestoreFailed((e as? BackupException)?.error)
    }

    private companion object {
        const val TAG = "BackupViewModel"
    }
}
