package com.nintech.budgment.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nintech.budgment.R
import com.nintech.budgment.data.repository.AuthError

@Composable
fun AuthError.toMessage(): String = when (this) {
    AuthError.USERNAME_TAKEN -> stringResource(R.string.auth_error_username_taken)
    AuthError.INVALID_CREDENTIALS -> stringResource(R.string.auth_error_invalid_credentials)
    AuthError.EMPTY_FIELDS -> stringResource(R.string.auth_error_empty_fields)
    AuthError.PASSWORDS_DO_NOT_MATCH -> stringResource(R.string.auth_error_passwords_do_not_match)
}