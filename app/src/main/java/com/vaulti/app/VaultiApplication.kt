package com.vaulti.app

import android.app.Application
import com.vaulti.app.data.database.VaultiDatabase
import com.vaulti.app.data.repository.AccountRepository
import com.vaulti.app.data.repository.BudgetRepository
import com.vaulti.app.data.repository.GoalRepository
import com.vaulti.app.data.repository.TransactionRepository

class VaultiApplication : Application() {
    val database: VaultiDatabase by lazy { VaultiDatabase.getDatabase(this) }
    val accountRepository: AccountRepository by lazy { AccountRepository(database.accountDao()) }
    val transactionRepository: TransactionRepository by lazy { TransactionRepository(database.transactionDao()) }
    val budgetRepository: BudgetRepository by lazy { BudgetRepository(database.budgetDao()) }
    val goalRepository: GoalRepository by lazy { GoalRepository(database.goalDao()) }
}
