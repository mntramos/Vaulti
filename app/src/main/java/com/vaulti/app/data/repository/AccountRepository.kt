package com.vaulti.app.data.repository

import com.vaulti.app.data.database.dao.AccountDao
import com.vaulti.app.data.database.entity.Account
import com.vaulti.app.data.database.entity.AccountType
import com.vaulti.app.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow

class AccountRepository(
    private val accountDao: AccountDao,
    private val syncManager: SyncManager
) {
    fun getAllActive(): Flow<List<Account>> = accountDao.getAllActive()
    fun getAll(): Flow<List<Account>> = accountDao.getAll()
    fun getTotalBalance(): Flow<Double?> = accountDao.getTotalBalance()
    fun getTotalAssets(): Flow<Double?> = accountDao.getTotalAssets()
    fun getTotalLiabilities(): Flow<Double?> = accountDao.getTotalLiabilities()

    suspend fun insert(account: Account): Long {
        val id = accountDao.insert(account)
        val saved = account.copy(id = id)
        syncManager.pushAccount(saved)
        return id
    }

    suspend fun update(account: Account) {
        accountDao.update(account)
        syncManager.pushAccount(account)
    }

    suspend fun delete(account: Account) {
        accountDao.delete(account)
        syncManager.deleteAccount(account.id)
    }

    suspend fun archive(id: Long) {
        accountDao.archive(id)
        val account = accountDao.getById(id)
        if (account != null) syncManager.pushAccount(account)
    }

    suspend fun updateBalance(accountId: Long, amount: Double) {
        accountDao.updateBalance(accountId, amount)
        val account = accountDao.getById(accountId)
        if (account != null) syncManager.pushAccount(account)
    }
}
