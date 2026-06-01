package com.vaulti.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.vaulti.app.data.database.entity.Account
import com.vaulti.app.data.database.entity.Transaction
import com.vaulti.app.ui.components.AccountCard
import com.vaulti.app.ui.FormatUtils
import com.vaulti.app.ui.components.TransactionItem
import com.vaulti.app.ui.theme.AppPreferences
import com.vaulti.app.viewmodel.DashboardViewModel

private enum class DashboardAccountSort {
    NAME_ASC, NAME_DESC, BALANCE_ASC, BALANCE_DESC, LAST_UPDATED_DESC, LAST_UPDATED_ASC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    appPreferences: AppPreferences,
    balancesHidden: Boolean = false,
    onToggleBalancesHidden: () -> Unit = {},
    onAddTransaction: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onTransactionDelete: (Transaction) -> Unit = {},
    onAccountClick: (Account) -> Unit,
    onSeeAllTransactions: () -> Unit,
    onSeeAllAccounts: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    val allTransactions by viewModel.recentTransactions.collectAsState()
    val monthlyExpense by viewModel.monthlyExpense.collectAsState()

    val monthlyIncome by viewModel.monthlyIncome.collectAsState()

    val accountMap = remember(accounts) { accounts.associateBy { it.id } }
    var showAccountSortMenu by remember { mutableStateOf(false) }
    var accountSortOrder by remember { mutableStateOf(FormatUtils.safeValueOf(appPreferences.dashboardAccountSort, DashboardAccountSort.LAST_UPDATED_DESC)) }
    val lastTransactionDateByAccount by viewModel.lastTransactionDateByAccount.collectAsState()

    val sortedAccounts = remember(accounts, accountSortOrder, lastTransactionDateByAccount) {
        when (accountSortOrder) {
            DashboardAccountSort.NAME_ASC -> accounts.sortedBy { it.name.lowercase() }
            DashboardAccountSort.NAME_DESC -> accounts.sortedByDescending { it.name.lowercase() }
            DashboardAccountSort.BALANCE_ASC -> accounts.sortedBy { it.balance }
            DashboardAccountSort.BALANCE_DESC -> accounts.sortedByDescending { it.balance }
            DashboardAccountSort.LAST_UPDATED_DESC -> accounts.sortedByDescending { lastTransactionDateByAccount[it.id] ?: 0L }
            DashboardAccountSort.LAST_UPDATED_ASC -> accounts.sortedBy { lastTransactionDateByAccount[it.id] ?: 0L }
        }
    }

    val maxVisible = appPreferences.maxVisibleAccounts
    val maxRecent = appPreferences.maxRecentTransactions

    val visibleRecentTransactions = remember(allTransactions, maxRecent) {
        allTransactions.take(maxRecent)
    }

    fun formatAmount(amount: Double): String = if (balancesHidden) "₱*****" else "₱${FormatUtils.formatAmount(amount)}"

