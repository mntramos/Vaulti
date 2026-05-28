package com.vaulti.app.data.repository

import com.vaulti.app.data.database.dao.TransactionDao
import com.vaulti.app.data.database.entity.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {
    fun getAll(): Flow<List<Transaction>> = transactionDao.getAll()
    fun getByAccountId(accountId: Long): Flow<List<Transaction>> = transactionDao.getByAccountId(accountId)
    fun getByDateRange(start: Long, end: Long): Flow<List<Transaction>> = transactionDao.getByDateRange(start, end)
    fun getByDateRangeDesc(start: Long, end: Long): Flow<List<Transaction>> = transactionDao.getByDateRangeDesc(start, end)
    fun getByAccountAndDateRange(accountId: Long, start: Long, end: Long): Flow<List<Transaction>> = transactionDao.getByAccountAndDateRange(accountId, start, end)
    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>> = transactionDao.getRecentTransactions(limit)
    fun getTotalExpense(start: Long, end: Long): Flow<Double?> = transactionDao.getTotalExpense(start, end)
    fun getTotalIncome(start: Long, end: Long): Flow<Double?> = transactionDao.getTotalIncome(start, end)
    suspend fun getById(id: Long): Transaction? = transactionDao.getById(id)
    suspend fun insert(transaction: Transaction): Long = transactionDao.insert(transaction)
    suspend fun update(transaction: Transaction) = transactionDao.update(transaction)
    suspend fun delete(transaction: Transaction) = transactionDao.delete(transaction)
}
