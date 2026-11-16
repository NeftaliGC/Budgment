package com.nintech.budgment.ui.categories

import com.nintech.budgment.ui.common.EmptyState
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.ui.theme.BudgmentTheme
import com.nintech.budgment.ui.common.ColorIconTile
import com.nintech.budgment.ui.common.SurfaceSystemBars
import com.nintech.budgment.ui.common.RowActionsMenu
import com.nintech.budgment.ui.common.rememberUndoableDelete
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.pluralStringResource

@Composable
fun CategoriesListScreen(
    onBackClick: () -> Unit,
    onAddCategoryClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    viewModel: CategoriesViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    if (viewModel != null) {
        val uiState by viewModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        CategoriesListScreen(
            uiState = uiState,
            onBackClick = onBackClick,
            onAddCategoryClick = onAddCategoryClick,
            onCategoryClick = onCategoryClick,
            onDeleteCategory = rememberUndoableDelete(
                snackbarHostState = snackbarHostState,
                message = stringResource(R.string.categories_deleted_message),
                onDelete = viewModel::deleteCategory,
                onRestore = viewModel::restoreCategory
            ),
            snackbarHostState = snackbarHostState
        )
    } else {
        CategoriesListScreen(
            uiState = CategoriesUiState(),
            onBackClick = onBackClick,
            onAddCategoryClick = onAddCategoryClick,
            onCategoryClick = onCategoryClick,
            onDeleteCategory = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesListScreen(
    uiState: CategoriesUiState,
    onBackClick: () -> Unit,
    onAddCategoryClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    SurfaceSystemBars()

    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    uiState.categories.firstOrNull { it.id == pendingDeleteId }?.let { category ->
        val count = uiState.transactionCounts[category.id] ?: 0
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.categories_delete_confirm_title, category.name)) },
            text = { Text(pluralStringResource(R.plurals.categories_delete_confirm_message, count, count)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCategory(category.id)
                        pendingDeleteId = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCategoryClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.categories_add_content_description))
            }
        }
    ) { innerPadding ->
        if (!uiState.isLoading && uiState.categories.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.categories_empty_title),
                message = stringResource(R.string.categories_empty_subtitle),
                actionLabel = stringResource(R.string.categories_add_content_description),
                onAction = onAddCategoryClick,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp)
            ) {
                CategoryType.entries.forEach { type ->
                    val typeCategories = uiState.categories.filter { it.type == type }
                    if (typeCategories.isNotEmpty()) {
                        item(key = "header_${type.name}") {
                            Text(
                                text = stringResource(
                                    if (type == CategoryType.EXPENSE) R.string.categories_section_expense
                                    else R.string.categories_section_income
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }
                        items(typeCategories, key = { it.id }) { category ->
                            CategoryRow(
                                category = category,
                                onClick = { onCategoryClick(category.id) },
                                onDeleteClick = {
                                    // Con movimientos se avisa antes; sin ellos basta con "Deshacer".
                                    if ((uiState.transactionCounts[category.id] ?: 0) > 0) {
                                        pendingDeleteId = category.id
                                    } else {
                                        onDeleteCategory(category.id)
                                    }
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(category: CategoryEntity, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    val accentColor = when {
        parseHexColor(category.color) != null -> parseHexColor(category.color)!!
        category.type == CategoryType.EXPENSE -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }
    val icon = CategoryIcon.resolveIcon(category.icon)
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            ColorIconTile(color = accentColor, icon = icon, size = 36.dp, cornerRadius = 10.dp)
            Spacer(Modifier.width(12.dp))
            // El tipo ya lo dice el encabezado de la sección (Gastos / Ingresos).
            Text(category.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        RowActionsMenu(deleteLabel = stringResource(R.string.categories_delete_content_description), onDeleteClick = onDeleteClick)
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoriesListScreenPreview() {
    BudgmentTheme {
        CategoriesListScreen(
            uiState = CategoriesUiState(
                categories = listOf(
                    CategoryEntity(
                        id = "1",
                        userId = "user_1",
                        name = "Comida",
                        type = CategoryType.EXPENSE
                    ),
                    CategoryEntity(
                        id = "2",
                        userId = "user_1",
                        name = "Sueldo",
                        type = CategoryType.INCOME
                    )
                ),
                isLoading = false
            ),
            onBackClick = {},
            onAddCategoryClick = {},
            onCategoryClick = {},
            onDeleteCategory = {}
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoriesListScreenEmptyPreview() {
    BudgmentTheme {
        CategoriesListScreen(
            uiState = CategoriesUiState(
                categories = emptyList(),
                isLoading = false
            ),
            onBackClick = {},
            onAddCategoryClick = {},
            onCategoryClick = {},
            onDeleteCategory = {}
        )
    }
}
