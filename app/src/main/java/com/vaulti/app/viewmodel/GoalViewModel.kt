package com.vaulti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaulti.app.data.database.entity.Goal
import com.vaulti.app.data.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository
) : ViewModel() {

    val goals: StateFlow<List<Goal>> = goalRepository.getAllActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addGoal(
        name: String,
        targetAmount: Double,
        targetDate: Long? = null,
        color: Long = 0xFF6C63FF
    ) {
        viewModelScope.launch {
            val goal = Goal(
                name = name,
                targetAmount = targetAmount,
                targetDate = targetDate,
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

    fun completeGoal(goalId: Long) {
        viewModelScope.launch {
            goalRepository.completeGoal(goalId)
        }
    }

    fun updateGoal(
        goal: Goal,
        name: String,
        targetAmount: Double,
        targetDate: Long? = null,
        color: Long = 0xFF6C63FF
    ) {
        viewModelScope.launch {
            val isNowIncomplete = goal.isCompleted && targetAmount > goal.currentAmount
            goalRepository.update(goal.copy(
                name = name,
                targetAmount = targetAmount,
                targetDate = targetDate,
                color = color,
                isCompleted = if (isNowIncomplete) false else goal.isCompleted
            ))
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            goalRepository.delete(goal)
        }
    }
}
