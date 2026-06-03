package com.vaulti.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vaulti.app.data.database.dao.AccountDao
import com.vaulti.app.data.database.dao.BudgetDao
import com.vaulti.app.data.database.dao.CategoryDao
import com.vaulti.app.data.database.dao.GoalDao
import com.vaulti.app.data.database.dao.TransactionDao
import com.vaulti.app.data.database.entity.Account
import com.vaulti.app.data.database.entity.Budget
import com.vaulti.app.data.database.entity.Category
import com.vaulti.app.data.database.entity.Goal
import com.vaulti.app.data.database.entity.Transaction

@Database(
    entities = [Account::class, Transaction::class, Budget::class, Goal::class, Category::class],
    version = 1,
    exportSchema = false
)
abstract class VaultiDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: VaultiDatabase? = null

        fun getDatabase(context: Context): VaultiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultiDatabase::class.java,
                    "vaulti_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
