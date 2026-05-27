package com.vaulti.app.data.repository

import com.vaulti.app.data.database.dao.GoalDao
import com.vaulti.app.data.database.entity.Goal
import kotlinx.coroutines.flow.Flow

class GoalRepository(private val goalDao: GoalDao) {
    fun getAllActive(): Flow<List<Goal>> = goalDao.getAllActive()
    fun getAll(): Flow<List<Goal>> = goalDao.getAll()
    suspend fun getById(id: Long): Goal? = goalDao.getById(id)
    suspend fun insert(goal: Goal): Long = goalDao.insert(goal)
    suspend fun update(goal: Goal) = goalDao.update(goal)
    suspend fun delete(goal: Goal) = goalDao.delete(goal)
    suspend fun updateProgress(id: Long, amount: Double) = goalDao.updateProgress(id, amount)
    suspend fun deleteAll() = goalDao.deleteAll()
}
