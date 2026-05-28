package com.vaulti.app

import android.app.Application
import com.vaulti.app.data.database.VaultiDatabase
import com.vaulti.app.data.repository.AccountRepository
import com.vaulti.app.data.repository.BudgetRepository
import com.vaulti.app.data.repository.GoalRepository
import com.vaulti.app.data.repository.TransactionRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VaultiApplication : Application() {
    @Inject lateinit var database: VaultiDatabase
    @Inject lateinit var accountRepository: AccountRepository
    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var budgetRepository: BudgetRepository
    @Inject lateinit var goalRepository: GoalRepository
}
