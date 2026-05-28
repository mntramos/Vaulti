package com.vaulti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaulti.app.data.database.entity.Account
import com.vaulti.app.data.database.entity.Transaction
import com.vaulti.app.data.database.entity.TransactionType
import com.vaulti.app.data.repository.AccountRepository
import com.vaulti.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.getAllActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Double> = accountRepository.getTotalBalance()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val recentTransactions: StateFlow<List<Transaction>> = transactionRepository.getAll()
        .map { it.sortedByDescending { t -> t.date } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _monthlyExpense = MutableStateFlow(0.0)
    val monthlyExpense: StateFlow<Double> = _monthlyExpense.asStateFlow()

    private val _monthlyIncome = MutableStateFlow(0.0)
    val monthlyIncome: StateFlow<Double> = _monthlyIncome.asStateFlow()

    init {
        viewModelScope.launch {
            transactionRepository.getAll().collect { allTransactions ->
                val monthAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                var expense = 0.0
                var income = 0.0
                for (t in allTransactions) {
                    if (t.date >= monthAgo) {
                        when (t.type) {
                            TransactionType.EXPENSE -> expense += t.amount
                            TransactionType.INCOME -> income += t.amount
                            TransactionType.TRANSFER -> {}
                        }
                    }
                }
                _monthlyExpense.value = expense
                _monthlyIncome.value = income
            }
        }
    }
}
