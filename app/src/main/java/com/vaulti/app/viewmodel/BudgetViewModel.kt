package com.vaulti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaulti.app.data.database.entity.Budget
import com.vaulti.app.data.database.entity.BudgetPeriod
import com.vaulti.app.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    val budgets: StateFlow<List<Budget>> = budgetRepository.getAllActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addBudget(
        name: String,
        amount: Double,
        period: BudgetPeriod,
        color: Long = 0xFF6C63FF
    ) {
        viewModelScope.launch {
            val budget = Budget(
                name = name,
                amount = amount,
                period = period,
                color = color
            )
            budgetRepository.insert(budget)
        }
    }

    fun updateBudget(
        budget: Budget,
        name: String,
        amount: Double,
        period: BudgetPeriod,
        color: Long = 0xFF6C63FF
    ) {
        viewModelScope.launch {
            budgetRepository.update(budget.copy(name = name, amount = amount, period = period, color = color))
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.delete(budget)
        }
    }
}
