package com.vaulti.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: Long? = null,
    val color: Long = 0xFF6C63FF,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)
