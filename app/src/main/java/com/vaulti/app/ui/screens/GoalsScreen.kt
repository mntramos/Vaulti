package com.vaulti.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.vaulti.app.data.database.entity.Goal
import com.vaulti.app.ui.FormatUtils
import com.vaulti.app.ui.theme.AppPreferences
import com.vaulti.app.viewmodel.GoalViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class GoalSort {
    NAME_ASC, NAME_DESC, TARGET_ASC, TARGET_DESC, PROGRESS_ASC, PROGRESS_DESC
}

private enum class GoalFilter {
    ALL, ACTIVE, COMPLETED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalViewModel,
    appPreferences: AppPreferences
) {
    val goals by viewModel.goals.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Goal?>(null) }
    var showContributeDialog by remember { mutableStateOf<Goal?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Goal?>(null) }
    var selectedFilter by remember { mutableStateOf(GoalFilter.ALL) }
    var sortOrder by remember { mutableStateOf(FormatUtils.safeValueOf(appPreferences.goalsSort, GoalSort.NAME_ASC)) }
    var showSortMenu by remember { mutableStateOf(false) }

    val filteredGoals = remember(goals, selectedFilter, sortOrder) {
        val filtered = when (selectedFilter) {
            GoalFilter.ALL -> goals
            GoalFilter.ACTIVE -> goals.filter { !it.isCompleted }
            GoalFilter.COMPLETED -> goals.filter { it.isCompleted }
        }
        when (sortOrder) {
            GoalSort.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            GoalSort.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            GoalSort.TARGET_ASC -> filtered.sortedBy { it.targetAmount }
            GoalSort.TARGET_DESC -> filtered.sortedByDescending { it.targetAmount }
            GoalSort.PROGRESS_ASC -> filtered.sortedBy {
                if (it.targetAmount > 0) (it.currentAmount / it.targetAmount) else 0.0
            }
            GoalSort.PROGRESS_DESC -> filtered.sortedByDescending {
                if (it.targetAmount > 0) (it.currentAmount / it.targetAmount) else 0.0
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Goal")
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
        val listState = rememberLazyListState()
        LaunchedEffect(Unit) { listState.scrollToItem(0) }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Goals",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Track your savings milestones",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GoalFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    filter.name.lowercase().replaceFirstChar { it.uppercase() },
                                    fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (selectedFilter == filter) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Filled.FilterList, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Target (High-Low)", fontWeight = if (sortOrder == GoalSort.TARGET_DESC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { sortOrder = GoalSort.TARGET_DESC; showSortMenu = false; appPreferences.goalsSort = GoalSort.TARGET_DESC.name },
                                leadingIcon = if (sortOrder == GoalSort.TARGET_DESC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                            DropdownMenuItem(
                                text = { Text("Target (Low-High)", fontWeight = if (sortOrder == GoalSort.TARGET_ASC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { sortOrder = GoalSort.TARGET_ASC; showSortMenu = false; appPreferences.goalsSort = GoalSort.TARGET_ASC.name },
                                leadingIcon = if (sortOrder == GoalSort.TARGET_ASC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                            DropdownMenuItem(
                                text = { Text("Name (A-Z)", fontWeight = if (sortOrder == GoalSort.NAME_ASC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { sortOrder = GoalSort.NAME_ASC; showSortMenu = false; appPreferences.goalsSort = GoalSort.NAME_ASC.name },
                                leadingIcon = if (sortOrder == GoalSort.NAME_ASC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z-A)", fontWeight = if (sortOrder == GoalSort.NAME_DESC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { sortOrder = GoalSort.NAME_DESC; showSortMenu = false; appPreferences.goalsSort = GoalSort.NAME_DESC.name },
                                leadingIcon = if (sortOrder == GoalSort.NAME_DESC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                            DropdownMenuItem(
                                text = { Text("Most Complete", fontWeight = if (sortOrder == GoalSort.PROGRESS_DESC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { sortOrder = GoalSort.PROGRESS_DESC; showSortMenu = false; appPreferences.goalsSort = GoalSort.PROGRESS_DESC.name },
                                leadingIcon = if (sortOrder == GoalSort.PROGRESS_DESC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                            DropdownMenuItem(
                                text = { Text("Least Complete", fontWeight = if (sortOrder == GoalSort.PROGRESS_ASC) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { sortOrder = GoalSort.PROGRESS_ASC; showSortMenu = false; appPreferences.goalsSort = GoalSort.PROGRESS_ASC.name },
                                leadingIcon = if (sortOrder == GoalSort.PROGRESS_ASC) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (filteredGoals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No goals yet",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap + to set your first savings goal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(filteredGoals) { goal ->
                GoalCard(
                    goal = goal,
                    onEdit = { showEditDialog = goal },
                    onDelete = { showDeleteConfirm = goal },
                    onContribute = { showContributeDialog = goal },
                    onComplete = { viewModel.completeGoal(goal.id) },
                    currency = appPreferences.currency
                )
            }

            item { Spacer(modifier = Modifier.height(120.dp)) }
        }
        }
    }

    if (showAddDialog) {
        AddGoalDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, targetAmount, targetDate, color ->
                viewModel.addGoal(name, targetAmount, targetDate, color)
                showAddDialog = false
            },
            currency = appPreferences.currency
        )
    }

    showEditDialog?.let { goal ->
        AddGoalDialog(
            initial = goal,
            onDismiss = { showEditDialog = null },
            onConfirm = { name, targetAmount, targetDate, color ->
                viewModel.updateGoal(goal, name, targetAmount, targetDate, color)
                showEditDialog = null
            },
            currency = appPreferences.currency
        )
    }

    if (showContributeDialog != null) {
        val goal = showContributeDialog!!
        var contributeAmount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showContributeDialog = null },
            title = { Text("Contribute to \"${goal.name}\"") },
            text = {
                OutlinedTextField(
                    value = contributeAmount,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) contributeAmount = it },
                    label = { Text("Amount") },
                    prefix = { Text(FormatUtils.currencySymbol(appPreferences.currency)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = contributeAmount.toDoubleOrNull() ?: return@TextButton
                        viewModel.updateProgress(goal.id, goal.currentAmount + amount)
                        showContributeDialog = null
                    },
                    enabled = (contributeAmount.toDoubleOrNull() ?: 0.0) > 0
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showContributeDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm != null) {
        val goal = showDeleteConfirm!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Goal") },
            text = { Text("Are you sure you want to delete \"${goal.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGoal(goal)
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
private fun GoalCard(
    goal: Goal,
    onEdit: (Goal) -> Unit = {},
    onDelete: (Goal) -> Unit = {},
    onContribute: (Goal) -> Unit = {},
    onComplete: () -> Unit = {},
    currency: String = "PHP"
) {
    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val dateFormat = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()) }
    val hasReachedTarget = goal.currentAmount >= goal.targetAmount

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        color = Color(goal.color)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (goal.targetDate != null) {
                        Text(
                            text = Instant.ofEpochMilli(goal.targetDate).atZone(ZoneId.systemDefault()).format(dateFormat),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onEdit(goal) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit goal", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { onDelete(goal) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete goal", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = Color(goal.color),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            val gSymbol = FormatUtils.currencySymbol(currency)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$gSymbol${FormatUtils.formatAmount(goal.currentAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(goal.color)
                )
                Text(
                    text = "$gSymbol${FormatUtils.formatAmount(goal.targetAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (goal.isCompleted) {
                Text(
                    text = "🎉 Complete!",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(goal.color)
                )
            } else if (hasReachedTarget) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}% complete",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onComplete) {
                        Text("Mark Complete", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}% complete",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { onContribute(goal) }) {
                        Text("Contribute", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalDialog(
    initial: Goal? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Long?, Long) -> Unit,
    currency: String = "PHP"
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var targetAmount by remember { mutableStateOf(if (initial != null) FormatUtils.formatAmountForEdit(initial.targetAmount) else "") }
    var hasTargetDate by remember { mutableStateOf(initial?.targetDate != null) }
    var targetDate by remember { mutableLongStateOf(initial?.targetDate ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
    val isEditing = initial != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Goal" else "Add Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) targetAmount = it },
                    label = { Text("Target Amount") },
                    prefix = { Text(FormatUtils.currencySymbol(currency)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Set target date")
                    Switch(
                        checked = hasTargetDate,
                        onCheckedChange = { hasTargetDate = it }
                    )
                }

                if (hasTargetDate) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = Instant.ofEpochMilli(targetDate).atZone(ZoneId.systemDefault()).format(dateFormat),
                            onValueChange = {},
                            label = { Text("Target Date") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountValue = targetAmount.toDoubleOrNull() ?: return@TextButton
                    if (amountValue <= 0) return@TextButton
                    onConfirm(name, amountValue, if (hasTargetDate) targetDate else null, 0xFF6C63FF)
                },
                enabled = name.isNotBlank() && targetAmount.isNotBlank()
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = targetDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { targetDate = it }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
