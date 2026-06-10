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
    val isLiability: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)
