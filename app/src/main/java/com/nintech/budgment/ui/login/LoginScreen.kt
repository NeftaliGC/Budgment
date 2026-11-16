package com.nintech.budgment.ui.login

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nintech.budgment.R
import com.nintech.budgment.ui.theme.BudgmentTheme
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nintech.budgment.ui.common.HeroSystemBars
import com.nintech.budgment.ui.common.toMessage
import com.nintech.budgment.ui.settings.RestoreBackupButton
import com.nintech.budgment.ui.theme.ChromeBackground
import com.nintech.budgment.ui.theme.ChromeContent

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel? = if (LocalInspectionMode.current) null else hiltViewModel(),
) {
    if (viewModel != null) {
        val state by viewModel.state.collectAsState()
        LoginContent(
            state = state,
            onUsernameChange = viewModel::onUsernameChange,
            onPasswordChange = viewModel::onPasswordChange,
            onRememberMeChange = viewModel::onRememberMeChange,
            onLoginClick = viewModel::login,
            onLoginSuccess = onLoginSuccess,
            onRegisterClick = onRegisterClick,
            modifier = modifier,
            restoreBackup = { RestoreBackupButton() }
        )
    } else {
        LoginContent(
            state = LoginUiState(),
            onUsernameChange = {},
            onPasswordChange = {},
            onRememberMeChange = {},
            onLoginClick = {},
            onLoginSuccess = onLoginSuccess,
            onRegisterClick = onRegisterClick,
            modifier = modifier
        )
    }
}

@Composable
fun LoginContent(
    state: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRememberMeChange: (Boolean) -> Unit,
    onLoginClick: () -> Unit,
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
    restoreBackup: @Composable () -> Unit = {}
) {
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.loginSuccess) {
        if (state.loginSuccess) onLoginSuccess()
    }

    HeroSystemBars()

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .background(ChromeBackground)
    ) {
        // Hero
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                // Altura mínima, no fija: con texto grande el encabezado crece en vez de recortarse.
                .heightIn(min = 116.dp)
                .padding(horizontal = 32.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_mark),
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    color = ChromeContent,
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.login_tagline),
                    color = ChromeContent.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Formulario
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 40.dp)
            ) {
                Text(
                    text = stringResource(R.string.login_welcome),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.login_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = state.username,
                    onValueChange = onUsernameChange,
                    label = { Text(stringResource(R.string.login_username_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
                    isError = state.errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = { Text(stringResource(R.string.login_password_label)) },
                    singleLine = true,
                    // "Listo" en el teclado inicia sesión, sin tener que buscar el botón.
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onLoginClick() }),
                    isError = state.errorMessage != null,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(
                                text = stringResource(
                                    if (passwordVisible) R.string.common_password_hide else R.string.common_password_show
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                state.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error.toMessage(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = state.rememberMe,
                        onCheckedChange = onRememberMeChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = stringResource(R.string.login_remember_me),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onLoginClick,
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.login_button), style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.login_no_account),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(
                        onClick = onRegisterClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text(
                            text = stringResource(R.string.login_register),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // En un teléfono nuevo no hay cuenta con la que entrar: el respaldo se restaura desde aquí.
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    restoreBackup()
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Login - Claro")
@Composable
private fun LoginScreenLightPreview() {
    BudgmentTheme(darkTheme = false) {
        LoginScreen(
            onLoginSuccess = {},
            onRegisterClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Login - Oscuro")
@Composable
private fun LoginScreenDarkPreview() {
    BudgmentTheme(darkTheme = true) {
        LoginScreen(
            onLoginSuccess = {},
            onRegisterClick = {}
        )
    }
}
