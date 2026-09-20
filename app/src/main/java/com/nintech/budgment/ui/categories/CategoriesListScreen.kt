package com.nintech.budgment.ui.categories

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.ui.theme.BudgmentTheme

@Composable
fun CategoriesListScreen(
    onBackClick: () -> Unit,
    onAddCategoryClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    viewModel: CategoriesViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    if (viewModel != null) {
        val uiState by viewModel.uiState.collectAsState()

        CategoriesListScreen(
            uiState = uiState,
            onBackClick = onBackClick,
            onAddCategoryClick = onAddCategoryClick,
            onCategoryClick = onCategoryClick,
            onDeleteCategory = viewModel::deleteCategory
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
    onDeleteCategory: (String) -> Unit
) {
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
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.categories_empty_title),
                        fontSize = 18.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.categories_empty_subtitle),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(uiState.categories, key = { it.id }) { category ->
                    CategoryRow(
                        category = category,
                        onClick = { onCategoryClick(category.id) },
                        onDeleteClick = { onDeleteCategory(category.id) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Box(Modifier.size(10.dp).background(accentColor, CircleShape))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(category.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(2.dp))
                Text(category.type.label(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        }
        IconButton(onClick = onDeleteClick) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.categories_delete_content_description),
                tint = MaterialTheme.colorScheme.error
            )
        }
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
