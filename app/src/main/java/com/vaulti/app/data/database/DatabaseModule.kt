package com.vaulti.app.data.database

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vaulti.app.data.database.dao.AccountDao
import com.vaulti.app.data.database.dao.BudgetDao
import com.vaulti.app.data.database.dao.CategoryDao
import com.vaulti.app.data.database.dao.GoalDao
import com.vaulti.app.data.database.dao.TransactionDao
import com.vaulti.app.data.repository.AccountRepository
import com.vaulti.app.data.repository.BudgetRepository
import com.vaulti.app.data.repository.CategoryRepository
import com.vaulti.app.data.repository.GoalRepository
import com.vaulti.app.data.repository.TransactionRepository
import com.vaulti.app.data.sync.SyncManager
import com.vaulti.app.ui.theme.AppPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VaultiDatabase =
        VaultiDatabase.getDatabase(context)

    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    fun provideAccountDao(db: VaultiDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideTransactionDao(db: VaultiDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideBudgetDao(db: VaultiDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideGoalDao(db: VaultiDatabase): GoalDao = db.goalDao()

    @Provides
    fun provideCategoryDao(db: VaultiDatabase): CategoryDao = db.categoryDao()

    @Provides @Singleton
    fun provideAccountRepository(dao: AccountDao, syncManager: SyncManager): AccountRepository =
        AccountRepository(dao, syncManager)

    @Provides @Singleton
    fun provideTransactionRepository(dao: TransactionDao, syncManager: SyncManager): TransactionRepository =
        TransactionRepository(dao, syncManager)

    @Provides @Singleton
    fun provideBudgetRepository(dao: BudgetDao, syncManager: SyncManager): BudgetRepository =
        BudgetRepository(dao, syncManager)

    @Provides @Singleton
    fun provideGoalRepository(dao: GoalDao, syncManager: SyncManager): GoalRepository =
        GoalRepository(dao, syncManager)

    @Provides @Singleton
    fun provideCategoryRepository(dao: CategoryDao, syncManager: SyncManager): CategoryRepository =
        CategoryRepository(dao, syncManager)

    @Provides @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context, firebaseAuth: FirebaseAuth): AppPreferences =
        AppPreferences(context, firebaseAuth)
}
