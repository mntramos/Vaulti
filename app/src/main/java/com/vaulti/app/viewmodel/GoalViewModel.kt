package com.vaulti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vaulti.app.data.database.entity.Goal
import com.vaulti.app.data.repository.GoalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GoalViewModel(
    private val goalRepository: GoalRepository
) : ViewModel() {

    val goals: StateFlow<List<Goal>> = goalRepository.getAllActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addGoal(
        name: String,
        targetAmount: Double,
        targetDate: Long? = null,
        category: String = "",
        color: Long = 0xFF6C63FF
    ) {
        viewModelScope.launch {
            val goal = Goal(
                name = name,
                targetAmount = targetAmount,
                targetDate = targetDate,
                category = category,
                color = color
            )
            goalRepository.insert(goal)
        }
    }

    fun updateProgress(goalId: Long, amount: Double) {
        viewModelScope.launch {
            goalRepository.updateProgress(goalId, amount)
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            goalRepository.delete(goal)
        }
    }

    class Factory(
        private val goalRepository: GoalRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GoalViewModel(goalRepository) as T
        }
    }
}
