package com.vaulti.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vaulti.app.data.database.entity.Transaction
import com.vaulti.app.data.database.entity.TransactionType
import com.vaulti.app.ui.FormatUtils
import com.vaulti.app.viewmodel.AccountViewModel
import com.vaulti.app.viewmodel.TransactionViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    transactionViewModel: TransactionViewModel,
    accountViewModel: AccountViewModel,
    onNavigateBack: () -> Unit,
    existingTransaction: Transaction? = null,
    preselectedAccountId: Long = -1L,
    currency: String = "PHP"
) {
    val accounts by accountViewModel.accounts.collectAsState()
    val budgets by transactionViewModel.budgets.collectAsState()
    val categories by transactionViewModel.categories.collectAsState()
    val isEditing = existingTransaction != null
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var selectedType by remember(existingTransaction) {
        mutableStateOf(existingTransaction?.type ?: TransactionType.EXPENSE)
    }
    var selectedAccount by remember(existingTransaction, preselectedAccountId, accounts) {
        mutableStateOf(
            existingTransaction?.let { tx -> accounts.find { a -> a.id == tx.accountId } }
                ?: if (preselectedAccountId >= 0) accounts.find { it.id == preselectedAccountId }
                else null
        )
    }
    var selectedToAccount by remember(existingTransaction) {
        mutableStateOf(
            if (existingTransaction?.toAccountId != null)
                accounts.find { it.id == existingTransaction.toAccountId }
            else null
        )
    }
    var amount by remember(existingTransaction) {
        mutableStateOf(if (existingTransaction != null) String.format(Locale.getDefault(), "%.2f", existingTransaction.amount) else "")
    }
    var category by remember(existingTransaction) {
        mutableStateOf(existingTransaction?.category ?: "")
    }
    var note by remember(existingTransaction) {
        mutableStateOf(existingTransaction?.note ?: "")
    }
    var date by remember(existingTransaction) {
        mutableLongStateOf(existingTransaction?.date ?: System.currentTimeMillis())
    }

    var selectedBudget by remember(existingTransaction, budgets) {
        mutableStateOf(
            existingTransaction?.let { tx -> budgets.find { it.id == tx.budgetId } }
        )
    }
    var showAccountDropdown by remember { mutableStateOf(false) }
    var showToAccountDropdown by remember { mutableStateOf(false) }
    var showBudgetDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()) }
    val filteredAccounts = if (selectedAccount != null)
        accounts.filter { it.id != selectedAccount!!.id } else accounts

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Transaction" else "Add Transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete transaction", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionType.entries.forEach { type ->
                    val label = when (type) {
                        TransactionType.EXPENSE -> "Expense"
                        TransactionType.INCOME -> "Income"
                        TransactionType.TRANSFER -> "Transfer"
                    }
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            ExposedDropdownMenuBox(
                expanded = showAccountDropdown,
                onExpandedChange = { showAccountDropdown = it }
            ) {
                OutlinedTextField(
                    value = selectedAccount?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (selectedType == TransactionType.TRANSFER) "From Account" else "Account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showAccountDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showAccountDropdown,
                    onDismissRequest = { showAccountDropdown = false }
                ) {
                    accounts.sortedBy { it.name.lowercase() }.forEach { account ->
                        val sym = FormatUtils.currencySymbol(currency)
                        DropdownMenuItem(
                            text = { Text("${account.name} ($sym${FormatUtils.formatAmount(account.balance)})") },
                            onClick = {
                                selectedAccount = account
                                showAccountDropdown = false
                            }
                        )
                    }
                }
            }

            if (selectedType == TransactionType.TRANSFER) {
                ExposedDropdownMenuBox(
                    expanded = showToAccountDropdown,
                    onExpandedChange = { showToAccountDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedToAccount?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showToAccountDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showToAccountDropdown,
                        onDismissRequest = { showToAccountDropdown = false }
                    ) {
                        filteredAccounts.sortedBy { it.name.lowercase() }.forEach { account ->
                            val sym = FormatUtils.currencySymbol(currency)
                            DropdownMenuItem(
                            text = { Text("${account.name} ($sym${FormatUtils.formatAmount(account.balance)})") },
                                onClick = {
                                    selectedToAccount = account
                                    showToAccountDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                label = { Text("Amount") },
                prefix = { Text(FormatUtils.currencySymbol(currency)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Category", style = MaterialTheme.typography.bodyLarge)

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = category == cat.name,
                        onClick = { category = cat.name },
                        label = { Text(cat.name, style = MaterialTheme.typography.bodySmall) }
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { showAddCategoryDialog = true },
                    label = { Text("+ Add Category", style = MaterialTheme.typography.bodySmall) }
                )
            }

            if (selectedType == TransactionType.EXPENSE && budgets.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = showBudgetDropdown,
                    onExpandedChange = { showBudgetDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedBudget?.name ?: "None",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Budget (optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showBudgetDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showBudgetDropdown,
                        onDismissRequest = { showBudgetDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                selectedBudget = null
                                showBudgetDropdown = false
                            },
                            leadingIcon = if (selectedBudget == null) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                        )
                        budgets.sortedBy { it.name.lowercase() }.forEach { budget ->
                            DropdownMenuItem(
                                text = { Text(budget.name) },
                                onClick = {
                                    selectedBudget = budget
                                    showBudgetDropdown = false
                                },
                                leadingIcon = if (selectedBudget == budget) {{ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).format(dateFormat),
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val utcDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                                date = utcDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            }
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

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: return@Button
                    if (amountValue <= 0) return@Button
                    val account = selectedAccount ?: return@Button
                    val budgetId = if (selectedType == TransactionType.EXPENSE) selectedBudget?.id else null
                    if (isEditing && existingTransaction != null) {
                        transactionViewModel.updateTransaction(
                            transaction = existingTransaction,
                            accountId = account.id,
                            amount = amountValue,
                            type = selectedType,
                            category = category.ifBlank { "Other" },
                            note = note,
                            date = date,
                            toAccountId = if (selectedType == TransactionType.TRANSFER) selectedToAccount?.id else null,
                            budgetId = budgetId
                        )
                    } else {
                        transactionViewModel.addTransaction(
                            accountId = account.id,
                            amount = amountValue,
                            type = selectedType,
                            category = category.ifBlank { "Other" },
                            note = note,
                            date = date,
                            toAccountId = if (selectedType == TransactionType.TRANSFER) selectedToAccount?.id else null,
                            budgetId = budgetId
                        )
                    }
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedAccount != null && amount.isNotBlank() && category.isNotBlank() &&
                        (selectedType != TransactionType.TRANSFER || selectedToAccount != null)
            ) {
                Text(
                    if (isEditing) "Update Transaction" else "Save Transaction",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }

    if (showAddCategoryDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            transactionViewModel.addCategory(newCategoryName)
                            category = newCategoryName
                            showAddCategoryDialog = false
                        }
                    },
                    enabled = newCategoryName.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm && existingTransaction != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionViewModel.deleteTransaction(existingTransaction)
                        onNavigateBack()
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
