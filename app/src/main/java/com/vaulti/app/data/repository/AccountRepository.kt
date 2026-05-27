package com.vaulti.app.data.repository

import com.vaulti.app.data.database.dao.AccountDao
import com.vaulti.app.data.database.entity.Account
import kotlinx.coroutines.flow.Flow

class AccountRepository(private val accountDao: AccountDao) {
    fun getAllActive(): Flow<List<Account>> = accountDao.getAllActive()
    fun getAll(): Flow<List<Account>> = accountDao.getAll()
    fun getTotalBalance(): Flow<Double?> = accountDao.getTotalBalance()
    suspend fun getById(id: Long): Account? = accountDao.getById(id)
    suspend fun insert(account: Account): Long = accountDao.insert(account)
    suspend fun update(account: Account) = accountDao.update(account)
    suspend fun delete(account: Account) = accountDao.delete(account)
    suspend fun archive(id: Long) = accountDao.archive(id)
    suspend fun updateBalance(accountId: Long, amount: Double) = accountDao.updateBalance(accountId, amount)
    suspend fun deleteAll() = accountDao.deleteAll()
}
