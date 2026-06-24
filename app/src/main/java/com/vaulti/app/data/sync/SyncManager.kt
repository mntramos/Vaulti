package com.vaulti.app.data.sync

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.vaulti.app.data.crypto.CryptoManager
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: VaultiDatabase,
    private val firebaseAuth: FirebaseAuth,
    private val cryptoManager: CryptoManager
) {
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
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
        val currentUid = uid ?: return
        scope.launch {
            try { ref.document(account.id.toString()).set(account.toSecureMap(cryptoManager, currentUid)) } catch (e: Exception) { Log.e("SyncManager", "pushAccount failed", e) }
        }
    }

    fun deleteAccount(id: Long) {
        scope.launch {
            try { accountsRef()?.document(id.toString())?.delete() } catch (e: Exception) { Log.e("SyncManager", "deleteAccount failed", e) }
        }
    }

    fun pushTransaction(transaction: Transaction) {
        val ref = transactionsRef() ?: return
        val currentUid = uid ?: return
        scope.launch {
            try { ref.document(transaction.id.toString()).set(transaction.toSecureMap(cryptoManager, currentUid)) } catch (e: Exception) { Log.e("SyncManager", "pushTransaction failed", e) }
        }
    }

    fun deleteTransaction(id: Long) {
        scope.launch {
            try { transactionsRef()?.document(id.toString())?.delete() } catch (e: Exception) { Log.e("SyncManager", "deleteTransaction failed", e) }
        }
    }

    fun pushBudget(budget: Budget) {
        val ref = budgetsRef() ?: return
        val currentUid = uid ?: return
        scope.launch {
            try { ref.document(budget.id.toString()).set(budget.toSecureMap(cryptoManager, currentUid)) } catch (e: Exception) { Log.e("SyncManager", "pushBudget failed", e) }
        }
    }

    fun deleteBudget(id: Long) {
        scope.launch {
            try { budgetsRef()?.document(id.toString())?.delete() } catch (e: Exception) { Log.e("SyncManager", "deleteBudget failed", e) }
        }
    }

    fun pushGoal(goal: Goal) {
        val ref = goalsRef() ?: return
        val currentUid = uid ?: return
        scope.launch {
            try { ref.document(goal.id.toString()).set(goal.toSecureMap(cryptoManager, currentUid)) } catch (e: Exception) { Log.e("SyncManager", "pushGoal failed", e) }
        }
    }

    fun deleteGoal(id: Long) {
        scope.launch {
            try { goalsRef()?.document(id.toString())?.delete() } catch (e: Exception) { Log.e("SyncManager", "deleteGoal failed", e) }
        }
    }

    fun pushCategory(category: Category) {
        val ref = categoriesRef() ?: return
        val currentUid = uid ?: return
        scope.launch {
            try { ref.document(category.id.toString()).set(category.toSecureMap(cryptoManager, currentUid)) } catch (e: Exception) { Log.e("SyncManager", "pushCategory failed", e) }
        }
    }

    fun deleteCategory(id: Long) {
        scope.launch {
            try { categoriesRef()?.document(id.toString())?.delete() } catch (e: Exception) { Log.e("SyncManager", "deleteCategory failed", e) }
        }
    }

    suspend fun deleteAll() {
        val currentUid = uid ?: return
        try {
            val baseRef = firestore.collection("users").document(currentUid)
            for (collection in listOf("accounts", "transactions", "budgets", "goals", "categories")) {
                val snapshot = baseRef.collection(collection).get().await()
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
            }
            try {
                baseRef.collection("_crypto").document("key").delete()
            } catch (e: Exception) {
                Log.e("SyncManager", "deleteAll: failed to delete _crypto key", e)
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "deleteAll failed", e)
            FirebaseCrashlytics.getInstance().recordException(e)
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
                val account = doc.data?.toAccount(cryptoManager, currentUid) ?: continue
                val existing = accountDao.getById(account.id)
                if (existing != null) {
                    accountDao.update(account)
                } else {
                    accountDao.insert(account)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "pullAll: accounts failed", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }

        try {
            val transactionSnapshot = baseRef.collection("transactions").get().await()
            for (doc in transactionSnapshot.documents) {
                doc.data?.toTransaction(cryptoManager, currentUid)?.let { transactionDao.insert(it) }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "pullAll: transactions failed", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }

        try {
            val budgetSnapshot = baseRef.collection("budgets").get().await()
            for (doc in budgetSnapshot.documents) {
                doc.data?.toBudget(cryptoManager, currentUid)?.let { budgetDao.insert(it) }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "pullAll: budgets failed", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }

        try {
            val goalSnapshot = baseRef.collection("goals").get().await()
            for (doc in goalSnapshot.documents) {
                doc.data?.toGoal(cryptoManager, currentUid)?.let { goalDao.insert(it) }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "pullAll: goals failed", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }

        try {
            val categorySnapshot = baseRef.collection("categories").get().await()
            for (doc in categorySnapshot.documents) {
                doc.data?.toCategory(cryptoManager, currentUid)?.let { categoryDao.insert(it) }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "pullAll: categories failed", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    fun startListening() {
        val currentUid = uid ?: return
        val baseRef = firestore.collection("users").document(currentUid)

        val accountReg = baseRef.collection("accounts").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("SyncManager", "accounts snapshot listener error", error)
                return@addSnapshotListener
            }
            snapshot?.documentChanges?.forEach { change ->
                if (change.type == DocumentChange.Type.REMOVED) return@forEach
                val account = change.document.data.toAccount(cryptoManager, currentUid) ?: return@forEach
                scope.launch {
                    val existing = database.accountDao().getById(account.id)
                    if (existing != null) {
                        database.accountDao().update(account)
                    } else {
                        database.accountDao().insert(account)
                    }
                }
            }
        }
        listeners.add(accountReg)

        val transactionReg = baseRef.collection("transactions").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("SyncManager", "transactions snapshot listener error", error)
                return@addSnapshotListener
            }
            snapshot?.documentChanges?.forEach { change ->
                if (change.type == DocumentChange.Type.REMOVED) return@forEach
                val transaction = change.document.data.toTransaction(cryptoManager, currentUid) ?: return@forEach
                scope.launch { database.transactionDao().insert(transaction) }
            }
        }
        listeners.add(transactionReg)

        val budgetReg = baseRef.collection("budgets").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("SyncManager", "budgets snapshot listener error", error)
                return@addSnapshotListener
            }
            snapshot?.documentChanges?.forEach { change ->
                if (change.type == DocumentChange.Type.REMOVED) return@forEach
                val budget = change.document.data.toBudget(cryptoManager, currentUid) ?: return@forEach
                scope.launch { database.budgetDao().insert(budget) }
            }
        }
        listeners.add(budgetReg)

        val goalReg = baseRef.collection("goals").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("SyncManager", "goals snapshot listener error", error)
                return@addSnapshotListener
            }
            snapshot?.documentChanges?.forEach { change ->
                if (change.type == DocumentChange.Type.REMOVED) return@forEach
                val goal = change.document.data.toGoal(cryptoManager, currentUid) ?: return@forEach
                scope.launch { database.goalDao().insert(goal) }
            }
        }
        listeners.add(goalReg)

        val categoryReg = baseRef.collection("categories").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("SyncManager", "categories snapshot listener error", error)
                return@addSnapshotListener
            }
            snapshot?.documentChanges?.forEach { change ->
                if (change.type == DocumentChange.Type.REMOVED) return@forEach
                val category = change.document.data.toCategory(cryptoManager, currentUid) ?: return@forEach
                scope.launch { database.categoryDao().insert(category) }
            }
        }
        listeners.add(categoryReg)
    }

    fun stopListening() {
        scope.cancel()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        listeners.forEach { it.remove() }
        listeners.clear()
    }

    companion object {
        private fun Map<String, Any?>.toAccount(crypto: CryptoManager?, uid: String?): Account? {
            val id = (this["id"] as? Number)?.toLong() ?: return null
            val ctx = if (crypto != null && uid != null) "$uid|accounts|$id" else null
            val name = if (ctx != null && this.containsKey("name_enc")) {
                try { crypto!!.decrypt(this["name_enc"] as String, "$ctx|name") } catch (_: Exception) { null }
            } else {
                this["name"] as? String
            } ?: return null
            val typeName = this["type"] as? String ?: return null
            val type = try { AccountType.valueOf(typeName) } catch (_: Exception) { return null }
            val balance = if (ctx != null && this.containsKey("balance_enc")) {
                try { crypto!!.decrypt(this["balance_enc"] as String, "$ctx|balance").toDoubleOrNull() ?: 0.0 } catch (_: Exception) { 0.0 }
            } else {
                (this["balance"] as? Number)?.toDouble() ?: 0.0
            }
            val currency = if (ctx != null && this.containsKey("currency_enc")) {
                try { crypto!!.decrypt(this["currency_enc"] as String, "$ctx|currency") } catch (_: Exception) { null }
            } else {
                this["currency"] as? String
            } ?: "PHP"
            return Account(
                id = id,
                name = name,
                type = type,
                balance = balance,
                currency = currency,
                color = (this["color"] as? Number)?.toLong() ?: 0xFF6C63FF,
                isArchived = this["isArchived"] as? Boolean ?: false,
                isLiability = this["isLiability"] as? Boolean ?: false,
                createdAt = (this["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                lastModified = (this["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        private fun Map<String, Any?>.toTransaction(crypto: CryptoManager?, uid: String?): Transaction? {
            val id = (this["id"] as? Number)?.toLong() ?: return null
            val accountId = (this["accountId"] as? Number)?.toLong() ?: return null
            val ctx = if (crypto != null && uid != null) "$uid|transactions|$id" else null
            val amount = if (ctx != null && this.containsKey("amount_enc")) {
                try { crypto!!.decrypt(this["amount_enc"] as String, "$ctx|amount").toDoubleOrNull() } catch (_: Exception) { null }
            } else {
                (this["amount"] as? Number)?.toDouble()
            } ?: return null
            val typeName = this["type"] as? String ?: return null
            val type = try { TransactionType.valueOf(typeName) } catch (_: Exception) { return null }
            val category = if (ctx != null && this.containsKey("category_enc")) {
                try { crypto!!.decrypt(this["category_enc"] as String, "$ctx|category") } catch (_: Exception) { null }
            } else {
                this["category"] as? String
            } ?: return null
            return Transaction(
                id = id,
                accountId = accountId,
                toAccountId = (this["toAccountId"] as? Number)?.toLong(),
                amount = amount,
                type = type,
                category = category,
                note = if (ctx != null && this.containsKey("note_enc")) {
                    try { crypto!!.decrypt(this["note_enc"] as String, "$ctx|note") } catch (_: Exception) { "" }
                } else {
                    this["note"] as? String ?: ""
                },
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

        private fun Map<String, Any?>.toBudget(crypto: CryptoManager?, uid: String?): Budget? {
            val id = (this["id"] as? Number)?.toLong() ?: return null
            val ctx = if (crypto != null && uid != null) "$uid|budgets|$id" else null
            val name = if (ctx != null && this.containsKey("name_enc")) {
                try { crypto!!.decrypt(this["name_enc"] as String, "$ctx|name") } catch (_: Exception) { null }
            } else {
                this["name"] as? String
            } ?: return null
            val amount = if (ctx != null && this.containsKey("amount_enc")) {
                try { crypto!!.decrypt(this["amount_enc"] as String, "$ctx|amount").toDoubleOrNull() } catch (_: Exception) { null }
            } else {
                (this["amount"] as? Number)?.toDouble()
            } ?: return null
            val periodName = this["period"] as? String ?: return null
            val period = try { BudgetPeriod.valueOf(periodName) } catch (_: Exception) { return null }
            return Budget(
                id = id,
                name = name,
                amount = amount,
                spent = if (ctx != null && this.containsKey("spent_enc")) {
                    try { crypto!!.decrypt(this["spent_enc"] as String, "$ctx|spent").toDoubleOrNull() ?: 0.0 } catch (_: Exception) { 0.0 }
                } else {
                    (this["spent"] as? Number)?.toDouble() ?: 0.0
                },
                period = period,
                color = (this["color"] as? Number)?.toLong() ?: 0xFF6C63FF,
                startDate = (this["startDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isActive = this["isActive"] as? Boolean ?: true,
                lastModified = (this["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        private fun Map<String, Any?>.toGoal(crypto: CryptoManager?, uid: String?): Goal? {
            val id = (this["id"] as? Number)?.toLong() ?: return null
            val ctx = if (crypto != null && uid != null) "$uid|goals|$id" else null
            val name = if (ctx != null && this.containsKey("name_enc")) {
                try { crypto!!.decrypt(this["name_enc"] as String, "$ctx|name") } catch (_: Exception) { null }
            } else {
                this["name"] as? String
            } ?: return null
            val targetAmount = if (ctx != null && this.containsKey("targetAmount_enc")) {
                try { crypto!!.decrypt(this["targetAmount_enc"] as String, "$ctx|targetAmount").toDoubleOrNull() } catch (_: Exception) { null }
            } else {
                (this["targetAmount"] as? Number)?.toDouble()
            } ?: return null
            return Goal(
                id = id,
                name = name,
                targetAmount = targetAmount,
                currentAmount = if (ctx != null && this.containsKey("currentAmount_enc")) {
                    try { crypto!!.decrypt(this["currentAmount_enc"] as String, "$ctx|currentAmount").toDoubleOrNull() ?: 0.0 } catch (_: Exception) { 0.0 }
                } else {
                    (this["currentAmount"] as? Number)?.toDouble() ?: 0.0
                },
                targetDate = (this["targetDate"] as? Number)?.toLong(),
                color = (this["color"] as? Number)?.toLong() ?: 0xFF6C63FF,
                isCompleted = this["isCompleted"] as? Boolean ?: false,
                createdAt = (this["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                lastModified = (this["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        private fun Map<String, Any?>.toCategory(crypto: CryptoManager?, uid: String?): Category? {
            val id = (this["id"] as? Number)?.toLong() ?: return null
            val ctx = if (crypto != null && uid != null) "$uid|categories|$id" else null
            val name = if (ctx != null && this.containsKey("name_enc")) {
                try { crypto!!.decrypt(this["name_enc"] as String, "$ctx|name") } catch (_: Exception) { null }
            } else {
                this["name"] as? String
            } ?: return null
            return Category(
                id = id,
                name = name,
                lastModified = (this["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

private fun Account.toSecureMap(crypto: CryptoManager, uid: String): Map<String, Any?> {
    val ctx = "$uid|accounts|$id"
    return mapOf(
        "id" to id,
        "name_enc" to crypto.encrypt(name, "$ctx|name"),
        "type" to type.name,
        "balance_enc" to crypto.encrypt(balance.toString(), "$ctx|balance"),
        "currency_enc" to crypto.encrypt(currency, "$ctx|currency"),
        "color" to color,
        "isArchived" to isArchived,
        "isLiability" to isLiability,
        "createdAt" to createdAt,
        "lastModified" to lastModified
    )
}

private fun Transaction.toSecureMap(crypto: CryptoManager, uid: String): Map<String, Any?> {
    val ctx = "$uid|transactions|$id"
    return mapOf(
        "id" to id,
        "accountId" to accountId,
        "toAccountId" to toAccountId,
        "amount_enc" to crypto.encrypt(amount.toString(), "$ctx|amount"),
        "type" to type.name,
        "category_enc" to crypto.encrypt(category, "$ctx|category"),
        "note_enc" to crypto.encrypt(note, "$ctx|note"),
        "date" to date,
        "isRecurring" to isRecurring,
        "recurringInterval" to recurringInterval?.name,
        "imagePath" to imagePath,
        "budgetId" to budgetId,
        "createdAt" to createdAt,
        "lastModified" to lastModified
    )
}

private fun Budget.toSecureMap(crypto: CryptoManager, uid: String): Map<String, Any?> {
    val ctx = "$uid|budgets|$id"
    return mapOf(
        "id" to id,
        "name_enc" to crypto.encrypt(name, "$ctx|name"),
        "amount_enc" to crypto.encrypt(amount.toString(), "$ctx|amount"),
        "spent_enc" to crypto.encrypt(spent.toString(), "$ctx|spent"),
        "period" to period.name,
        "color" to color,
        "startDate" to startDate,
        "isActive" to isActive,
        "lastModified" to lastModified
    )
}

private fun Goal.toSecureMap(crypto: CryptoManager, uid: String): Map<String, Any?> {
    val ctx = "$uid|goals|$id"
    return mapOf(
        "id" to id,
        "name_enc" to crypto.encrypt(name, "$ctx|name"),
        "targetAmount_enc" to crypto.encrypt(targetAmount.toString(), "$ctx|targetAmount"),
        "currentAmount_enc" to crypto.encrypt(currentAmount.toString(), "$ctx|currentAmount"),
        "targetDate" to targetDate,
        "color" to color,
        "isCompleted" to isCompleted,
        "createdAt" to createdAt,
        "lastModified" to lastModified
    )
}

private fun Category.toSecureMap(crypto: CryptoManager, uid: String): Map<String, Any?> {
    val ctx = "$uid|categories|$id"
    return mapOf(
        "id" to id,
        "name_enc" to crypto.encrypt(name, "$ctx|name"),
        "lastModified" to lastModified
    )
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
