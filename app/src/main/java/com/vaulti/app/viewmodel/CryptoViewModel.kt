package com.vaulti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaulti.app.data.crypto.CryptoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PinMode { Setup, Unlock }

@HiltViewModel
class CryptoViewModel @Inject constructor(
    private val cryptoManager: CryptoManager
) : ViewModel() {

    private val _mode = MutableStateFlow<PinMode?>(null)
    val mode: StateFlow<PinMode?> = _mode.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = Channel<String>(Channel.BUFFERED)
    val error: Flow<String> = _error.receiveAsFlow()

    fun checkStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (cryptoManager.isInitialized || cryptoManager.tryDeviceUnlock()) {
                    _isReady.value = true
                } else if (cryptoManager.hasKeyBackup()) {
                    _mode.value = PinMode.Unlock
                } else {
                    _mode.value = PinMode.Setup
                }
            } catch (e: Exception) {
                _error.send("Failed to check encryption status")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setupPin(pin: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                cryptoManager.generateAndBackupKey(pin)
                _isReady.value = true
            } catch (e: Exception) {
                _error.send("Failed to set up encryption. Please try again.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun unlock(pin: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (cryptoManager.recoverKey(pin)) {
                    _isReady.value = true
                } else {
                    _error.send("Wrong PIN. Please try again.")
                }
            } catch (e: Exception) {
                _error.send("Wrong PIN. Please try again.")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
