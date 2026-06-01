package com.vaulti.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaulti.app.data.database.entity.Account
import com.vaulti.app.data.database.entity.AccountType
import com.vaulti.app.ui.FormatUtils
import com.vaulti.app.ui.theme.AppPreferences
import com.vaulti.app.viewmodel.AccountViewModel

private enum class AccountSort {
    NAME_ASC, NAME_DESC, BALANCE_ASC, BALANCE_DESC, LAST_UPDATED_DESC, LAST_UPDATED_ASC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountViewModel,
    appPreferences: AppPreferences,
    onAccountClick: (Account) -> Unit,
    hideBalance: Boolean = false,
    onToggleBalancesHidden: () -> Unit = {}
) {
    val accounts by viewModel.accounts.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    val lastTransactionDateByAccount by viewModel.lastTransactionDateByAccount.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(FormatUtils.safeValueOf(appPreferences.accountsSort, AccountSort.LAST_UPDATED_DESC)) }

    val sortedAccounts = remember(accounts, sortOrder, lastTransactionDateByAccount) {
        when (sortOrder) {
            AccountSort.NAME_ASC -> accounts.sortedBy { it.name.lowercase() }
            AccountSort.NAME_DESC -> accounts.sortedByDescending { it.name.lowercase() }
            AccountSort.BALANCE_ASC -> accounts.sortedBy { it.balance }
            AccountSort.BALANCE_DESC -> accounts.sortedByDescending { it.balance }
            AccountSort.LAST_UPDATED_DESC -> accounts.sortedByDescending { lastTransactionDateByAccount[it.id] ?: 0L }
            AccountSort.LAST_UPDATED_ASC -> accounts.sortedBy { lastTransactionDateByAccount[it.id] ?: 0L }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Account")
            }
        }
    ) { padding ->
        val listState = rememberLazyListState()
        LaunchedEffect(Unit) { listState.scrollToItem(0) }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Accounts",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Filled.FilterList, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Name (A-Z)", fontWeight = if (sortOrder == AccountSort.NAME_ASC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { sortOrder = AccountSort.NAME_ASC; showSortMenu = false; appPreferences.accountsSort = AccountSort.NAME_ASC.name },
                                leadingIcon = if (sortOrder == AccountSort.NAME_ASC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z-A)", fontWeight = if (sortOrder == AccountSort.NAME_DESC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { sortOrder = AccountSort.NAME_DESC; showSortMenu = false; appPreferences.accountsSort = AccountSort.NAME_DESC.name },
                                leadingIcon = if (sortOrder == AccountSort.NAME_DESC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                            DropdownMenuItem(
                                text = { Text("Balance (High-Low)", fontWeight = if (sortOrder == AccountSort.BALANCE_DESC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { sortOrder = AccountSort.BALANCE_DESC; showSortMenu = false; appPreferences.accountsSort = AccountSort.BALANCE_DESC.name },
                                leadingIcon = if (sortOrder == AccountSort.BALANCE_DESC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                            DropdownMenuItem(
                                text = { Text("Balance (Low-High)", fontWeight = if (sortOrder == AccountSort.BALANCE_ASC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { sortOrder = AccountSort.BALANCE_ASC; showSortMenu = false; appPreferences.accountsSort = AccountSort.BALANCE_ASC.name },
                                leadingIcon = if (sortOrder == AccountSort.BALANCE_ASC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                            DropdownMenuItem(
                                text = { Text("Last Updated (Newest)", fontWeight = if (sortOrder == AccountSort.LAST_UPDATED_DESC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { sortOrder = AccountSort.LAST_UPDATED_DESC; showSortMenu = false; appPreferences.accountsSort = AccountSort.LAST_UPDATED_DESC.name },
                                leadingIcon = if (sortOrder == AccountSort.LAST_UPDATED_DESC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                            DropdownMenuItem(
                                text = { Text("Last Updated (Oldest)", fontWeight = if (sortOrder == AccountSort.LAST_UPDATED_ASC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { sortOrder = AccountSort.LAST_UPDATED_ASC; showSortMenu = false; appPreferences.accountsSort = AccountSort.LAST_UPDATED_ASC.name },
                                leadingIcon = if (sortOrder == AccountSort.LAST_UPDATED_ASC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Total Balance",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = onToggleBalancesHidden,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        if (hideBalance) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (hideBalance) "Show balances" else "Hide balances",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (hideBalance) "₱*****" else "₱${FormatUtils.formatAmount(totalBalance)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "${accounts.size} accounts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            if (sortedAccounts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No accounts yet",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap + to add your first account",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(sortedAccounts) { account ->
                AccountDetailCard(
                    account = account,
                    onClick = { onAccountClick(account) },
                    hideBalance = hideBalance
                )
            }
        }
    }

    if (showAddDialog) {
        AddAccountDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, balance, color, isLiability ->
                viewModel.addAccount(name, type, balance, color, isLiability)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AccountDetailCard(
    account: Account,
    onClick: () -> Unit,
    hideBalance: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = Color(account.color)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = account.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = account.type.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = if (hideBalance) "₱*****" else "₱${FormatUtils.formatAmount(account.balance)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, AccountType, Double, Long, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.CASH) }
    var balance by remember { mutableStateOf("") }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var isLiability by remember(selectedType) { mutableStateOf(selectedType.isLiability) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = balance,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) balance = it },
                    label = { Text(if (isLiability) "Outstanding Debt" else "Initial Balance") },
                    prefix = { Text("₱") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = showTypeDropdown,
                    onExpandedChange = { showTypeDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showTypeDropdown,
                        onDismissRequest = { showTypeDropdown = false }
                    ) {
                        AccountType.entries.sortedBy { it.displayName }.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedType = type
                                    isLiability = type.isLiability
                                    showTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                if (selectedType == AccountType.OTHER) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isLiability = !isLiability }
                    ) {
                        Checkbox(
                            checked = isLiability,
                            onCheckedChange = { isLiability = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("This is a liability / debt")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val balanceValue = balance.toDoubleOrNull() ?: 0.0
                    onConfirm(name, selectedType, balanceValue, selectedType.defaultColor, isLiability)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
