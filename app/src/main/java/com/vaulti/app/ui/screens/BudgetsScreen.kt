package com.vaulti.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaulti.app.data.database.entity.Budget
import com.vaulti.app.data.database.entity.BudgetPeriod
import com.vaulti.app.ui.FormatUtils
import com.vaulti.app.ui.theme.AppPreferences
import com.vaulti.app.viewmodel.BudgetViewModel

private enum class BudgetSort {
    NAME_ASC, NAME_DESC, AMOUNT_ASC, AMOUNT_DESC, SPENT_ASC, SPENT_DESC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetViewModel,
    appPreferences: AppPreferences
) {
    val budgets by viewModel.budgets.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Budget?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Budget?>(null) }
    var sortOrder by remember { mutableStateOf(FormatUtils.safeValueOf(appPreferences.budgetsSort, BudgetSort.NAME_ASC)) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sortedBudgets = remember(budgets, sortOrder) {
        when (sortOrder) {
            BudgetSort.NAME_ASC -> budgets.sortedBy { it.name.lowercase() }
            BudgetSort.NAME_DESC -> budgets.sortedByDescending { it.name.lowercase() }
            BudgetSort.AMOUNT_ASC -> budgets.sortedBy { it.amount }
            BudgetSort.AMOUNT_DESC -> budgets.sortedByDescending { it.amount }
            BudgetSort.SPENT_ASC -> budgets.sortedBy { it.spent }
            BudgetSort.SPENT_DESC -> budgets.sortedByDescending { it.spent }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Budget")
            }
        }
    ) { padding ->
        LazyColumn(
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
                    Column {
                        Text(
                            text = "Budgets",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Track your spending limits",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                text = { Text("Amount (High-Low)", fontWeight = if (sortOrder == BudgetSort.AMOUNT_DESC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { sortOrder = BudgetSort.AMOUNT_DESC; showSortMenu = false; appPreferences.budgetsSort = BudgetSort.AMOUNT_DESC.name },
                                leadingIcon = if (sortOrder == BudgetSort.AMOUNT_DESC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                            DropdownMenuItem(
                                text = { Text("Amount (Low-High)", fontWeight = if (sortOrder == BudgetSort.AMOUNT_ASC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { sortOrder = BudgetSort.AMOUNT_ASC; showSortMenu = false; appPreferences.budgetsSort = BudgetSort.AMOUNT_ASC.name },
                                leadingIcon = if (sortOrder == BudgetSort.AMOUNT_ASC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                            DropdownMenuItem(
                                text = { Text("Name (A-Z)", fontWeight = if (sortOrder == BudgetSort.NAME_ASC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { sortOrder = BudgetSort.NAME_ASC; showSortMenu = false; appPreferences.budgetsSort = BudgetSort.NAME_ASC.name },
                                leadingIcon = if (sortOrder == BudgetSort.NAME_ASC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z-A)", fontWeight = if (sortOrder == BudgetSort.NAME_DESC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { sortOrder = BudgetSort.NAME_DESC; showSortMenu = false; appPreferences.budgetsSort = BudgetSort.NAME_DESC.name },
                                leadingIcon = if (sortOrder == BudgetSort.NAME_DESC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                            DropdownMenuItem(
                                text = { Text("Most Spent", fontWeight = if (sortOrder == BudgetSort.SPENT_DESC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { sortOrder = BudgetSort.SPENT_DESC; showSortMenu = false; appPreferences.budgetsSort = BudgetSort.SPENT_DESC.name },
                                leadingIcon = if (sortOrder == BudgetSort.SPENT_DESC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                            DropdownMenuItem(
                                text = { Text("Least Spent", fontWeight = if (sortOrder == BudgetSort.SPENT_ASC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { sortOrder = BudgetSort.SPENT_ASC; showSortMenu = false; appPreferences.budgetsSort = BudgetSort.SPENT_ASC.name },
                                leadingIcon = if (sortOrder == BudgetSort.SPENT_ASC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                        }
                    }
                }
            }

            if (sortedBudgets.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No budgets set",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap + to create your first budget",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(sortedBudgets) { budget ->
                BudgetCard(
                    budget = budget,
                    onDelete = { showDeleteConfirm = budget },
                    onEdit = { showEditDialog = budget }
                )
            }
        }
    }

    if (showAddDialog) {
        AddBudgetDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, amount, period, color ->
                viewModel.addBudget(name, amount, period, color)
                showAddDialog = false
            }
        )
    }

    showEditDialog?.let { budget ->
        AddBudgetDialog(
            initial = budget,
            onDismiss = { showEditDialog = null },
            onConfirm = { name, amount, period, color ->
                viewModel.updateBudget(budget, name, amount, period, color)
                showEditDialog = null
            }
        )
    }

    showDeleteConfirm?.let { budget ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Budget") },
            text = { Text("Are you sure you want to delete \"${budget.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBudget(budget)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BudgetCard(budget: Budget, onDelete: (Budget) -> Unit = {}, onEdit: (Budget) -> Unit = {}) {
    val progress = if (budget.amount > 0) (budget.spent / budget.amount).toFloat().coerceIn(0f, 1f) else 0f
    val isOverBudget = budget.spent > budget.amount
    val overspent = if (isOverBudget) budget.spent - budget.amount else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverBudget) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(12.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = Color(budget.color)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = budget.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = budget.period.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = { onEdit(budget) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit budget", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { onDelete(budget) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete budget", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (isOverBudget) MaterialTheme.colorScheme.error else Color(budget.color),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "₱${FormatUtils.formatAmount(budget.spent)} spent",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "₱${FormatUtils.formatAmount(budget.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isOverBudget) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₱${FormatUtils.formatAmount(overspent)} over budget",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetDialog(
    initial: Budget? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, BudgetPeriod, Long) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amount by remember { mutableStateOf(if (initial != null) FormatUtils.formatAmountForEdit(initial.amount) else "") }
    var selectedPeriod by remember { mutableStateOf(initial?.period ?: BudgetPeriod.MONTHLY) }
    var showPeriodDropdown by remember { mutableStateOf(false) }
    val isEditing = initial != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Budget" else "Add Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Budget Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                    label = { Text("Budget Amount") },
                    prefix = { Text("₱") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = showPeriodDropdown,
                    onExpandedChange = { showPeriodDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedPeriod.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Period") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPeriodDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showPeriodDropdown,
                        onDismissRequest = { showPeriodDropdown = false }
                    ) {
                        BudgetPeriod.entries.forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    selectedPeriod = period
                                    showPeriodDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: return@TextButton
                    if (amountValue <= 0) return@TextButton
                    onConfirm(name, amountValue, selectedPeriod, 0xFF6C63FF)
                },
                enabled = name.isNotBlank() && amount.isNotBlank()
            ) {
                Text(if (isEditing) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
