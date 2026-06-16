package com.vaulti.app.data.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.vaulti.app.data.database.VaultiDatabase
import com.vaulti.app.data.database.entity.Account
import com.vaulti.app.data.database.entity.AccountType
import com.vaulti.app.data.database.entity.Budget
import com.vaulti.app.data.database.entity.BudgetPeriod
import com.vaulti.app.data.database.entity.Category
import com.vaulti.app.data.database.entity.Goal
import com.vaulti.app.data.database.entity.RecurringInterval
import com.vaulti.app.data.database.entity.Transaction
import com.vaulti.app.data.database.entity.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: VaultiDatabase,
    private val firebaseAuth: FirebaseAuth
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val listeners = mutableListOf<ListenerRegistration>()

    private val uid: String?
        get() = firebaseAuth.currentUser?.uid

    private fun accountsRef() = uid?.let { firestore.collection("users").document(it).collection("accounts") }
    private fun transactionsRef() = uid?.let { firestore.collection("users").document(it).collection("transactions") }
    private fun budgetsRef() = uid?.let { firestore.collection("users").document(it).collection("budgets") }
    private fun goalsRef() = uid?.let { firestore.collection("users").document(it).collection("goals") }
    private fun categoriesRef() = uid?.let { firestore.collection("users").document(it).collection("categories") }

    fun pushAccount(account: Account) {
        val ref = accountsRef() ?: return
        scope.launch {
            try { ref.document(account.id.toString()).set(account.toMap()) } catch (_: Exception) {}
        }
    }

    fun deleteAccount(id: Long) {
        scope.launch {
            try { accountsRef()?.document(id.toString())?.delete() } catch (_: Exception) {}
        }
    }

    fun pushTransaction(transaction: Transaction) {
        val ref = transactionsRef() ?: return
        scope.launch {
            try { ref.document(transaction.id.toString()).set(transaction.toMap()) } catch (_: Exception) {}
        }
    }

    fun deleteTransaction(id: Long) {
        scope.launch {
            try { transactionsRef()?.document(id.toString())?.delete() } catch (_: Exception) {}
        }
    }

    fun pushBudget(budget: Budget) {
        val ref = budgetsRef() ?: return
        scope.launch {
            try { ref.document(budget.id.toString()).set(budget.toMap()) } catch (_: Exception) {}
        }
    }

    fun deleteBudget(id: Long) {
        scope.launch {
            try { budgetsRef()?.document(id.toString())?.delete() } catch (_: Exception) {}
        }
    }

    fun pushGoal(goal: Goal) {
        val ref = goalsRef() ?: return
        scope.launch {
            try { ref.document(goal.id.toString()).set(goal.toMap()) } catch (_: Exception) {}
        }
    }

    fun deleteGoal(id: Long) {
        scope.launch {
            try { goalsRef()?.document(id.toString())?.delete() } catch (_: Exception) {}
        }
    }

    fun pushCategory(category: Category) {
        val ref = categoriesRef() ?: return
        scope.launch {
            try { ref.document(category.id.toString()).set(category.toMap()) } catch (_: Exception) {}
        }
    }

    fun deleteCategory(id: Long) {
        scope.launch {
            try { categoriesRef()?.document(id.toString())?.delete() } catch (_: Exception) {}
        }
    }

    suspend fun pullAll() = withContext(Dispatchers.IO) {
        val currentUid = uid ?: return@withContext

        val db = database
        val accountDao = db.accountDao()
        val transactionDao = db.transactionDao()
        val budgetDao = db.budgetDao()
        val goalDao = db.goalDao()
        val categoryDao = db.categoryDao()

        val baseRef = firestore.collection("users").document(currentUid)

        try {
            val accountSnapshot = baseRef.collection("accounts").get().await()
            for (doc in accountSnapshot.documents) {
                doc.data?.toAccount()?.let { accountDao.insert(it) }
            }
        } catch (_: Exception) {}

        try {
            val transactionSnapshot = baseRef.collection("transactions").get().await()
            for (doc in transactionSnapshot.documents) {
                doc.data?.toTransaction()?.let { transactionDao.insert(it) }
            }
        } catch (_: Exception) {}

        try {
            val budgetSnapshot = baseRef.collection("budgets").get().await()
            for (doc in budgetSnapshot.documents) {
                doc.data?.toBudget()?.let { budgetDao.insert(it) }
            }
        } catch (_: Exception) {}

        try {
            val goalSnapshot = baseRef.collection("goals").get().await()
            for (doc in goalSnapshot.documents) {
                doc.data?.toGoal()?.let { goalDao.insert(it) }
            }
        } catch (_: Exception) {}

        try {
            val categorySnapshot = baseRef.collection("categories").get().await()
            for (doc in categorySnapshot.documents) {
                doc.data?.toCategory()?.let { categoryDao.insert(it) }
            }
        } catch (_: Exception) {}
    }

    fun startListening() {
        val currentUid = uid ?: return
        val baseRef = firestore.collection("users").document(currentUid)

        val accountReg = baseRef.collection("accounts").addSnapshotListener { snapshot, _ ->
            snapshot?.documentChanges?.forEach { change ->
                if (change.type == DocumentChange.Type.REMOVED) return@forEach
                val account = change.document.data.toAccount() ?: return@forEach
                scope.launch { database.accountDao().insert(account) }
            }
        }
        listeners.add(accountReg)

        val transactionReg = baseRef.collection("transactions").addSnapshotListener { snapshot, _ ->
            snapshot?.documentChanges?.forEach { change ->
                if (change.type == DocumentChange.Type.REMOVED) return@forEach
                val transaction = change.document.data.toTransaction() ?: return@forEach
                scope.launch { database.transactionDao().insert(transaction) }
            }
        }
        listeners.add(transactionReg)

        val budgetReg = baseRef.collection("budgets").addSnapshotListener { snapshot, _ ->
            snapshot?.documentChanges?.forEach { change ->
                if (change.type == DocumentChange.Type.REMOVED) return@forEach
                val budget = change.document.data.toBudget() ?: return@forEach
                scope.launch { database.budgetDao().insert(budget) }
            }
        }
        listeners.add(budgetReg)

        val goalReg = baseRef.collection("goals").addSnapshotListener { snapshot, _ ->
            snapshot?.documentChanges?.forEach { change ->
                if (change.type == DocumentChange.Type.REMOVED) return@forEach
                val goal = change.document.data.toGoal() ?: return@forEach
                scope.launch { database.goalDao().insert(goal) }
            }
        }
        listeners.add(goalReg)

        val categoryReg = baseRef.collection("categories").addSnapshotListener { snapshot, _ ->
            snapshot?.documentChanges?.forEach { change ->
                if (change.type == DocumentChange.Type.REMOVED) return@forEach
                val category = change.document.data.toCategory() ?: return@forEach
                scope.launch { database.categoryDao().insert(category) }
            }
        }
        listeners.add(categoryReg)
    }

    fun stopListening() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }

    companion object {
        private fun Map<String, Any?>.toAccount(): Account? {
            val id = (this["id"] as? Number)?.toLong() ?: return null
            val name = this["name"] as? String ?: return null
            val typeName = this["type"] as? String ?: return null
            val type = try { AccountType.valueOf(typeName) } catch (_: Exception) { return null }
            val balance = (this["balance"] as? Number)?.toDouble() ?: 0.0
            return Account(
                id = id,
                name = name,
                type = type,
                balance = balance,
                currency = this["currency"] as? String ?: "PHP",
                color = (this["color"] as? Number)?.toLong() ?: 0xFF6C63FF,
                isArchived = this["isArchived"] as? Boolean ?: false,
                isLiability = this["isLiability"] as? Boolean ?: false,
                createdAt = (this["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                lastModified = (this["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        private fun Map<String, Any?>.toTransaction(): Transaction? {
            val id = (this["id"] as? Number)?.toLong() ?: return null
            val accountId = (this["accountId"] as? Number)?.toLong() ?: return null
            val amount = (this["amount"] as? Number)?.toDouble() ?: return null
            val typeName = this["type"] as? String ?: return null
            val type = try { TransactionType.valueOf(typeName) } catch (_: Exception) { return null }
            val category = this["category"] as? String ?: return null
            return Transaction(
                id = id,
                accountId = accountId,
                toAccountId = (this["toAccountId"] as? Number)?.toLong(),
                amount = amount,
                type = type,
                category = category,
                note = this["note"] as? String ?: "",
                date = (this["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isRecurring = this["isRecurring"] as? Boolean ?: false,
                recurringInterval = (this["recurringInterval"] as? String)?.let { n ->
                    try { RecurringInterval.valueOf(n) } catch (_: Exception) { null }
                },
                imagePath = this["imagePath"] as? String,
                budgetId = (this["budgetId"] as? Number)?.toLong(),
                createdAt = (this["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                lastModified = (this["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        private fun Map<String, Any?>.toBudget(): Budget? {
            val id = (this["id"] as? Number)?.toLong() ?: return null
            val name = this["name"] as? String ?: return null
            val amount = (this["amount"] as? Number)?.toDouble() ?: return null
            val periodName = this["period"] as? String ?: return null
            val period = try { BudgetPeriod.valueOf(periodName) } catch (_: Exception) { return null }
            return Budget(
                id = id,
                name = name,
                amount = amount,
                spent = (this["spent"] as? Number)?.toDouble() ?: 0.0,
                period = period,
                color = (this["color"] as? Number)?.toLong() ?: 0xFF6C63FF,
                startDate = (this["startDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isActive = this["isActive"] as? Boolean ?: true,
                lastModified = (this["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        private fun Map<String, Any?>.toGoal(): Goal? {
            val id = (this["id"] as? Number)?.toLong() ?: return null
            val name = this["name"] as? String ?: return null
            val targetAmount = (this["targetAmount"] as? Number)?.toDouble() ?: return null
            return Goal(
                id = id,
                name = name,
                targetAmount = targetAmount,
                currentAmount = (this["currentAmount"] as? Number)?.toDouble() ?: 0.0,
                targetDate = (this["targetDate"] as? Number)?.toLong(),
                color = (this["color"] as? Number)?.toLong() ?: 0xFF6C63FF,
                isCompleted = this["isCompleted"] as? Boolean ?: false,
                createdAt = (this["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                lastModified = (this["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        private fun Map<String, Any?>.toCategory(): Category? {
            val id = (this["id"] as? Number)?.toLong() ?: return null
            val name = this["name"] as? String ?: return null
            return Category(
                id = id,
                name = name,
                lastModified = (this["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

private fun Account.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "type" to type.name,
    "balance" to balance,
    "currency" to currency,
    "color" to color,
    "isArchived" to isArchived,
    "isLiability" to isLiability,
    "createdAt" to createdAt,
    "lastModified" to lastModified
)

private fun Transaction.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "accountId" to accountId,
    "toAccountId" to toAccountId,
    "amount" to amount,
    "type" to type.name,
    "category" to category,
    "note" to note,
    "date" to date,
    "isRecurring" to isRecurring,
    "recurringInterval" to recurringInterval?.name,
    "imagePath" to imagePath,
    "budgetId" to budgetId,
    "createdAt" to createdAt,
    "lastModified" to lastModified
)

private fun Budget.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "amount" to amount,
    "spent" to spent,
    "period" to period.name,
    "color" to color,
    "startDate" to startDate,
    "isActive" to isActive,
    "lastModified" to lastModified
)

private fun Goal.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "targetAmount" to targetAmount,
    "currentAmount" to currentAmount,
    "targetDate" to targetDate,
    "color" to color,
    "isCompleted" to isCompleted,
    "createdAt" to createdAt,
    "lastModified" to lastModified
)

private fun Category.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "lastModified" to lastModified
)
