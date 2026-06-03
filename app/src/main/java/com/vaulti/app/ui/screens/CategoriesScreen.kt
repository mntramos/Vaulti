package com.vaulti.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaulti.app.data.database.entity.Category
import com.vaulti.app.viewmodel.CategoriesViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: CategoriesViewModel = hiltViewModel()
    val categories by viewModel.categories.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var newCategory by remember { mutableStateOf("") }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            state = rememberLazyListState()
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                categories.forEach { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cat.name, style = MaterialTheme.typography.bodyMedium)
                        if (categories.size > 1) {
                            IconButton(onClick = { categoryToDelete = cat }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        placeholder = { Text("New category") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newCategory.isNotBlank()) {
                                val name = newCategory.trim()
                                val exists = categories.any { it.name.equals(name, ignoreCase = true) }
                                if (exists) {
                                    scope.launch { snackbarHostState.showSnackbar("Category \"$name\" already exists", duration = SnackbarDuration.Short) }
                                } else {
                                    viewModel.addCategory(name)
                                    newCategory = ""
                                }
                            }
                        },
                        enabled = newCategory.isNotBlank()
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }

    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete \"${cat.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        categoryToDelete = null
                        viewModel.deleteCategory(cat)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
