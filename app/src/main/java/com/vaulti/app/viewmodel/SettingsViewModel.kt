package com.vaulti.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.vaulti.app.data.database.VaultiDatabase
import com.vaulti.app.data.database.entity.AccountType
import com.vaulti.app.data.database.entity.BudgetPeriod
import com.vaulti.app.data.database.entity.Category
import com.vaulti.app.data.database.entity.RecurringInterval
import com.vaulti.app.data.database.entity.TransactionType
import com.vaulti.app.data.repository.AccountRepository
import com.vaulti.app.data.repository.BudgetRepository
import com.vaulti.app.data.repository.CategoryRepository
import com.vaulti.app.data.repository.GoalRepository
import com.vaulti.app.data.repository.TransactionRepository
import com.vaulti.app.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val database: VaultiDatabase,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val categoryRepository: CategoryRepository,
    private val syncManager: SyncManager,
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val categories = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCategory(name: String) {
        val exists = categories.value.any { it.name.equals(name, ignoreCase = true) }
        if (!exists) {
            viewModelScope.launch { categoryRepository.insert(name) }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { categoryRepository.delete(category) }
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            val accounts = accountRepository.getAll().first()
            val transactions = transactionRepository.getAll().first()
            val budgets = budgetRepository.getAll().first()
            val goals = goalRepository.getAll().first()

            val root = JSONObject()
            root.put("version", 1)
            root.put("exportedAt", System.currentTimeMillis())

            val accountsArr = JSONArray()
            accounts.forEach { a ->
                accountsArr.put(JSONObject().apply {
                    put("id", a.id)
                    put("name", a.name)
                    put("type", a.type.name)
                    put("balance", a.balance)
                    put("currency", a.currency)
                    put("color", a.color)
                    put("isArchived", a.isArchived)
                    put("createdAt", a.createdAt)
                })
            }
            root.put("accounts", accountsArr)

            val transactionsArr = JSONArray()
            transactions.forEach { t ->
                transactionsArr.put(JSONObject().apply {
                    put("id", t.id)
                    put("accountId", t.accountId)
                    put("toAccountId", t.toAccountId ?: JSONObject.NULL)
                    put("amount", t.amount)
                    put("type", t.type.name)
                    put("category", t.category)
                    put("note", t.note)
                    put("date", t.date)
                    put("isRecurring", t.isRecurring)
                    put("recurringInterval", t.recurringInterval?.name ?: JSONObject.NULL)
                    put("imagePath", t.imagePath ?: JSONObject.NULL)
                    put("budgetId", t.budgetId ?: JSONObject.NULL)
                    put("createdAt", t.createdAt)
                })
            }
            root.put("transactions", transactionsArr)

            val budgetsArr = JSONArray()
            budgets.forEach { b ->
                budgetsArr.put(JSONObject().apply {
                    put("id", b.id)
                    put("name", b.name)
                    put("amount", b.amount)
                    put("spent", b.spent)
                    put("period", b.period.name)
                    put("color", b.color)
                    put("startDate", b.startDate)
                    put("isActive", b.isActive)
                })
            }
            root.put("budgets", budgetsArr)

            val goalsArr = JSONArray()
            goals.forEach { g ->
                goalsArr.put(JSONObject().apply {
                    put("id", g.id)
                    put("name", g.name)
                    put("targetAmount", g.targetAmount)
                    put("currentAmount", g.currentAmount)
                    put("targetDate", g.targetDate ?: JSONObject.NULL)
                    put("color", g.color)
                    put("isCompleted", g.isCompleted)
                    put("createdAt", g.createdAt)
                })
            }
            root.put("goals", goalsArr)

            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(root.toString(2).toByteArray())
                }
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            val jsonString = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().readText()
                } ?: throw Exception("Could not read file")
            }

            val root = JSONObject(jsonString)

            withContext(Dispatchers.IO) {
                database.clearAllTables()

                val accountsArr = root.getJSONArray("accounts")
                for (i in 0 until accountsArr.length()) {
                    val obj = accountsArr.getJSONObject(i)
                    val account = com.vaulti.app.data.database.entity.Account(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        type = AccountType.valueOf(obj.getString("type")),
                        balance = obj.getDouble("balance"),
                        currency = obj.optString("currency", "PHP"),
                        color = obj.getLong("color"),
                        isArchived = obj.optBoolean("isArchived", false),
                        createdAt = obj.getLong("createdAt")
                    )
                    accountRepository.insert(account)
                }

                val transactionsArr = root.getJSONArray("transactions")
                for (i in 0 until transactionsArr.length()) {
                    val obj = transactionsArr.getJSONObject(i)
                    val toAccountId = if (obj.isNull("toAccountId")) null else obj.getLong("toAccountId")
                    val recurringInterval = if (obj.isNull("recurringInterval")) null else RecurringInterval.valueOf(obj.getString("recurringInterval"))
                    val imagePath = if (obj.isNull("imagePath")) null else obj.getString("imagePath")
                    val budgetId = if (obj.isNull("budgetId")) null else obj.getLong("budgetId")
                    val transaction = com.vaulti.app.data.database.entity.Transaction(
                        id = obj.getLong("id"),
                        accountId = obj.getLong("accountId"),
                        toAccountId = toAccountId,
                        amount = obj.getDouble("amount"),
                        type = TransactionType.valueOf(obj.getString("type")),
                        category = obj.getString("category"),
                        note = obj.optString("note", ""),
                        date = obj.getLong("date"),
                        isRecurring = obj.optBoolean("isRecurring", false),
                        recurringInterval = recurringInterval,
                        imagePath = imagePath,
                        budgetId = budgetId,
                        createdAt = obj.getLong("createdAt")
                    )
                    transactionRepository.insert(transaction)
                }

                val budgetsArr = root.getJSONArray("budgets")
                for (i in 0 until budgetsArr.length()) {
                    val obj = budgetsArr.getJSONObject(i)
                    val budget = com.vaulti.app.data.database.entity.Budget(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        amount = obj.getDouble("amount"),
                        spent = obj.optDouble("spent", 0.0),
                        period = BudgetPeriod.valueOf(obj.getString("period")),
                        color = obj.getLong("color"),
                        startDate = obj.getLong("startDate"),
                        isActive = obj.optBoolean("isActive", true)
                    )
                    budgetRepository.insert(budget)
                }

                val goalsArr = root.getJSONArray("goals")
                for (i in 0 until goalsArr.length()) {
                    val obj = goalsArr.getJSONObject(i)
                    val targetDate = if (obj.isNull("targetDate")) null else obj.getLong("targetDate")
                    val goal = com.vaulti.app.data.database.entity.Goal(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        targetAmount = obj.getDouble("targetAmount"),
                        currentAmount = obj.optDouble("currentAmount", 0.0),
                        targetDate = targetDate,
                        color = obj.getLong("color"),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        createdAt = obj.getLong("createdAt")
                    )
                    goalRepository.insert(goal)
                }
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            syncManager.deleteAll()
            syncManager.stopListening()
            withContext(Dispatchers.IO) {
                database.clearAllTables()
            }
            try {
                firebaseAuth.currentUser?.delete()?.await()
            } catch (_: Exception) {}
            firebaseAuth.signOut()
        }
    }
}
