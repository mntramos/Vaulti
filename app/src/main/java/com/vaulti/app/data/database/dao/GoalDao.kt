package com.vaulti.app.data.database.dao

import androidx.room.*
import com.vaulti.app.data.database.entity.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY targetAmount ASC")
    fun getAllActive(): Flow<List<Goal>>

    @Query("SELECT * FROM goals ORDER BY targetAmount ASC")
    fun getAll(): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: Goal): Long

    @Update
    suspend fun update(goal: Goal)

    @Delete
    suspend fun delete(goal: Goal)

    @Query("UPDATE goals SET currentAmount = :amount WHERE id = :id")
    suspend fun updateProgress(id: Long, amount: Double)

    @Query("UPDATE goals SET isCompleted = 1 WHERE id = :id")
    suspend fun completeGoal(id: Long)

}
