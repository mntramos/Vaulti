package com.vaulti.app.data.database

import android.content.Context
import com.vaulti.app.data.database.dao.AccountDao
import com.vaulti.app.data.database.dao.BudgetDao
import com.vaulti.app.data.database.dao.GoalDao
import com.vaulti.app.data.database.dao.TransactionDao
import com.vaulti.app.data.repository.AccountRepository
import com.vaulti.app.data.repository.BudgetRepository
import com.vaulti.app.data.repository.GoalRepository
import com.vaulti.app.data.repository.TransactionRepository
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

    @Provides
    fun provideAccountDao(db: VaultiDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideTransactionDao(db: VaultiDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideBudgetDao(db: VaultiDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideGoalDao(db: VaultiDatabase): GoalDao = db.goalDao()

    @Provides @Singleton
    fun provideAccountRepository(dao: AccountDao): AccountRepository = AccountRepository(dao)

    @Provides @Singleton
    fun provideTransactionRepository(dao: TransactionDao): TransactionRepository = TransactionRepository(dao)

    @Provides @Singleton
    fun provideBudgetRepository(dao: BudgetDao): BudgetRepository = BudgetRepository(dao)

    @Provides @Singleton
    fun provideGoalRepository(dao: GoalDao): GoalRepository = GoalRepository(dao)

    @Provides @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences =
        AppPreferences(context)
}
