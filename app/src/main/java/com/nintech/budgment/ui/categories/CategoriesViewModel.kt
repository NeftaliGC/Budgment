package com.nintech.budgment.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.repository.CategoryRepository
import com.nintech.budgment.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val uiState: StateFlow<CategoriesUiState> = sessionManager.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) flowOf(CategoriesUiState(isLoading = true))
            else categoryRepository.observeCategories(userId).map { CategoriesUiState(categories = it, isLoading = false) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoriesUiState())

    fun deleteCategory(id: String) {
        viewModelScope.launch { categoryRepository.deleteCategory(id) }
    }
}