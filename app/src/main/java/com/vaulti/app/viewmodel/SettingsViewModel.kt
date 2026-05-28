package com.vaulti.app.viewmodel

import androidx.lifecycle.ViewModel
import com.vaulti.app.data.database.VaultiDatabase
import com.vaulti.app.data.repository.AccountRepository
import com.vaulti.app.data.repository.BudgetRepository
import com.vaulti.app.data.repository.GoalRepository
import com.vaulti.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val database: VaultiDatabase,
    val accountRepository: AccountRepository,
    val transactionRepository: TransactionRepository,
    val budgetRepository: BudgetRepository,
    val goalRepository: GoalRepository
) : ViewModel()
