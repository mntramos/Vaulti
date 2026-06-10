package com.vaulti.app.data.repository

import com.vaulti.app.data.database.dao.BudgetDao
import com.vaulti.app.data.database.entity.Budget
import com.vaulti.app.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow

class BudgetRepository(
    private val budgetDao: BudgetDao,
    private val syncManager: SyncManager
) {
    fun getAllActive(): Flow<List<Budget>> = budgetDao.getAllActive()
    fun getAll(): Flow<List<Budget>> = budgetDao.getAll()
    suspend fun getById(id: Long): Budget? = budgetDao.getById(id)

    suspend fun insert(budget: Budget): Long {
        val id = budgetDao.insert(budget)
        val saved = budget.copy(id = id)
        syncManager.pushBudget(saved)
        return id
    }

    suspend fun update(budget: Budget) {
        budgetDao.update(budget)
        syncManager.pushBudget(budget)
    }

    suspend fun delete(budget: Budget) {
        budgetDao.delete(budget)
        syncManager.deleteBudget(budget.id)
    }

    suspend fun updateSpent(id: Long, spent: Double) {
        budgetDao.updateSpent(id, spent)
        val budget = budgetDao.getById(id)
        if (budget != null) syncManager.pushBudget(budget)
    }
}
