package com.vaulti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vaulti.app.data.database.entity.Account
import com.vaulti.app.data.database.entity.Budget
import com.vaulti.app.data.database.entity.Transaction
import com.vaulti.app.data.database.entity.TransactionType
import com.vaulti.app.data.repository.AccountRepository
import com.vaulti.app.data.repository.BudgetRepository
import com.vaulti.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> = transactionRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> = accountRepository.getAllActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<Budget>> = budgetRepository.getAllActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private suspend fun updateBudgetSpent(budgetId: Long?, amountDelta: Double) {
        if (budgetId == null) return
        val budget = budgetRepository.getById(budgetId) ?: return
        val newSpent = (budget.spent + amountDelta).coerceAtLeast(0.0)
        budgetRepository.updateSpent(budget.id, newSpent)
    }

    fun addTransaction(
        accountId: Long,
        amount: Double,
        type: TransactionType,
        category: String,
        note: String = "",
        date: Long = System.currentTimeMillis(),
        toAccountId: Long? = null,
        budgetId: Long? = null
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                accountId = accountId,
                toAccountId = toAccountId,
                amount = amount,
                type = type,
                category = category,
                note = note,
                date = date,
                budgetId = budgetId
            )
            transactionRepository.insert(transaction)

            when (type) {
                TransactionType.EXPENSE -> {
                    accountRepository.updateBalance(accountId, -amount)
                    updateBudgetSpent(budgetId, amount)
                }
                TransactionType.INCOME -> {
                    accountRepository.updateBalance(accountId, amount)
                }
                TransactionType.TRANSFER -> {
                    val target = toAccountId ?: return@launch
                    accountRepository.updateBalance(accountId, -amount)
                    accountRepository.updateBalance(target, amount)
                }
            }
        }
    }

    fun updateTransaction(
        transaction: Transaction,
        accountId: Long,
        amount: Double,
        type: TransactionType,
        category: String,
        note: String,
        date: Long,
        toAccountId: Long? = null,
        budgetId: Long? = null
    ) {
        viewModelScope.launch {
            val oldAmount = transaction.amount
            val oldType = transaction.type
            val oldAccountId = transaction.accountId
            val oldToAccountId = transaction.toAccountId
            val oldBudgetId = transaction.budgetId

            val updated = transaction.copy(
                accountId = accountId,
                toAccountId = toAccountId,
                amount = amount,
                type = type,
                category = category,
                note = note,
                date = date,
                budgetId = budgetId
            )
            transactionRepository.update(updated)

            reverseTransaction(oldAccountId, oldToAccountId, oldAmount, oldType)
            if (oldType == TransactionType.EXPENSE) {
                updateBudgetSpent(oldBudgetId, -oldAmount)
            }

            when (type) {
                TransactionType.EXPENSE -> {
                    accountRepository.updateBalance(accountId, -amount)
                    updateBudgetSpent(budgetId, amount)
                }
                TransactionType.INCOME -> accountRepository.updateBalance(accountId, amount)
                TransactionType.TRANSFER -> {
                    val target = toAccountId ?: return@launch
                    accountRepository.updateBalance(accountId, -amount)
                    accountRepository.updateBalance(target, amount)
                }
            }
        }
    }

    private suspend fun reverseTransaction(
        accountId: Long,
        toAccountId: Long?,
        amount: Double,
        type: TransactionType
    ) {
        when (type) {
            TransactionType.EXPENSE -> accountRepository.updateBalance(accountId, amount)
            TransactionType.INCOME -> accountRepository.updateBalance(accountId, -amount)
            TransactionType.TRANSFER -> {
                accountRepository.updateBalance(accountId, amount)
                if (toAccountId != null) {
                    accountRepository.updateBalance(toAccountId, -amount)
                }
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.delete(transaction)
            reverseTransaction(transaction.accountId, transaction.toAccountId, transaction.amount, transaction.type)
            if (transaction.type == TransactionType.EXPENSE) {
                updateBudgetSpent(transaction.budgetId, -transaction.amount)
            }
        }
    }

    fun deleteTransactionById(transactionId: Long) {
        viewModelScope.launch {
            val transaction = transactionRepository.getById(transactionId) ?: return@launch
            transactionRepository.delete(transaction)
            reverseTransaction(transaction.accountId, transaction.toAccountId, transaction.amount, transaction.type)
            if (transaction.type == TransactionType.EXPENSE) {
                updateBudgetSpent(transaction.budgetId, -transaction.amount)
            }
        }
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val accountRepository: AccountRepository,
        private val budgetRepository: BudgetRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TransactionViewModel(transactionRepository, accountRepository, budgetRepository) as T
        }
    }
}