    Scaffold(
        floatingActionButton = {
            if (accounts.isNotEmpty()) {
                FloatingActionButton(onClick = onAddTransaction) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Good ${getTimeOfDay()}!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Here's your financial overview",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Total Balance",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = onToggleBalancesHidden,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    if (balancesHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (balancesHidden) "Show balances" else "Hide balances",
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatAmount(totalBalance),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formatAmount(monthlyIncome),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "Income (30d)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formatAmount(monthlyExpense),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "Expenses (30d)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            if (accounts.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Accounts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Box {
                            IconButton(onClick = { showAccountSortMenu = true }) {
                                Icon(Icons.Filled.FilterList, contentDescription = "Sort Accounts")
                            }
                            DropdownMenu(
                                expanded = showAccountSortMenu,
                                onDismissRequest = { showAccountSortMenu = false }
                            ) {
                                    DropdownMenuItem(
                                        text = { Text("Name (A-Z)", fontWeight = if (accountSortOrder == DashboardAccountSort.NAME_ASC) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = { accountSortOrder = DashboardAccountSort.NAME_ASC; showAccountSortMenu = false; appPreferences.dashboardAccountSort = DashboardAccountSort.NAME_ASC.name },
                                        leadingIcon = if (accountSortOrder == DashboardAccountSort.NAME_ASC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Name (Z-A)", fontWeight = if (accountSortOrder == DashboardAccountSort.NAME_DESC) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = { accountSortOrder = DashboardAccountSort.NAME_DESC; showAccountSortMenu = false; appPreferences.dashboardAccountSort = DashboardAccountSort.NAME_DESC.name },
                                        leadingIcon = if (accountSortOrder == DashboardAccountSort.NAME_DESC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Balance (High-Low)", fontWeight = if (accountSortOrder == DashboardAccountSort.BALANCE_DESC) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = { accountSortOrder = DashboardAccountSort.BALANCE_DESC; showAccountSortMenu = false; appPreferences.dashboardAccountSort = DashboardAccountSort.BALANCE_DESC.name },
                                        leadingIcon = if (accountSortOrder == DashboardAccountSort.BALANCE_DESC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Balance (Low-High)", fontWeight = if (accountSortOrder == DashboardAccountSort.BALANCE_ASC) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = { accountSortOrder = DashboardAccountSort.BALANCE_ASC; showAccountSortMenu = false; appPreferences.dashboardAccountSort = DashboardAccountSort.BALANCE_ASC.name },
                                        leadingIcon = if (accountSortOrder == DashboardAccountSort.BALANCE_ASC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Last Updated (Newest)", fontWeight = if (accountSortOrder == DashboardAccountSort.LAST_UPDATED_DESC) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = { accountSortOrder = DashboardAccountSort.LAST_UPDATED_DESC; showAccountSortMenu = false; appPreferences.dashboardAccountSort = DashboardAccountSort.LAST_UPDATED_DESC.name },
                                        leadingIcon = if (accountSortOrder == DashboardAccountSort.LAST_UPDATED_DESC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Last Updated (Oldest)", fontWeight = if (accountSortOrder == DashboardAccountSort.LAST_UPDATED_ASC) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = { accountSortOrder = DashboardAccountSort.LAST_UPDATED_ASC; showAccountSortMenu = false; appPreferences.dashboardAccountSort = DashboardAccountSort.LAST_UPDATED_ASC.name },
                                        leadingIcon = if (accountSortOrder == DashboardAccountSort.LAST_UPDATED_ASC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                                    )
                            }
                        }
                    }
                }

                item {
                    val accountsRowState = rememberLazyListState()
                    LaunchedEffect(Unit) { accountsRowState.scrollToItem(0) }

                    LazyRow(
                        state = accountsRowState,
                        modifier = Modifier.height(120.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sortedAccounts.take(maxVisible)) { account ->
                            Box(modifier = Modifier.fillMaxHeight()) {
                                AccountCard(
                                    account = account,
                                    modifier = Modifier.clickable { onAccountClick(account) },
                                    hideBalance = balancesHidden
                                )
                            }
                        }
                        if (accounts.size > maxVisible) {
                            item {
                                Box(modifier = Modifier.fillMaxHeight()) {
                                    Card(
                                        modifier = Modifier
                                            .clickable(onClick = onSeeAllAccounts)
                                            .width(160.dp)
                                            .fillMaxHeight(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "See All",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (accounts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Welcome to Vaulti!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add an account to get started",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (visibleRecentTransactions.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onSeeAllTransactions) {
                            Text("See All")
                        }
                    }
                }

                items(visibleRecentTransactions) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        accountName = accountMap[transaction.accountId]?.name ?: "",
                        toAccountName = if (transaction.toAccountId != null) accountMap[transaction.toAccountId]?.name ?: "" else "",
                        onEditClick = { onTransactionClick(transaction) },
                        onDeleteClick = { onTransactionDelete(transaction) }
                    )
                }
            }
        }
    }
}

private fun getTimeOfDay(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..11 -> "Morning"
        in 12..16 -> "Afternoon"
        else -> "Evening"
    }
}
