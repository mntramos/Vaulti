package com.vaulti.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaulti.app.data.database.entity.Transaction
import com.vaulti.app.data.database.entity.TransactionType
import com.vaulti.app.ui.components.TransactionItem
import com.vaulti.app.viewmodel.TransactionViewModel

enum class SortOrder { DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionViewModel,
    onAddTransaction: () -> Unit,
    onTransactionClick: (Transaction) -> Unit
) {
    val allTransactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    var selectedFilterType by remember { mutableStateOf<TransactionType?>(null) }
    var sortOrder by remember { mutableStateOf(SortOrder.DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }

    val transactions = remember(allTransactions, selectedFilterType, sortOrder) {
        val filtered = if (selectedFilterType != null) {
            allTransactions.filter { it.type == selectedFilterType }
        } else {
            allTransactions
        }
        when (sortOrder) {
            SortOrder.DATE_DESC -> filtered.sortedByDescending { it.date }
            SortOrder.DATE_ASC -> filtered.sortedBy { it.date }
            SortOrder.AMOUNT_DESC -> filtered.sortedByDescending { it.amount }
            SortOrder.AMOUNT_ASC -> filtered.sortedBy { it.amount }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (accounts.isNotEmpty()) {
                FloatingActionButton(onClick = onAddTransaction) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { padding ->
        if (allTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (accounts.isEmpty()) {
                        Text(
                            text = "No accounts yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add an account in the Accounts tab first",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "No transactions yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to add your first transaction",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transactions",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.Filled.FilterList, contentDescription = "Sort")
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Newest First", fontWeight = if (sortOrder == SortOrder.DATE_DESC) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = { sortOrder = SortOrder.DATE_DESC; showSortMenu = false },
                                        leadingIcon = if (sortOrder == SortOrder.DATE_DESC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Oldest First", fontWeight = if (sortOrder == SortOrder.DATE_ASC) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = { sortOrder = SortOrder.DATE_ASC; showSortMenu = false },
                                        leadingIcon = if (sortOrder == SortOrder.DATE_ASC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Highest Amount", fontWeight = if (sortOrder == SortOrder.AMOUNT_DESC) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = { sortOrder = SortOrder.AMOUNT_DESC; showSortMenu = false },
                                        leadingIcon = if (sortOrder == SortOrder.AMOUNT_DESC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Lowest Amount", fontWeight = if (sortOrder == SortOrder.AMOUNT_ASC) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = { sortOrder = SortOrder.AMOUNT_ASC; showSortMenu = false },
                                        leadingIcon = if (sortOrder == SortOrder.AMOUNT_ASC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilterType == null,
                            onClick = { selectedFilterType = null },
                            label = { Text("All", fontWeight = if (selectedFilterType == null) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (selectedFilterType == null) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                        )
                        TransactionType.entries.forEach { type ->
                            val label = type.name.lowercase().replaceFirstChar { it.uppercase() }
                            FilterChip(
                                selected = selectedFilterType == type,
                                onClick = { selectedFilterType = if (selectedFilterType == type) null else type },
                                label = { Text(label, fontWeight = if (selectedFilterType == type) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = if (selectedFilterType == type) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(transactions) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        accountName = accountMap[transaction.accountId]?.name ?: "",
                        toAccountName = if (transaction.toAccountId != null) accountMap[transaction.toAccountId]?.name ?: "" else "",
                        onItemClick = { onTransactionClick(transaction) }
                    )
                }
            }
        }
    }
}
