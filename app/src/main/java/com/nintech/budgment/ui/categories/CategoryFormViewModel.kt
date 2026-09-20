package com.nintech.budgment.ui.categories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.data.repository.CategoryRepository
import com.nintech.budgment.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryFormState(
    val name: String = "",
    val type: CategoryType = CategoryType.EXPENSE,
    val icon: String? = null,
    val color: String? = null,
    val isEditing: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class CategoryFormViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // .ifBlank { null } es importante: createRoute() manda "" (no ausencia del parámetro)
    // cuando es una categoría nueva — sin esto, "isEditing" quedaría mal en falso positivo.
    private val categoryId: String? = savedStateHandle.get<String>("categoryId")?.ifBlank { null }
    private var existingCategory: CategoryEntity? = null

    private val _state = MutableStateFlow(CategoryFormState(isEditing = categoryId != null))
    val state: StateFlow<CategoryFormState> = _state.asStateFlow()

    init {
        categoryId?.let { id ->
            viewModelScope.launch {
                categoryRepository.getCategory(id)?.let { category ->
                    existingCategory = category
                    _state.value = _state.value.copy(name = category.name, type = category.type, icon = category.icon, color = category.color)
                }
            }
        }
    }

    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value) }
    fun onTypeChange(value: CategoryType) { _state.value = _state.value.copy(type = value) }
    fun onIconChange(value: String?) { _state.value = _state.value.copy(icon = value) }
    fun onColorChange(value: String?) { _state.value = _state.value.copy(color = value) }

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            existingCategory?.let { category ->
                categoryRepository.updateCategory(category, s.name, s.type, s.icon, s.color)
            } ?: run {
                val userId = sessionManager.currentUserId.filterNotNull().first()
                categoryRepository.createCategory(userId, s.name, s.type, s.icon, s.color)
            }
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}