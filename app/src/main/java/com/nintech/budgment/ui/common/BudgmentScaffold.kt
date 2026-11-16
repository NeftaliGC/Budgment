package com.nintech.budgment.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R

/**
 * Estructura común de las pestañas principales: barra superior, barra inferior y contenido.
 *
 * @param title título de la pestaña; null muestra el logo con el saludo (solo Inicio).
 * @param topBarActions acciones propias de la pestaña en la barra superior.
 */
@Composable
fun BudgmentScaffold(
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    title: String? = null,
    onSettingsClick: () -> Unit = {},
    onStatisticsClick: () -> Unit = {},
    topBarActions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    headerViewModel: HeaderViewModel? = if (LocalInspectionMode.current) null else hiltViewModel(),
    content: @Composable (PaddingValues) -> Unit
) {
    ChromeSystemBars()

    val userName = if (headerViewModel != null) {
        val name by headerViewModel.userName.collectAsState()
        name
    } else {
        stringResource(R.string.user_placeholder)
    }

    Scaffold(
        topBar = {
            BudgmentTopBar(
                title = title,
                userName = userName,
                onSettingsClick = onSettingsClick,
                onStatisticsClick = onStatisticsClick,
                onLogoutClick = { headerViewModel?.logout() },
                actions = topBarActions
            )
        },
        bottomBar = { BudgmentBottomBar(currentRoute = currentRoute, onNavigate = onNavigateToTab) },
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost,
        content = content
    )
}
