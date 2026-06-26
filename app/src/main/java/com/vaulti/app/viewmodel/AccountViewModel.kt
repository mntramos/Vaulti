package com.vaulti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaulti.app.data.database.entity.Account
import com.vaulti.app.data.database.entity.AccountType
import com.vaulti.app.data.repository.AccountRepository
import com.vaulti.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.getAllActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Double> = accountRepository.getTotalBalance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val lastTransactionDateByAccount: StateFlow<Map<Long, Long>> = transactionRepository.getLastTransactionDateByAccount()
        .map { list -> list.groupBy({ it.cId }, { it.lastDate }).mapValues { (_, dates) -> dates.max() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun addAccount(name: String, type: AccountType, balance: Double, color: Long = 0xFF6C63FF, isLiability: Boolean = type.isLiability) {
        viewModelScope.launch {
            val storedBalance = if (isLiability) -balance else balance
            val account = Account(
                name = name,
                type = type,
                balance = storedBalance,
                color = color,
                isLiability = isLiability
            )
            accountRepository.insert(account)
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            accountRepository.update(account)
        }
    }

    fun archiveAccount(id: Long) {
        viewModelScope.launch {
            accountRepository.archive(id)
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            accountRepository.delete(account)
        }
    }
}
