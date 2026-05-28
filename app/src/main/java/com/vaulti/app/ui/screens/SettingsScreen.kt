package com.vaulti.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.vaulti.app.ui.theme.AppPreferences
import com.vaulti.app.ui.theme.ThemeMode
import com.vaulti.app.viewmodel.SettingsViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri
import com.vaulti.app.data.database.entity.AccountType
import com.vaulti.app.data.database.entity.BudgetPeriod
import com.vaulti.app.data.database.entity.RecurringInterval
import com.vaulti.app.data.database.entity.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appPreferences: AppPreferences,
    themeMode: ThemeMode,
    onThemeChanged: (ThemeMode) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val packageName = context.packageName
    val versionName = try {
        context.packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown"
    } catch (_: Exception) { "Unknown" }

    var maxAccountsText by remember { mutableStateOf(appPreferences.maxVisibleAccounts.toString()) }
    var maxRecentText by remember { mutableStateOf(appPreferences.maxRecentTransactions.toString()) }
    var pageSizeText by remember { mutableStateOf(appPreferences.transactionsPageSize.toString()) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    exportData(context, uri, viewModel)
                    snackbarHostState.showSnackbar("Data exported successfully", actionLabel = "Dismiss", duration = SnackbarDuration.Short)
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Export failed: ${e.message}", actionLabel = "Dismiss", duration = SnackbarDuration.Short)
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    importData(context, uri, viewModel)
                    snackbarHostState.showSnackbar("Data imported successfully", actionLabel = "Dismiss", duration = SnackbarDuration.Short)
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Import failed: ${e.message}", actionLabel = "Dismiss", duration = SnackbarDuration.Short)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(Icons.Filled.Palette, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Theme", style = MaterialTheme.typography.titleSmall)
                        }
                        ThemeMode.entries.forEach { mode ->
                            val label = when (mode) {
                                ThemeMode.SYSTEM -> "System default"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onThemeChanged(mode) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = themeMode == mode,
                                    onClick = { onThemeChanged(mode) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Display",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Max visible accounts on Home", style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = maxAccountsText,
                                onValueChange = {
                                    maxAccountsText = it
                                    it.toIntOrNull()?.let { v -> appPreferences.maxVisibleAccounts = v }
                                },
                                modifier = Modifier.width(80.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recent transactions on Home", style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = maxRecentText,
                                onValueChange = {
                                    maxRecentText = it
                                    it.toIntOrNull()?.let { v -> appPreferences.maxRecentTransactions = v }
                                },
                                modifier = Modifier.width(80.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Transactions per page", style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = pageSizeText,
                                onValueChange = {
                                    pageSizeText = it
                                    it.toIntOrNull()?.let { v -> appPreferences.transactionsPageSize = v }
                                },
                                modifier = Modifier.width(80.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Export all your data as a JSON file for backup.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                exportLauncher.launch("vaulti_backup_$dateStr.json")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Data")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Import data from a JSON backup file. This will replace all existing data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import Data")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Version", style = MaterialTheme.typography.bodyMedium)
                            Text(versionName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Package", style = MaterialTheme.typography.bodyMedium)
                            Text(packageName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, "https://github.com/mntramos/Vaulti".toUri())
                                    context.startActivity(intent)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "View on GitHub",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = TextDecoration.Underline,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Danger Zone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Delete all data permanently. This action cannot be undone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete All Data")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete All Data") },
            text = {
                Text("Are you sure you want to delete ALL data? This will remove all accounts, transactions, budgets, and goals. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                viewModel.database.clearAllTables()
                            }
                            snackbarHostState.showSnackbar("All data deleted", actionLabel = "Dismiss", duration = SnackbarDuration.Short)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private suspend fun exportData(context: Context, uri: Uri, viewModel: SettingsViewModel) {
    val accounts = viewModel.accountRepository.getAll().first()
    val transactions = viewModel.transactionRepository.getAll().first()
    val budgets = viewModel.budgetRepository.getAll().first()
    val goals = viewModel.goalRepository.getAll().first()

    val root = JSONObject()
    root.put("version", 1)
    root.put("exportedAt", System.currentTimeMillis())

    val accountsArr = JSONArray()
    accounts.forEach { a ->
        accountsArr.put(JSONObject().apply {
            put("id", a.id)
            put("name", a.name)
            put("type", a.type.name)
            put("balance", a.balance)
            put("currency", a.currency)
            put("color", a.color)
            put("isArchived", a.isArchived)
            put("createdAt", a.createdAt)
        })
    }
    root.put("accounts", accountsArr)

    val transactionsArr = JSONArray()
    transactions.forEach { t ->
        transactionsArr.put(JSONObject().apply {
            put("id", t.id)
            put("accountId", t.accountId)
            put("toAccountId", t.toAccountId ?: JSONObject.NULL)
            put("amount", t.amount)
            put("type", t.type.name)
            put("category", t.category)
            put("note", t.note)
            put("date", t.date)
            put("isRecurring", t.isRecurring)
            put("recurringInterval", t.recurringInterval?.name ?: JSONObject.NULL)
            put("imagePath", t.imagePath ?: JSONObject.NULL)
            put("budgetId", t.budgetId ?: JSONObject.NULL)
            put("createdAt", t.createdAt)
        })
    }
    root.put("transactions", transactionsArr)

    val budgetsArr = JSONArray()
    budgets.forEach { b ->
        budgetsArr.put(JSONObject().apply {
            put("id", b.id)
            put("name", b.name)
            put("amount", b.amount)
            put("spent", b.spent)
            put("period", b.period.name)
            put("color", b.color)
            put("startDate", b.startDate)
            put("isActive", b.isActive)
        })
    }
    root.put("budgets", budgetsArr)

    val goalsArr = JSONArray()
    goals.forEach { g ->
        goalsArr.put(JSONObject().apply {
            put("id", g.id)
            put("name", g.name)
            put("targetAmount", g.targetAmount)
            put("currentAmount", g.currentAmount)
            put("targetDate", g.targetDate ?: JSONObject.NULL)
            put("color", g.color)
            put("isCompleted", g.isCompleted)
            put("createdAt", g.createdAt)
        })
    }
    root.put("goals", goalsArr)

    withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(root.toString(2).toByteArray())
        }
    }
}

private suspend fun importData(context: Context, uri: Uri, viewModel: SettingsViewModel) {
    val contentResolver = context.contentResolver
    val jsonString = withContext(Dispatchers.IO) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().readText()
        } ?: throw Exception("Could not read file")
    }

    val root = JSONObject(jsonString)

    withContext(Dispatchers.IO) {
        viewModel.database.clearAllTables()

        val accountsArr = root.getJSONArray("accounts")
        for (i in 0 until accountsArr.length()) {
            val obj = accountsArr.getJSONObject(i)
            val account = com.vaulti.app.data.database.entity.Account(
                id = obj.getLong("id"),
                name = obj.getString("name"),
                type = AccountType.valueOf(obj.getString("type")),
                balance = obj.getDouble("balance"),
                currency = obj.optString("currency", "PHP"),
                color = obj.getLong("color"),
                isArchived = obj.optBoolean("isArchived", false),
                createdAt = obj.getLong("createdAt")
            )
            viewModel.accountRepository.insert(account)
        }

        val transactionsArr = root.getJSONArray("transactions")
        for (i in 0 until transactionsArr.length()) {
            val obj = transactionsArr.getJSONObject(i)
            val toAccountId = if (obj.isNull("toAccountId")) null else obj.getLong("toAccountId")
            val recurringInterval = if (obj.isNull("recurringInterval")) null else RecurringInterval.valueOf(obj.getString("recurringInterval"))
            val imagePath = if (obj.isNull("imagePath")) null else obj.getString("imagePath")
            val budgetId = if (obj.isNull("budgetId")) null else obj.getLong("budgetId")
            val transaction = com.vaulti.app.data.database.entity.Transaction(
                id = obj.getLong("id"),
                accountId = obj.getLong("accountId"),
                toAccountId = toAccountId,
                amount = obj.getDouble("amount"),
                type = TransactionType.valueOf(obj.getString("type")),
                category = obj.getString("category"),
                note = obj.optString("note", ""),
                date = obj.getLong("date"),
                isRecurring = obj.optBoolean("isRecurring", false),
                recurringInterval = recurringInterval,
                imagePath = imagePath,
                budgetId = budgetId,
                createdAt = obj.getLong("createdAt")
            )
            viewModel.transactionRepository.insert(transaction)
        }

        val budgetsArr = root.getJSONArray("budgets")
        for (i in 0 until budgetsArr.length()) {
            val obj = budgetsArr.getJSONObject(i)
            val budget = com.vaulti.app.data.database.entity.Budget(
                id = obj.getLong("id"),
                name = obj.getString("name"),
                amount = obj.getDouble("amount"),
                spent = obj.optDouble("spent", 0.0),
                period = BudgetPeriod.valueOf(obj.getString("period")),
                color = obj.getLong("color"),
                startDate = obj.getLong("startDate"),
                isActive = obj.optBoolean("isActive", true)
            )
            viewModel.budgetRepository.insert(budget)
        }

        val goalsArr = root.getJSONArray("goals")
        for (i in 0 until goalsArr.length()) {
            val obj = goalsArr.getJSONObject(i)
            val targetDate = if (obj.isNull("targetDate")) null else obj.getLong("targetDate")
            val goal = com.vaulti.app.data.database.entity.Goal(
                id = obj.getLong("id"),
                name = obj.getString("name"),
                targetAmount = obj.getDouble("targetAmount"),
                currentAmount = obj.optDouble("currentAmount", 0.0),
                targetDate = targetDate,
                color = obj.getLong("color"),
                isCompleted = obj.optBoolean("isCompleted", false),
                createdAt = obj.getLong("createdAt")
            )
            viewModel.goalRepository.insert(goal)
        }
    }
}
