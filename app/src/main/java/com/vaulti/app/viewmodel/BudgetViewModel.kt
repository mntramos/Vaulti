package com.vaulti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vaulti.app.data.database.entity.Budget
import com.vaulti.app.data.database.entity.BudgetPeriod
import com.vaulti.app.data.repository.BudgetRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BudgetViewModel(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    val budgets: StateFlow<List<Budget>> = budgetRepository.getAllActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addBudget(
        name: String,
        amount: Double,
        category: String,
        period: BudgetPeriod,
        color: Long = 0xFF6C63FF
    ) {
        viewModelScope.launch {
            val budget = Budget(
                name = name,
                amount = amount,
                category = category,
                period = period,
                color = color
            )
            budgetRepository.insert(budget)
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.delete(budget)
        }
    }

    class Factory(
        private val budgetRepository: BudgetRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BudgetViewModel(budgetRepository) as T
        }
    }
}
