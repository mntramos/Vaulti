package com.vaulti.app.data.repository

import com.vaulti.app.data.database.dao.BudgetDao
import com.vaulti.app.data.database.entity.Budget
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val budgetDao: BudgetDao) {
    fun getAllActive(): Flow<List<Budget>> = budgetDao.getAllActive()
    fun getAll(): Flow<List<Budget>> = budgetDao.getAll()
    suspend fun getById(id: Long): Budget? = budgetDao.getById(id)
    suspend fun insert(budget: Budget): Long = budgetDao.insert(budget)
    suspend fun update(budget: Budget) = budgetDao.update(budget)
    suspend fun delete(budget: Budget) = budgetDao.delete(budget)
    suspend fun updateSpent(id: Long, spent: Double) = budgetDao.updateSpent(id, spent)
    suspend fun deleteAll() = budgetDao.deleteAll()
}
