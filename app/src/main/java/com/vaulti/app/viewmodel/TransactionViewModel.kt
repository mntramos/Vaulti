package com.vaulti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaulti.app.data.database.entity.Account
import com.vaulti.app.data.database.entity.Budget
import com.vaulti.app.data.database.entity.BudgetPeriod
import com.vaulti.app.data.database.entity.Transaction
import com.vaulti.app.data.database.entity.TransactionType
import com.vaulti.app.data.repository.AccountRepository
import com.vaulti.app.data.repository.BudgetRepository
import com.vaulti.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
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
        val now = System.currentTimeMillis()
        val periodStart = getCurrentPeriodStart(budget.startDate, budget.period, now)
        val newSpent = if (periodStart > budget.startDate) {
            amountDelta.coerceAtLeast(0.0)
        } else {
            (budget.spent + amountDelta).coerceAtLeast(0.0)
        }
        if (periodStart > budget.startDate) {
            budgetRepository.update(budget.copy(startDate = periodStart, spent = newSpent))
        } else {
            budgetRepository.updateSpent(budget.id, newSpent)
        }
    }

    private fun getCurrentPeriodStart(startDate: Long, period: BudgetPeriod, now: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = startDate }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }
        return when (period) {
            BudgetPeriod.WEEKLY -> {
                val daysSinceStart = ((now - startDate) / (7 * 24 * 60 * 60 * 1000)).toInt()
                cal.apply { add(Calendar.DAY_OF_YEAR, daysSinceStart * 7) }.timeInMillis
            }
            BudgetPeriod.MONTHLY -> {
                val monthsDiff = (nowCal.get(Calendar.YEAR) - cal.get(Calendar.YEAR)) * 12 +
                        nowCal.get(Calendar.MONTH) - cal.get(Calendar.MONTH)
                cal.apply { add(Calendar.MONTH, monthsDiff) }.timeInMillis
            }
            BudgetPeriod.YEARLY -> {
                val yearsDiff = nowCal.get(Calendar.YEAR) - cal.get(Calendar.YEAR)
                cal.apply { add(Calendar.YEAR, yearsDiff) }.timeInMillis
            }
        }
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
}
