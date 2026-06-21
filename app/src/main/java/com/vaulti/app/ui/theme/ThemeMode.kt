package com.vaulti.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vaulti_prefs", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = ThemeMode.entries[prefs.getInt("theme_mode", 0)]
        set(value) = prefs.edit { putInt("theme_mode", value.ordinal) }

    var maxVisibleAccounts: Int
        get() = prefs.getInt("max_visible_accounts", 3)
        set(value) = prefs.edit { putInt("max_visible_accounts", value) }

    var maxRecentTransactions: Int
        get() = prefs.getInt("max_recent_transactions", 5)
        set(value) = prefs.edit { putInt("max_recent_transactions", value) }

    var transactionsPageSize: Int
        get() = prefs.getInt("transactions_page_size", 10)
        set(value) = prefs.edit { putInt("transactions_page_size", value) }

    var dashboardAccountSort: String
        get() = prefs.getString("dashboard_account_sort", "NAME_ASC") ?: "NAME_ASC"
        set(value) = prefs.edit { putString("dashboard_account_sort", value) }

    var transactionsSort: String
        get() = prefs.getString("transactions_sort", "DATE_DESC") ?: "DATE_DESC"
        set(value) = prefs.edit { putString("transactions_sort", value) }

    var accountsSort: String
        get() = prefs.getString("accounts_sort", "NAME_ASC") ?: "NAME_ASC"
        set(value) = prefs.edit { putString("accounts_sort", value) }

    var budgetsSort: String
        get() = prefs.getString("budgets_sort", "AMOUNT_DESC") ?: "AMOUNT_DESC"
        set(value) = prefs.edit { putString("budgets_sort", value) }

    var goalsSort: String
        get() = prefs.getString("goals_sort", "TARGET_DESC") ?: "TARGET_DESC"
        set(value) = prefs.edit { putString("goals_sort", value) }

    var balancesHidden: Boolean
        get() = prefs.getBoolean("balances_hidden", false)
        set(value) = prefs.edit {putBoolean("balances_hidden", value) }

    var hasSeenTutorial: Boolean
        get() = prefs.getBoolean("has_seen_tutorial", false)
        set(value) = prefs.edit { putBoolean("has_seen_tutorial", value) }

    var currency: String
        get() = prefs.getString("currency", "PHP") ?: "PHP"
        set(value) = prefs.edit { putString("currency", value) }
}
