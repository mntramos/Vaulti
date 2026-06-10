package com.vaulti.app.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId"), Index("toAccountId"), Index("date")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val toAccountId: Long? = null,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val isRecurring: Boolean = false,
    val recurringInterval: RecurringInterval? = null,
    val imagePath: String? = null,
    val budgetId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)
