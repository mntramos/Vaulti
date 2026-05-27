package com.vaulti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vaulti.app.data.database.entity.Account
import com.vaulti.app.data.database.entity.AccountType
import com.vaulti.app.data.repository.AccountRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AccountViewModel(
    private val accountRepository: AccountRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.getAllActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Double> = accountRepository.getTotalBalance()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun addAccount(name: String, type: AccountType, balance: Double, color: Long = 0xFF6C63FF) {
        viewModelScope.launch {
            val account = Account(
                name = name,
                type = type,
                balance = balance,
                color = color
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

    class Factory(
        private val accountRepository: AccountRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AccountViewModel(accountRepository) as T
        }
    }
}
