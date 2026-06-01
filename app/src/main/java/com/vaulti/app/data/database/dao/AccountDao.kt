package com.vaulti.app.data.database.dao

import androidx.room.*
import com.vaulti.app.data.database.entity.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY balance DESC")
    fun getAllActive(): Flow<List<Account>>

    @Query("SELECT * FROM accounts ORDER BY balance DESC")
    fun getAll(): Flow<List<Account>>

    @Query("SELECT SUM(balance) FROM accounts WHERE isArchived = 0")
    fun getTotalBalance(): Flow<Double?>

    @Query("SELECT SUM(balance) FROM accounts WHERE isArchived = 0 AND isLiability = 0")
    fun getTotalAssets(): Flow<Double?>

    @Query("SELECT SUM(balance) FROM accounts WHERE isArchived = 0 AND isLiability = 1")
    fun getTotalLiabilities(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("UPDATE accounts SET balance = balance + :amount WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, amount: Double)

    @Query("UPDATE accounts SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

}
