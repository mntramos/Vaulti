package com.vaulti.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vaulti.app.ui.components.VaultiBottomNavBar
import com.vaulti.app.ui.screens.*
import com.vaulti.app.ui.theme.AppPreferences
import com.vaulti.app.ui.theme.ThemeMode
import com.vaulti.app.ui.theme.VaultiTheme
import com.vaulti.app.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as VaultiApplication
        val appPreferences = AppPreferences(this)

        setContent {
            var themeMode by remember { mutableStateOf(appPreferences.themeMode) }

            VaultiTheme(themeMode = themeMode) {
                VaultiMainScreen(
                    app = app,
                    appPreferences = appPreferences,
                    themeMode = themeMode,
                    onThemeChanged = { newMode ->
                        themeMode = newMode
                        appPreferences.themeMode = newMode
                    }
                )
            }
        }
    }
}

@Composable
fun VaultiMainScreen(
    app: VaultiApplication,
    appPreferences: AppPreferences,
    themeMode: ThemeMode,
    onThemeChanged: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var balancesHidden by remember { mutableStateOf(appPreferences.balancesHidden) }
    val onToggleBalancesHidden: () -> Unit = {
        balancesHidden = !balancesHidden
        appPreferences.balancesHidden = balancesHidden
    }

    val showBottomBar = currentRoute in listOf("dashboard", "transactions", "accounts", "budgets", "goals")

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(app.accountRepository, app.transactionRepository)
    )
    val transactionViewModel: TransactionViewModel = viewModel(
        factory = TransactionViewModel.Factory(app.transactionRepository, app.accountRepository)
    )
    val accountViewModel: AccountViewModel = viewModel(
        factory = AccountViewModel.Factory(app.accountRepository)
    )
    val budgetViewModel: BudgetViewModel = viewModel(
        factory = BudgetViewModel.Factory(app.budgetRepository)
    )
    val goalViewModel: GoalViewModel = viewModel(
        factory = GoalViewModel.Factory(app.goalRepository)
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                VaultiBottomNavBar(
                    currentRoute = currentRoute,
                    onItemSelected = { item ->
                        navController.navigate(item.route) {
                            popUpTo("dashboard") {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    appPreferences = appPreferences,
                    balancesHidden = balancesHidden,
                    onToggleBalancesHidden = onToggleBalancesHidden,
                    onAddTransaction = { navController.navigate("add_transaction") },
                    onTransactionClick = { transaction ->
                        navController.navigate("edit_transaction/${transaction.id}")
                    },
                    onAccountClick = { account ->
                        navController.navigate("account_detail/${account.id}")
                    },
                    onSeeAllTransactions = { navController.navigate("transactions") },
                    onSeeAllAccounts = { navController.navigate("accounts") },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }

            composable("transactions") {
                TransactionsScreen(
                    viewModel = transactionViewModel,
                    appPreferences = appPreferences,
                    onAddTransaction = { navController.navigate("add_transaction") },
                    onTransactionClick = { transaction ->
                        navController.navigate("edit_transaction/${transaction.id}")
                    }
                )
            }

            composable(
                "add_transaction?accountId={accountId}",
                arguments = listOf(navArgument("accountId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getLong("accountId") ?: -1L
                AddTransactionScreen(
                    transactionViewModel = transactionViewModel,
                    accountViewModel = accountViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    preselectedAccountId = accountId
                )
            }

            composable(
                "edit_transaction/{transactionId}",
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: return@composable
                val allTransactions by transactionViewModel.transactions.collectAsState()
                val transaction = allTransactions.find { it.id == transactionId }
                if (transaction != null) {
                    AddTransactionScreen(
                        transactionViewModel = transactionViewModel,
                        accountViewModel = accountViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        existingTransaction = transaction
                    )
                }
            }

            composable(
                "account_detail/{accountId}",
                arguments = listOf(navArgument("accountId") { type = NavType.LongType })
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getLong("accountId") ?: return@composable
                AccountDetailScreen(
                    accountId = accountId,
                    transactionViewModel = transactionViewModel,
                    accountViewModel = accountViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onAddTransaction = { id ->
                        navController.navigate("add_transaction?accountId=$id")
                    },
                    onTransactionClick = { transaction ->
                        navController.navigate("edit_transaction/${transaction.id}")
                    },
                    hideBalance = balancesHidden
                )
            }

            composable("accounts") {
                AccountsScreen(
                    viewModel = accountViewModel,
                    appPreferences = appPreferences,
                    onAccountClick = { account ->
                        navController.navigate("account_detail/${account.id}")
                    },
                    hideBalance = balancesHidden,
                    onToggleBalancesHidden = onToggleBalancesHidden
                )
            }

            // TODO: Not yet implemented
//            composable("budgets") {
//                BudgetsScreen(
//                    viewModel = budgetViewModel
//                )
//            }

            // TODO: Not yet implemented
//            composable("goals") {
//                GoalsScreen(
//                    viewModel = goalViewModel
//                )
//            }

            composable("settings") {
                SettingsScreen(
                    app = app,
                    appPreferences = appPreferences,
                    themeMode = themeMode,
                    onThemeChanged = onThemeChanged,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
