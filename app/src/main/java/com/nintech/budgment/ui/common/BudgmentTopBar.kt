package com.nintech.budgment.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nintech.budgment.R
import com.nintech.budgment.ui.theme.BudgmentTheme
import com.nintech.budgment.ui.theme.ChromeBackground
import com.nintech.budgment.ui.theme.ChromeContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgmentTopBar(
    userName: String,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { menuExpanded = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_mark),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.header_greeting, userName), fontSize = 16.sp)
                }

                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.header_menu_settings)) },
                        onClick = { menuExpanded = false; onSettingsClick() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.header_menu_logout)) },
                        onClick = { menuExpanded = false; onLogoutClick() }
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = { /* no funcional por ahora */ }) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = stringResource(R.string.header_notifications_content_description)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ChromeBackground,
            titleContentColor = ChromeContent,
            actionIconContentColor = ChromeContent
        )
    )
}

@Preview(showBackground = true, name = "TopBar - Claro")
@Composable
private fun BudgmentTopBarPreview() {
    BudgmentTheme(darkTheme = false) {
        BudgmentTopBar(
            userName = "Juan",
            onSettingsClick = {},
            onLogoutClick = {}
        )
    }
}

@Preview(showBackground = true, name = "TopBar - Oscuro")
@Composable
private fun BudgmentTopBarDarkPreview() {
    BudgmentTheme(darkTheme = true) {
        BudgmentTopBar(
            userName = "Juan",
            onSettingsClick = {},
            onLogoutClick = {}
        )
    }
}