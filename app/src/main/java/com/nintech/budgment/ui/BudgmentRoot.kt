package com.nintech.budgment.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.ui.navigation.BudgmentDestination
import com.nintech.budgment.ui.navigation.BudgmentNavHost

@Composable
fun BudgmentRoot(sessionViewModel: SessionViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()) {
    if (sessionViewModel != null) {
        when (val session = sessionViewModel.state.collectAsState().value) {
            SessionUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is SessionUiState.LoggedIn -> BudgmentNavHost(startDestination = BudgmentDestination.Home.route)
            SessionUiState.LoggedOut -> BudgmentNavHost(startDestination = BudgmentDestination.Login.route)
        }
    } else {
        BudgmentNavHost(startDestination = BudgmentDestination.Home.route)
    }
}