package com.nintech.budgment.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R

@Composable
fun BudgmentScaffold(
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    onSettingsClick: () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    headerViewModel: HeaderViewModel? = if (LocalInspectionMode.current) null else hiltViewModel(),
    content: @Composable (PaddingValues) -> Unit
) {
    ForceLightSystemBars()

    val userName = if (headerViewModel != null) {
        val name by headerViewModel.userName.collectAsState()
        name
    } else {
        stringResource(R.string.user_placeholder)
    }

    Scaffold(
        topBar = {
            BudgmentTopBar(
                userName = userName,
                onSettingsClick = onSettingsClick,
                onLogoutClick = { headerViewModel?.logout() }
            )
        },
        bottomBar = { BudgmentBottomBar(currentRoute = currentRoute, onNavigate = onNavigateToTab) },
        floatingActionButton = floatingActionButton,
        content = content
    )
}