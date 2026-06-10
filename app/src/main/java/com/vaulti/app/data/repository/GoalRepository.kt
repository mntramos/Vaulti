package com.vaulti.app.data.repository

import com.vaulti.app.data.database.dao.GoalDao
import com.vaulti.app.data.database.entity.Goal
import com.vaulti.app.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow

class GoalRepository(
    private val goalDao: GoalDao,
    private val syncManager: SyncManager
) {
    fun getAllActive(): Flow<List<Goal>> = goalDao.getAllActive()
    fun getAll(): Flow<List<Goal>> = goalDao.getAll()

    suspend fun insert(goal: Goal): Long {
        val id = goalDao.insert(goal)
        val saved = goal.copy(id = id)
        syncManager.pushGoal(saved)
        return id
    }

    suspend fun update(goal: Goal) {
        goalDao.update(goal)
        syncManager.pushGoal(goal)
    }

    suspend fun delete(goal: Goal) {
        goalDao.delete(goal)
        syncManager.deleteGoal(goal.id)
    }

    suspend fun updateProgress(id: Long, amount: Double) {
        goalDao.updateProgress(id, amount)
        val goal = goalDao.getById(id)
        if (goal != null) syncManager.pushGoal(goal)
    }

    suspend fun completeGoal(id: Long) {
        goalDao.completeGoal(id)
        val goal = goalDao.getById(id)
        if (goal != null) syncManager.pushGoal(goal)
    }
}
