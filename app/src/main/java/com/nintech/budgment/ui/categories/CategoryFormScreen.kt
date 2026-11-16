package com.nintech.budgment.ui.categories

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.ui.theme.BudgmentTheme
import com.nintech.budgment.ui.common.IconPicker
import com.nintech.budgment.ui.common.ColorPicker
import com.nintech.budgment.ui.common.SurfaceSystemBars

@Composable
fun CategoryFormScreen(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CategoryFormViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    if (viewModel != null) {
        val state by viewModel.state.collectAsState()

        LaunchedEffect(state.isSaved) {
            if (state.isSaved) onSaved()
        }

        CategoryFormScreen(
            state = state,
            onNameChange = viewModel::onNameChange,
            onTypeChange = viewModel::onTypeChange,
            onIconChange = viewModel::onIconChange,
            onColorChange = viewModel::onColorChange,
            onSaveClick = viewModel::save,
            onBackClick = onBackClick
        )
    } else {
        CategoryFormScreen(
            state = CategoryFormState(),
            onNameChange = {},
            onTypeChange = {},
            onIconChange = {},
            onColorChange = {},
            onSaveClick = {},
            onBackClick = onBackClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormScreen(
    state: CategoryFormState,
    onNameChange: (String) -> Unit,
    onTypeChange: (CategoryType) -> Unit,
    onIconChange: (String?) -> Unit,
    onColorChange: (String?) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var typeMenuExpanded by remember { mutableStateOf(false) }

    SurfaceSystemBars()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isEditing) R.string.category_form_title_edit else R.string.category_form_title_new)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.category_form_name_label)) },
                isError = state.nameError,
                supportingText = if (state.nameError) {
                    { Text(stringResource(R.string.form_error_name_required)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            ExposedDropdownMenuBox(expanded = typeMenuExpanded, onExpandedChange = { typeMenuExpanded = it }) {
                OutlinedTextField(
                    value = state.type.label(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.category_form_type_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                    CategoryType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label()) },
                            onClick = { onTypeChange(type); typeMenuExpanded = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(15.dp))

            val accentColor = if (state.type == CategoryType.EXPENSE) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.tertiary
            }
            Text(
                text = stringResource(R.string.category_form_icon_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            IconPicker(
                items = CategoryIcon.entries,
                icon = { it.icon },
                label = { stringResource(it.labelRes) },
                isSelected = { state.icon == it.key },
                onClick = { onIconChange(it.key) },
                accentColor = accentColor
            )

            Spacer(Modifier.height(15.dp))

            Text(
                text = stringResource(R.string.category_form_color_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.category_form_color_auto_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            ColorPicker(
                colors = CategoryColorPalette,
                selectedHex = state.color,
                onSelect = onColorChange,
                defaultLabel = stringResource(R.string.category_form_color_default_content_description)
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Text(stringResource(R.string.category_form_save_button))
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoryFormScreenPreview() {
    BudgmentTheme {
        CategoryFormScreen(
            state = CategoryFormState(
                name = "Comida",
                type = CategoryType.EXPENSE,
                isEditing = false
            ),
            onNameChange = {},
            onTypeChange = {},
            onIconChange = {},
            onColorChange = {},
            onSaveClick = {},
            onBackClick = {}
        )
    }
}