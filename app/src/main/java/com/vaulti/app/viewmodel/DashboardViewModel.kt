package com.vaulti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaulti.app.data.database.entity.Account
import com.vaulti.app.data.database.entity.Transaction
import com.vaulti.app.data.repository.AccountRepository
import com.vaulti.app.data.repository.TransactionRepository
import com.vaulti.app.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                syncManager.pullAll()
            } catch (_: Exception) {}
            _isRefreshing.value = false
        }
    }

    val accounts: StateFlow<List<Account>> = accountRepository.getAllActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Double> = accountRepository.getTotalBalance()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalAssets: StateFlow<Double> = accountRepository.getTotalAssets()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalLiabilities: StateFlow<Double> = accountRepository.getTotalLiabilities()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalLiabilitiesDisplay: StateFlow<Double> = totalLiabilities
        .map { abs(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val recentTransactions: StateFlow<List<Transaction>> = transactionRepository.getRecentTransactions(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastTransactionDateByAccount: StateFlow<Map<Long, Long>> = transactionRepository.getLastTransactionDateByAccount()
        .map { list -> list.groupBy({ it.cId }, { it.lastDate }).mapValues { (_, dates) -> dates.max() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val now = LocalDate.now()
    private val startOfMonth = now.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val startOfNextMonth = now.plusMonths(1).withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val monthlyExpense: StateFlow<Double> = transactionRepository.getCurrentMonthExpense(startOfMonth, startOfNextMonth)
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyIncome: StateFlow<Double> = transactionRepository.getCurrentMonthIncome(startOfMonth, startOfNextMonth)
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
}
