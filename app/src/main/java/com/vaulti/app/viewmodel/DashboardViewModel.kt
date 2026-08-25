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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalAssets: StateFlow<Double> = accountRepository.getTotalAssets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalLiabilities: StateFlow<Double> = accountRepository.getTotalLiabilities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalLiabilitiesDisplay: StateFlow<Double> = totalLiabilities
        .map { abs(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val recentTransactions: StateFlow<List<Transaction>> = transactionRepository.getRecentTransactions(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastTransactionDateByAccount: StateFlow<Map<Long, Long>> = transactionRepository.getLastTransactionDateByAccount()
        .map { list -> list.groupBy({ it.cId }, { it.lastDate }).mapValues { (_, dates) -> dates.max() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private fun monthWindowFlow(): Flow<Pair<Long, Long>> = flow {
        while (true) {
            val now = LocalDate.now()
            val start = now.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val end = now.plusMonths(1).withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            emit(start to end)
            delay((end - System.currentTimeMillis()).coerceAtLeast(0L) + 1000L)
        }
    }

    val monthlyExpense: StateFlow<Double> = monthWindowFlow()
        .flatMapLatest { (start, end) -> transactionRepository.getCurrentMonthExpense(start, end) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyIncome: StateFlow<Double> = monthWindowFlow()
        .flatMapLatest { (start, end) -> transactionRepository.getCurrentMonthIncome(start, end) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
}
