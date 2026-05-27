package com.vaulti.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val spent: Double = 0.0,
    val category: String,
    val period: BudgetPeriod,
    val color: Long = 0xFF6C63FF,
    val startDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

enum class BudgetPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY
}
