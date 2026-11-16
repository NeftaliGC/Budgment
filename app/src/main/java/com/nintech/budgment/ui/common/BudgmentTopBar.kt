package com.nintech.budgment.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nintech.budgment.R
import com.nintech.budgment.ui.theme.BudgmentTheme
import com.nintech.budgment.ui.theme.ChromeBackground
import com.nintech.budgment.ui.theme.ChromeContent

/**
 * Barra superior de las pestañas principales.
 *
 * @param title título de la pestaña. Si es null (Inicio) se muestra el logo con el saludo.
 * @param actions acciones propias de la pestaña; van antes del menú de perfil.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgmentTopBar(
    title: String?,
    userName: String,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onStatisticsClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            if (title != null) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_mark),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (userName.isBlank()) {
                            stringResource(R.string.app_name)
                        } else {
                            stringResource(R.string.header_greeting, userName)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            actions()
            ProfileMenu(
                onStatisticsClick = onStatisticsClick,
                onSettingsClick = onSettingsClick,
                onLogoutClick = onLogoutClick
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ChromeBackground,
            titleContentColor = ChromeContent,
            actionIconContentColor = ChromeContent
        )
    )
}

@Composable
private fun ProfileMenu(onStatisticsClick: () -> Unit, onSettingsClick: () -> Unit, onLogoutClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = stringResource(R.string.header_profile_content_description)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.header_menu_statistics)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_estadisticas), contentDescription = null, modifier = Modifier.size(24.dp)) },
                onClick = {
                    expanded = false
                    onStatisticsClick()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.header_menu_settings)) },
                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSettingsClick()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.header_menu_logout)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                onClick = {
                    expanded = false
                    onLogoutClick()
                }
            )
        }
    }
}

@Preview(showBackground = true, name = "TopBar - Inicio")
@Composable
private fun BudgmentTopBarHomePreview() {
    BudgmentTheme(darkTheme = false) {
        BudgmentTopBar(title = null, userName = "Juan", onSettingsClick = {}, onLogoutClick = {})
    }
}

@Preview(showBackground = true, name = "TopBar - Pestaña")
@Composable
private fun BudgmentTopBarTabPreview() {
    BudgmentTheme(darkTheme = true) {
        BudgmentTopBar(title = "Movimientos", userName = "Juan", onSettingsClick = {}, onLogoutClick = {})
    }
}
