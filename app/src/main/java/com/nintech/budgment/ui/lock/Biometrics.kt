package com.nintech.budgment.ui.lock

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability { AVAILABLE, NOT_ENROLLED, UNAVAILABLE }

/**
 * Huella o rostro, incluidos los sensores de clase «débil» (muchos desbloqueos faciales): el
 * bloqueo es una barrera de interfaz, no descifra datos, así que no hace falta clase fuerte.
 *
 * Solo desde Android 9: antes, la librería muestra su propio diálogo de huella con AppCompat,
 * y la app no usa un tema AppCompat. En esos teléfonos queda el PIN.
 */
fun biometricAvailability(context: Context): BiometricAvailability {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return BiometricAvailability.UNAVAILABLE
    return when (BiometricManager.from(context).canAuthenticate(BIOMETRIC_WEAK)) {
        BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
        else -> BiometricAvailability.UNAVAILABLE
    }
}

tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

/** Muestra el diálogo del sistema. [onFailure] se llama si se cancela o no se puede usar. */
fun FragmentActivity.authenticateWithBiometrics(
    title: String,
    negativeButton: String,
    onSuccess: () -> Unit,
    onFailure: () -> Unit = {}
) {
    val prompt = BiometricPrompt(
        this,
        ContextCompat.getMainExecutor(this),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            // Errores definitivos (cancelar, demasiados intentos, sin sensor). Un dedo no
            // reconocido llega a onAuthenticationFailed y el diálogo sigue abierto.
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFailure()
        }
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText(negativeButton)
            .setAllowedAuthenticators(BIOMETRIC_WEAK)
            .build()
    )
}
