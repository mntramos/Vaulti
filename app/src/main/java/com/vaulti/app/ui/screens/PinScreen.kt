package com.vaulti.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaulti.app.viewmodel.CryptoViewModel
import com.vaulti.app.viewmodel.PinMode

@Composable
fun PinScreen(
    cryptoViewModel: CryptoViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val mode by cryptoViewModel.mode.collectAsState()
    val isReady by cryptoViewModel.isReady.collectAsState()
    val isLoading by cryptoViewModel.isLoading.collectAsState()

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        cryptoViewModel.checkStatus()
    }

    LaunchedEffect(isReady) {
        if (isReady) onComplete()
    }

    LaunchedEffect(Unit) {
        cryptoViewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                mode == null && isLoading -> {
                    CircularProgressIndicator()
                }
                mode != null -> {
                    val isSetup = mode == PinMode.Setup

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = if (isSetup) "Set Encryption PIN" else "Enter Encryption PIN",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isSetup)
                                "This PIN encrypts your financial data before it reaches the cloud. Choose a PIN you will remember."
                            else
                                "Enter the PIN you set up to unlock your encrypted data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        OutlinedTextField(
                            value = pin,
                            onValueChange = {
                                if (it.length <= 6) {
                                    pin = it
                                    pinError = null
                                }
                            },
                            label = { Text("PIN") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            isError = pinError != null
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (isSetup) {
                            OutlinedTextField(
                                value = confirmPin,
                                onValueChange = {
                                    if (it.length <= 6) {
                                        confirmPin = it
                                        pinError = null
                                    }
                                },
                                label = { Text("Confirm PIN") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                isError = pinError != null
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        } else {
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        pinError?.let { err ->
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = {
                                val trimmedPin = pin.trim()
                                if (trimmedPin.length < 4) {
                                    pinError = "PIN must be at least 4 digits"
                                    return@Button
                                }
                                if (isSetup && trimmedPin != confirmPin.trim()) {
                                    pinError = "PINs do not match"
                                    return@Button
                                }
                                if (isSetup) {
                                    cryptoViewModel.setupPin(trimmedPin)
                                } else {
                                    cryptoViewModel.unlock(trimmedPin)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = pin.isNotBlank() && (!isSetup || confirmPin.isNotBlank()) && !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(if (isSetup) "Save PIN" else "Unlock")
                            }
                        }
                    }
                }
            }
        }
    }
}
