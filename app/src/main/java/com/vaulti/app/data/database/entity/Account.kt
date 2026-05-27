package com.vaulti.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val balance: Double,
    val currency: String = "PHP",
    val color: Long = 0xFF6C63FF,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AccountType(val displayName: String, val defaultColor: Long) {
    CASH("Cash", 0xFF2ECC71),
    BANK("Bank Account", 0xFF3498DB),
    CREDIT("Credit Card", 0xFFFF6B6B),
    E_WALLET("E-Wallet", 0xFF6C63FF),
    INVESTMENT("Investment", 0xFF00C9A7),
    OTHER("Other", 0xFF95A5A6)
}
