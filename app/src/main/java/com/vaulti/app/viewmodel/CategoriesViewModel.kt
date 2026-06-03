package com.vaulti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaulti.app.data.database.entity.Category
import com.vaulti.app.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCategory(name: String) {
        val exists = categories.value.any { it.name.equals(name, ignoreCase = true) }
        if (!exists) {
            viewModelScope.launch { categoryRepository.insert(name) }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { categoryRepository.delete(category) }
    }
}
