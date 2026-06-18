package com.vaulti.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaulti.app.data.database.entity.Transaction
import com.vaulti.app.data.database.entity.TransactionType
import com.vaulti.app.ui.FormatUtils
import com.vaulti.app.ui.components.TransactionItem
import com.vaulti.app.ui.theme.AppPreferences
import com.vaulti.app.viewmodel.TransactionViewModel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionViewModel,
    appPreferences: AppPreferences,
    onAddTransaction: () -> Unit,
    onTransactionClick: (Transaction) -> Unit
) {
    val allTransactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    var selectedFilterType by remember { mutableStateOf<TransactionType?>(null) }
    var sortOrder by remember { mutableStateOf(FormatUtils.safeValueOf(appPreferences.transactionsSort, SortOrder.DATE_DESC)) }
    var showSortMenu by remember { mutableStateOf(false) }
    var visibleCount by remember { mutableIntStateOf(appPreferences.transactionsPageSize) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val pageSize = appPreferences.transactionsPageSize

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

    val visibleTransactions = remember(transactions, visibleCount) {
        transactions.take(visibleCount)
    }

    fun resetPaging() {
        visibleCount = pageSize
        selectedIds = emptySet()
    }

    Scaffold(
        floatingActionButton = {
            if (accounts.isNotEmpty()) {
                FloatingActionButton(onClick = onAddTransaction) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Transaction")
                }
            }
        },
        modifier = Modifier.padding(bottom = 80.dp)
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
            val listState = rememberLazyListState()
            LaunchedEffect(Unit) { listState.scrollToItem(0) }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(horizontal = 16.dp),
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
                            if (selectedIds.isNotEmpty()) {
                                IconButton(onClick = { showDeleteConfirm = true }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete selected")
                                }
                            }
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
                                        onClick = { sortOrder = SortOrder.DATE_DESC; showSortMenu = false; appPreferences.transactionsSort = SortOrder.DATE_DESC.name; resetPaging() },
                                        leadingIcon = if (sortOrder == SortOrder.DATE_DESC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Oldest First", fontWeight = if (sortOrder == SortOrder.DATE_ASC) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = { sortOrder = SortOrder.DATE_ASC; showSortMenu = false; appPreferences.transactionsSort = SortOrder.DATE_ASC.name; resetPaging() },
                                        leadingIcon = if (sortOrder == SortOrder.DATE_ASC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Highest Amount", fontWeight = if (sortOrder == SortOrder.AMOUNT_DESC) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = { sortOrder = SortOrder.AMOUNT_DESC; showSortMenu = false; appPreferences.transactionsSort = SortOrder.AMOUNT_DESC.name; resetPaging() },
                                        leadingIcon = if (sortOrder == SortOrder.AMOUNT_DESC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Lowest Amount", fontWeight = if (sortOrder == SortOrder.AMOUNT_ASC) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = { sortOrder = SortOrder.AMOUNT_ASC; showSortMenu = false; appPreferences.transactionsSort = SortOrder.AMOUNT_ASC.name; resetPaging() },
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
                            onClick = { selectedFilterType = null; resetPaging() },
                            label = { Text("All", fontWeight = if (selectedFilterType == null) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (selectedFilterType == null) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                        )
                        TransactionType.entries.sortedBy { it.name }.forEach { type ->
                            val label = type.name.lowercase().replaceFirstChar { it.uppercase() }
                            FilterChip(
                                selected = selectedFilterType == type,
                                onClick = { selectedFilterType = if (selectedFilterType == type) null else type; resetPaging() },
                                label = { Text(label, fontWeight = if (selectedFilterType == type) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = if (selectedFilterType == type) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(visibleTransactions) { transaction ->
                    val isSelected = transaction.id in selectedIds
                    @OptIn(ExperimentalFoundationApi::class)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (selectedIds.isNotEmpty()) {
                                        selectedIds = if (isSelected) selectedIds - transaction.id else selectedIds + transaction.id
                                    }
                                },
                                onLongClick = {
                                    if (selectedIds.isEmpty()) {
                                        selectedIds = setOf(transaction.id)
                                    } else {
                                        selectedIds = if (isSelected) selectedIds - transaction.id else selectedIds + transaction.id
                                    }
                                }
                            ),
                        colors = if (isSelected) CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) else CardDefaults.cardColors()
                    ) {
                    TransactionItem(
                        transaction = transaction,
                        accountName = accountMap[transaction.accountId]?.name ?: "",
                        toAccountName = if (transaction.toAccountId != null) accountMap[transaction.toAccountId]?.name ?: "" else "",
                        currency = appPreferences.currency,
                        onEditClick = { onTransactionClick(transaction) }
                    )
                    }
                }

                if (visibleTransactions.size < transactions.size) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(
                                onClick = { visibleCount += pageSize }
                            ) {
                                Text("See More (${transactions.size - visibleTransactions.size} remaining)")
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(120.dp)) }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Transactions") },
            text = { Text("Are you sure you want to delete ${selectedIds.size} selected transaction(s)? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedIds.forEach { id ->
                            viewModel.deleteTransactionById(id)
                        }
                        selectedIds = emptySet()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
