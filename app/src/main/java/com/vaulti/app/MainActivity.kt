package com.vaulti.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.vaulti.app.data.crypto.CryptoManager
import com.vaulti.app.ui.components.VaultiBottomNavBar
import com.vaulti.app.ui.screens.*
import com.vaulti.app.viewmodel.CryptoViewModel
import com.vaulti.app.ui.theme.AppPreferences
import com.vaulti.app.ui.theme.ThemeMode
import com.vaulti.app.ui.theme.VaultiTheme
import com.vaulti.app.viewmodel.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var cryptoManager: CryptoManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var themeMode by remember { mutableStateOf(appPreferences.themeMode) }

            VaultiTheme(themeMode = themeMode) {
                VaultiMainScreen(
                    appPreferences = appPreferences,
                    cryptoManager = cryptoManager,
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
    appPreferences: AppPreferences,
    cryptoManager: CryptoManager,
    themeMode: ThemeMode,
    onThemeChanged: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    var balancesHidden by remember { mutableStateOf(appPreferences.balancesHidden) }
    val onToggleBalancesHidden: () -> Unit = {
        balancesHidden = !balancesHidden
        appPreferences.balancesHidden = balancesHidden
    }

    val mainRoutes = listOf("dashboard", "transactions", "accounts", "budgets", "goals")
    val showBottomBar = currentRoute in mainRoutes

    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val transactionViewModel: TransactionViewModel = hiltViewModel()
    val accountViewModel: AccountViewModel = hiltViewModel()
    val budgetViewModel: BudgetViewModel = hiltViewModel()
    val goalViewModel: GoalViewModel = hiltViewModel()

    val startDestination = if (isLoggedIn) {
        if (cryptoManager.isInitialized) {
            if (appPreferences.hasSeenTutorial) "dashboard" else "tutorial"
        } else "pin"
    } else "login"

    val tutorialRoutes = listOf("pin", "tutorial", "dashboard")

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && navController.currentDestination?.route !in tutorialRoutes) {
            val dest = if (cryptoManager.isInitialized) {
                if (appPreferences.hasSeenTutorial) "dashboard" else "tutorial"
            } else "pin"
            navController.navigate(dest) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                authViewModel.signInWithGoogle(idToken)
            } else {
                authViewModel.reportGoogleSignInError("Google Sign-In failed. Try again or use email/password.")
            }
        } catch (e: ApiException) {
            authViewModel.reportGoogleSignInError("Google Sign-In failed. Try again or use email/password.")
        }
    }

    Scaffold { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.matchParentSize()
            ) {
            composable("login") {
                val context = LocalContext.current
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = { /* LaunchedEffect handles navigation */ },
                    onNavigateToRegister = { navController.navigate("register") },
                    onGoogleSignInRequest = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                            .requestEmail()
                            .build()
                        val client = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(client.signInIntent)
                    }
                )
            }

            composable("register") {
                RegisterScreen(
                    viewModel = authViewModel,
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            composable("pin") {
                val cryptoViewModel: CryptoViewModel = hiltViewModel()
                PinScreen(
                    cryptoViewModel = cryptoViewModel,
                    onComplete = {
                        authViewModel.startSync()
                        val dest = if (appPreferences.hasSeenTutorial) "dashboard" else "tutorial"
                        navController.navigate(dest) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable("tutorial") {
                TutorialScreen(
                    onDone = {
                        appPreferences.hasSeenTutorial = true
                        navController.navigate("dashboard") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

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
                    hideBalance = balancesHidden,
                    onToggleBalancesHidden = onToggleBalancesHidden
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

            composable("budgets") {
                BudgetsScreen(
                    viewModel = budgetViewModel,
                    appPreferences = appPreferences
                )
            }

            composable("goals") {
                GoalsScreen(
                    viewModel = goalViewModel,
                    appPreferences = appPreferences
                )
            }

            composable("settings") {
                SettingsScreen(
                    appPreferences = appPreferences,
                    themeMode = themeMode,
                    onThemeChanged = onThemeChanged,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCategories = { navController.navigate("categories") },
                    onLogout = { authViewModel.logout() }
                )
            }

            composable("categories") {
                CategoriesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

            if (showBottomBar) {
                VaultiBottomNavBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
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
    }

}
