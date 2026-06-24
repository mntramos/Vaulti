package com.vaulti.app.data.repository

import com.vaulti.app.data.database.dao.AccountLastTransactionRaw
import com.vaulti.app.data.database.dao.TransactionDao
import com.vaulti.app.data.database.entity.Transaction
import com.vaulti.app.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val syncManager: SyncManager
) {
    fun getAll(): Flow<List<Transaction>> = transactionDao.getAll()
    fun getByAccountId(accountId: Long): Flow<List<Transaction>> = transactionDao.getByAccountId(accountId)
    fun getByDateRange(start: Long, end: Long): Flow<List<Transaction>> = transactionDao.getByDateRange(start, end)
    fun getByDateRangeDesc(start: Long, end: Long): Flow<List<Transaction>> = transactionDao.getByDateRangeDesc(start, end)
    fun getByAccountAndDateRange(accountId: Long, start: Long, end: Long): Flow<List<Transaction>> = transactionDao.getByAccountAndDateRange(accountId, start, end)
    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>> = transactionDao.getRecentTransactions(limit)
    fun getTotalExpense(start: Long, end: Long): Flow<Double?> = transactionDao.getTotalExpense(start, end)
    fun getTotalIncome(start: Long, end: Long): Flow<Double?> = transactionDao.getTotalIncome(start, end)
    fun getCurrentMonthExpense(startOfMonth: Long, startOfNextMonth: Long): Flow<Double?> = transactionDao.getCurrentMonthExpense(startOfMonth, startOfNextMonth)
    fun getCurrentMonthIncome(startOfMonth: Long, startOfNextMonth: Long): Flow<Double?> = transactionDao.getCurrentMonthIncome(startOfMonth, startOfNextMonth)
    fun getLastTransactionDateByAccount(): Flow<List<AccountLastTransactionRaw>> = transactionDao.getLastTransactionDateByAccount()
    suspend fun getById(id: Long): Transaction? = transactionDao.getById(id)

    suspend fun insert(transaction: Transaction): Long {
        val id = transactionDao.insert(transaction)
        val saved = transaction.copy(id = id)
        syncManager.pushTransaction(saved)
        return id
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction)
        syncManager.pushTransaction(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
        syncManager.deleteTransaction(transaction.id)
    }
}
